package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.ReglasTutoria;
import com.tme.service.ReglasTutoria.Rol;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;

/** External collections only. A receipt is evidence for review, never a bank confirmation. */
public final class PagosDAO {
    public record Pago(String estado,int version,BigDecimal importe,LocalDateTime enviado,LocalDateTime corregir,String observacion,LocalDateTime validado) { }
    public record Comprobante(int id,int version,String medio,String referencia,LocalDateTime operado,LocalDateTime registrado) { }
    public record Archivo(byte[] datos,String mime) { }
    public record Devolucion(int id,BigDecimal importe,String motivo,LocalDateTime aprobada,LocalDateTime ejecutada,String referencia) { }
    public record Fila(int id,String alumno,String curso,String sesion,String pago,LocalDateTime enviado,LocalDateTime inicio,boolean solicitud,BigDecimal importe) { }
    public record Pagina(List<Fila> filas,int total,int pagina,int paginas) { }
    private final Connection conn;
    private final TutoriaDAO base;
    private final GestionAsesoriasDAO gestion;
    public PagosDAO(Connection c){this(c,Clock.system(ReglasTutoria.ZONA));}
    public PagosDAO(Connection c,Clock clock){conn=c;base=new TutoriaDAO(c,clock);gestion=new GestionAsesoriasDAO(c,clock);}

    public Pago obtener(int id){
        try(PreparedStatement s=conn.prepareStatement("select * from pago_alumno where tutoria_id=?")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?leer(r):null;}
        }catch(SQLException e){throw failure(e);}
    }
    private Pago leer(ResultSet r)throws SQLException{return new Pago(r.getString("estado"),r.getInt("version"),r.getBigDecimal("importe"),r.getObject("enviado_en",LocalDateTime.class),r.getObject("corregir_hasta",LocalDateTime.class),r.getString("observacion"),r.getObject("validado_en",LocalDateTime.class));}
    private Tutoria bloquear(Tutoria initial)throws SQLException{
        if(!base.lockAlumno(initial.getUserId()))throw new IllegalArgumentException("No se encuentra al alumno.");
        base.lockDocente(initial.getDocenteId());
        try(PreparedStatement s=conn.prepareStatement("select * from tutoria where id=? for update")){s.setInt(1,initial.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("No se encuentra la asesoría.");return base.read(r);}}
    }
    private Pago bloquearPago(int id)throws SQLException{
        try(PreparedStatement s=conn.prepareStatement("select * from pago_alumno where tutoria_id=? for update")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?leer(r):null;}}
    }
    public static BigDecimal dinero(String valor){
        if(valor==null||!valor.matches("[0-9]{1,8}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("Indica un importe positivo en soles, con hasta dos decimales.");
        BigDecimal n=new BigDecimal(valor).setScale(2);if(n.signum()<=0)throw new IllegalArgumentException("El importe debe ser mayor que cero.");return n;
    }
    public static Archivo archivo(byte[] bytes){
        if(bytes==null||bytes.length<8||bytes.length>5*1024*1024)throw new IllegalArgumentException("Adjunta un comprobante de hasta 5 MB en PDF, JPG o PNG.");
        String mime;
        if(bytes[0]=='%'&&bytes[1]=='P'&&bytes[2]=='D'&&bytes[3]=='F'&&bytes[4]=='-')mime="application/pdf";
        else if((bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)mime="image/jpeg";
        else if(Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))mime="image/png";
        else throw new IllegalArgumentException("El archivo debe ser un PDF, JPG o PNG. No basta con cambiar su extensión.");
        return new Archivo(bytes,mime);
    }
    public static String hash(String value){return hash(value.getBytes(StandardCharsets.UTF_8));}
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private String referencia(String valor){String ref=OfertaDAO.texto(valor,100,"Referencia de la operación");if(!ref.matches("[\\p{L}\\p{N} ._/#-]{2,100}")||!ref.matches(".*[\\p{L}\\p{N}].*"))throw new IllegalArgumentException("La referencia debe contener letras, números o separadores habituales.");return ref;}
    private String clave(String medio,String referencia){return hash(medio.strip().toUpperCase(Locale.ROOT)+"|"+referencia.toUpperCase(Locale.ROOT).replaceAll("[\\s._/#-]",""));}

    public boolean registrar(int id,int actor,Rol rol,int version,String importe,String ref,LocalDateTime operado,byte[] bytes,boolean tardio,String motivo){
        if(rol!=Rol.ADMIN&&rol!=Rol.ALUMNO)throw new IllegalArgumentException("Tu cuenta no puede registrar comprobantes.");
        Tutoria initial=gestion.obtener(id,actor,rol);if(initial==null)return false;
        BigDecimal monto=dinero(importe);String referencia=referencia(ref);Archivo file=archivo(bytes);
        return base.transaction(()->{
            Tutoria t=bloquear(initial);Pago p=bloquearPago(id);LocalDateTime now=base.ahora();
            exigirVersion(p,version);
            if(t.getPrecio()==null||monto.compareTo(t.getPrecio())!=0)throw new IllegalArgumentException("El importe debe coincidir con el precio total de la reserva. Contacta a administración si pagaste otro importe.");
            if(operado==null||operado.isAfter(now))throw new IllegalArgumentException("Indica la fecha y hora real del pago, sin fechas futuras.");
            if(p!=null&&!p.estado().equals("Observado"))throw new IllegalArgumentException("El pago ya está en revisión o fue validado.");
            if(tardio){
                if(rol!=Rol.ADMIN||t.getVencePago()==null||!Set.of("Vencida","Cancelada","Rechazada","Incidencia").contains(t.getEstado()))throw new IllegalArgumentException("El registro fuera de plazo solo está disponible para administración en reservas cerradas o con incidencia.");
                OfertaDAO.texto(motivo,850,"Motivo del ingreso fuera de plazo");
            }else if(!t.getEstado().equals("Pendiente de pago")||!ReglasTutoria.vigente(t,now)||t.isCancelacionSolicitada())throw new IllegalArgumentException("Ya no se puede enviar un comprobante para esta reserva. Contacta a administración si realizaste un pago.");
            if(!new ConfiguracionPagosDAO(conn).condiciones(id).completa()){
                if(rol!=Rol.ADMIN)throw new IllegalArgumentException("Administración debe habilitar las instrucciones de esta reserva anterior.");
                new ConfiguracionPagosDAO(conn).copiar(id);
            }
            var c=new ConfiguracionPagosDAO(conn).condiciones(id);int nueva=version+1;
            if(p==null){try(PreparedStatement s=conn.prepareStatement("insert into pago_alumno(tutoria_id,estado,version,importe,enviado_en) values(?,'En revisión',?,?,?)")){s.setInt(1,id);s.setInt(2,nueva);s.setBigDecimal(3,monto);s.setObject(4,now);s.executeUpdate();}}
            else try(PreparedStatement s=conn.prepareStatement("update pago_alumno set estado='En revisión',version=?,enviado_en=?,corregir_hasta=null,observacion='' where tutoria_id=?")){s.setInt(1,nueva);s.setObject(2,now);s.setInt(3,id);s.executeUpdate();}
            String huella=hash(bytes),operacion=clave(c.medio(),referencia);identificador(id,"archivo",huella);identificador(id,"operacion",operacion);
            try(PreparedStatement s=conn.prepareStatement("insert into pago_comprobante(tutoria_id,version,medio,referencia,operacion_clave,operado_en,registrado_en,actor_rol,actor_id,archivo,mime,huella) values(?,?,?,?,?,?,?,?,?,?,?,?)")){
                s.setInt(1,id);s.setInt(2,nueva);s.setString(3,c.medio());s.setString(4,referencia);s.setString(5,operacion);s.setObject(6,operado);s.setObject(7,now);s.setString(8,rol.name());actor(s,9,actor,rol);s.setBytes(10,file.datos());s.setString(11,file.mime());s.setString(12,huella);s.executeUpdate();
            }
            try(PreparedStatement s=conn.prepareStatement("update tutoria set pago_revision=true where id=?")){s.setInt(1,id);s.executeUpdate();}
            evento(id,rol,actor,"Comprobante recibido","Comprobante #"+nueva+" recibido para revisión. "+(tardio?"Ingreso fuera de plazo: "+motivo:"No confirma el ingreso bancario."),now);
            GestionAsesoriasDAO.evento(conn,id,rol.name(),rol==Rol.ADMIN?null:actor,"Comprobante recibido","Se recibió un comprobante para revisión administrativa. La asesoría todavía no se confirma.",now);return true;
        });
    }
    private void identificador(int id,String tipo,String clave)throws SQLException{
        try(PreparedStatement s=conn.prepareStatement("insert ignore into pago_identificador(tipo,clave,tutoria_id) values(?,?,?)")){s.setString(1,tipo);s.setString(2,clave);s.setInt(3,id);s.executeUpdate();}
        try(PreparedStatement s=conn.prepareStatement("select tutoria_id from pago_identificador where tipo=? and clave=? for update")){s.setString(1,tipo);s.setString(2,clave);try(ResultSet r=s.executeQuery()){if(!r.next()||r.getInt(1)!=id)throw new IllegalArgumentException("Ese comprobante o referencia ya pertenece a otra reserva. Administración debe revisar el caso.");}}
    }
    public boolean gestionar(int id,int actor,Rol rol,int version,String accion,String motivo,String importe,String ref,LocalDateTime fecha,int devolucion){
        if(accion==null)throw new IllegalArgumentException("Indica una acción de pago válida.");
        Tutoria initial=gestion.obtener(id,actor,rol);if(initial==null)return false;
        if(rol!=Rol.ADMIN&&!accion.equals("solicitar_devolucion"))throw new IllegalArgumentException("Solo administración puede validar ingresos y registrar devoluciones.");
        return base.transaction(()->{
            Tutoria t=bloquear(initial);Pago p=bloquearPago(id);LocalDateTime now=base.ahora();
            exigirVersion(p,version);String detalle;
            if(accion.equals("habilitar")){
                if(rol!=Rol.ADMIN||p!=null||!t.getEstado().equals("Pendiente de pago")||!ReglasTutoria.vigente(t,now)||new ConfiguracionPagosDAO(conn).condiciones(id).completa())throw new IllegalArgumentException("Esta reserva no necesita habilitación de cobro.");
                new ConfiguracionPagosDAO(conn).copiar(id);GestionAsesoriasDAO.evento(conn,id,"ADMIN",null,"Cobro habilitado","Administración habilitó las instrucciones para esta reserva anterior. Se conserva el vencimiento original.",now);return true;
            }
            if(p==null)throw new IllegalArgumentException("Todavía no hay un comprobante registrado.");
            switch(accion){
                case "validar" -> {
                    if(!p.estado().equals("En revisión"))throw new IllegalArgumentException("Este pago ya no está pendiente de revisión.");
                    detalle=OfertaDAO.texto(motivo,850,"Verificación del ingreso real");
                    if(dinero(importe).compareTo(p.importe())!=0)throw new IllegalArgumentException("Verifica que el ingreso real coincida con el total de la reserva.");
                    boolean confirmar=t.getEstado().equals("Pendiente de pago")&&new DocenteDAO(conn).isActiveDocente(t.getDocenteId());
                    if(t.getEstado().equals("Pendiente de pago")){
                        LocalDateTime inicio=ReglasTutoria.inicio(t);if(inicio==null||!now.isBefore(inicio))throw new IllegalArgumentException("La sesión ya comenzó. Actualiza la página para resolverla como incidencia.");
                        base.validarDisponibilidad(t);
                        try(PreparedStatement s=conn.prepareStatement("update tutoria set estado=?,pago_revision=false where id=?")){s.setString(1,confirmar?"Confirmada":"Incidencia");s.setInt(2,id);s.executeUpdate();}
                    }else try(PreparedStatement s=conn.prepareStatement("update tutoria set pago_revision=false where id=?")){s.setInt(1,id);s.executeUpdate();}
                    try(PreparedStatement s=conn.prepareStatement("update pago_alumno set estado='Validado',validado_en=?,corregir_hasta=null,version=version+1 where tutoria_id=?")){s.setObject(1,now);s.setInt(2,id);s.executeUpdate();}
                    GestionAsesoriasDAO.evento(conn,id,"ADMIN",null,"Ingreso validado",confirmar?"Ingreso verificado por administración. Asesoría confirmada.":t.getEstado().equals("Pendiente de pago")?"Ingreso verificado con el profesor inactivo. La sesión pasa a incidencia para resolución administrativa.":"Ingreso verificado fuera del flujo de confirmación. Se conserva el estado de la sesión; requiere resolver su destino.",now);
                }
                case "observar" -> {
                    if(!p.estado().equals("En revisión"))throw new IllegalArgumentException("Este pago ya no está en revisión.");
                    detalle=OfertaDAO.texto(motivo,850,"Qué debe corregir el alumno");
                    LocalDateTime inicio=ReglasTutoria.inicio(t),limite=null;
                    if(t.getEstado().equals("Pendiente de pago")&&inicio!=null&&now.isBefore(inicio)){
                        Integer minutos=new ConfiguracionPagosDAO(conn).condiciones(id).correccion();if(minutos==null)throw new IllegalArgumentException("Falta configurar el plazo de corrección.");
                        limite=now.plusMinutes(minutos);if(limite.isAfter(inicio))limite=inicio;
                    }
                    try(PreparedStatement s=conn.prepareStatement("update pago_alumno set estado='Observado',observacion=?,corregir_hasta=?,version=version+1 where tutoria_id=?")){s.setString(1,detalle);s.setObject(2,limite);s.setInt(3,id);s.executeUpdate();}
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set pago_revision=false,vence_pago=coalesce(?,vence_pago) where id=?")){s.setObject(1,limite);s.setInt(2,id);s.executeUpdate();}
                    detalle+=". "+(limite==null?"Sin nuevo plazo de reserva; contactar a administración.":"Corregir hasta "+limite+" (Lima).");
                }
                case "solicitar_devolucion" -> {
                    if(rol!=Rol.ALUMNO||p.validado()==null||p.estado().equals("Devuelto")||p.estado().equals("Devolución pendiente")||t.isCancelacionSolicitada())throw new IllegalArgumentException("No se puede enviar otra solicitud en este estado.");
                    detalle=OfertaDAO.texto(motivo,850,"Motivo de la solicitud");
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set cancelacion_solicitada=true,motivo_cancelacion=? where id=?")){s.setString(1,detalle);s.setInt(2,id);s.executeUpdate();}
                    try(PreparedStatement s=conn.prepareStatement("update tutoria_cambio set estado='Anulada' where tutoria_id=? and estado='Pendiente'")){s.setInt(1,id);s.executeUpdate();}
                    try(PreparedStatement s=conn.prepareStatement("update pago_alumno set version=version+1 where tutoria_id=?")){s.setInt(1,id);s.executeUpdate();}
                    GestionAsesoriasDAO.evento(conn,id,"ALUMNO",actor,"Solicitud de revisión","Solicitud de cancelación o devolución enviada a administración. No cambia automáticamente la sesión ni devuelve dinero.",now);
                }
                case "aprobar_devolucion" -> {
                    if(p.validado()==null||p.estado().equals("Devolución pendiente")||p.estado().equals("Devuelto"))throw new IllegalArgumentException("No hay un ingreso disponible para esta devolución.");
                    if(!Set.of("Cancelada","Vencida","Rechazada","Completada").contains(t.getEstado()))throw new IllegalArgumentException("Resuelve primero la sesión: cancela o resuelve la incidencia antes de aprobar una devolución.");
                    detalle=OfertaDAO.texto(motivo,850,"Motivo de la devolución");BigDecimal monto=dinero(importe);
                    if(monto.compareTo(p.importe().subtract(devuelto(id)))>0)throw new IllegalArgumentException("La devolución supera el saldo recibido disponible.");
                    try(PreparedStatement s=conn.prepareStatement("insert into pago_devolucion(tutoria_id,importe,motivo,aprobada_en) values(?,?,?,?)")){s.setInt(1,id);s.setBigDecimal(2,monto);s.setString(3,detalle);s.setObject(4,now);s.executeUpdate();}
                    try(PreparedStatement s=conn.prepareStatement("update pago_alumno set estado='Devolución pendiente',version=version+1 where tutoria_id=?")){s.setInt(1,id);s.executeUpdate();}
                    detalle="Se aprobó devolver S/ "+monto+". Todavía no se realizó la transferencia. "+detalle;
                }
                case "ejecutar_devolucion" -> {
                    if(!p.estado().equals("Devolución pendiente"))throw new IllegalArgumentException("No hay una devolución pendiente.");
                    String referencia=referencia(ref);if(fecha==null||fecha.isAfter(now))throw new IllegalArgumentException("Indica la fecha y hora real de la transferencia, sin fechas futuras.");
                    BigDecimal monto=null;LocalDateTime aprobada=null;
                    try(PreparedStatement s=conn.prepareStatement("select importe,aprobada_en from pago_devolucion where id=? and tutoria_id=? and ejecutada_en is null for update")){s.setInt(1,devolucion);s.setInt(2,id);try(ResultSet r=s.executeQuery()){if(r.next()){monto=r.getBigDecimal(1);aprobada=r.getObject(2,LocalDateTime.class);}}}
                    if(monto==null)throw new IllegalArgumentException("La devolución ya fue registrada o no pertenece a esta asesoría.");
                    if(fecha.isBefore(aprobada.withSecond(0).withNano(0)))throw new IllegalArgumentException("La transferencia no puede ser anterior a su aprobación.");
                    if(dinero(importe).compareTo(monto)!=0)throw new IllegalArgumentException("El importe transferido debe coincidir con la devolución aprobada.");
                    try(PreparedStatement s=conn.prepareStatement("update pago_devolucion set ejecutada_en=?,referencia=?,registrada_en=?,transferencia_clave=? where id=?")){s.setObject(1,fecha);s.setString(2,referencia);s.setObject(3,now);s.setString(4,clave("devolucion",referencia));s.setInt(5,devolucion);s.executeUpdate();}
                    catch(SQLIntegrityConstraintViolationException e){throw new IllegalArgumentException("Esa referencia de transferencia ya se utilizó en otra devolución.");}
                    String nuevo=devuelto(id).compareTo(p.importe())==0?"Devuelto":"Validado";
                    try(PreparedStatement s=conn.prepareStatement("update pago_alumno set estado=?,version=version+1 where tutoria_id=?")){s.setString(1,nuevo);s.setInt(2,id);s.executeUpdate();}
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set cancelacion_solicitada=false where id=?")){s.setInt(1,id);s.executeUpdate();}
                    LiquidacionesDAO.registrarDevolucion(conn,t,devolucion,now);
                    detalle="Devolución registrada: S/ "+monto+"; referencia "+referencia+"; ejecutada el "+fecha+" (Lima).";
                }
                default -> throw new IllegalArgumentException("Acción de pago inválida.");
            }
            evento(id,rol,actor,accion,detalle,now);return true;
        });
    }
    private void exigirVersion(Pago p,int version){if(version!=(p==null?0:p.version()))throw new IllegalArgumentException("El pago cambió desde que abriste la página. Revisa su estado actualizado antes de continuar.");}
    public BigDecimal devuelto(int id){try(PreparedStatement s=conn.prepareStatement("select coalesce(sum(importe),0) from pago_devolucion where tutoria_id=? and ejecutada_en is not null")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){r.next();return r.getBigDecimal(1);}}catch(SQLException e){throw failure(e);}}
    public List<Comprobante> comprobantes(int id){List<Comprobante> result=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select id,version,medio,referencia,operado_en,registrado_en from pago_comprobante where tutoria_id=? order by id desc")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new Comprobante(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getObject(5,LocalDateTime.class),r.getObject(6,LocalDateTime.class)));}return result;}catch(SQLException e){throw failure(e);}}
    public Archivo descargar(int archivo,int actor,Rol rol){
        if(rol!=Rol.ADMIN&&rol!=Rol.ALUMNO)return null;
        try(PreparedStatement s=conn.prepareStatement("select c.archivo,c.mime from pago_comprobante c join tutoria t on t.id=c.tutoria_id where c.id=?"+(rol==Rol.ALUMNO?" and t.userId=?":""))){s.setInt(1,archivo);if(rol==Rol.ALUMNO)s.setInt(2,actor);try(ResultSet r=s.executeQuery()){return r.next()?new Archivo(r.getBytes(1),r.getString(2)):null;}}catch(SQLException e){throw failure(e);}
    }
    public List<Devolucion> devoluciones(int id){List<Devolucion> result=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select * from pago_devolucion where tutoria_id=? order by id desc")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new Devolucion(r.getInt("id"),r.getBigDecimal("importe"),r.getString("motivo"),r.getObject("aprobada_en",LocalDateTime.class),r.getObject("ejecutada_en",LocalDateTime.class),r.getString("referencia")));}return result;}catch(SQLException e){throw failure(e);}}
    public List<GestionAsesoriasDAO.Evento> historial(int id){List<GestionAsesoriasDAO.Evento> result=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select actor_rol,accion,detalle,ocurrido_en from pago_evento where tutoria_id=? order by id desc")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new GestionAsesoriasDAO.Evento(r.getString(1),r.getString(2),r.getString(3),r.getObject(4,LocalDateTime.class)));}return result;}catch(SQLException e){throw failure(e);}}
    public Pagina listar(int actor,Rol rol,String filtro,int pagina){
        if(rol!=Rol.ADMIN&&rol!=Rol.ALUMNO)throw new IllegalArgumentException("Tu cuenta no tiene acceso a los pagos.");
        if(!Set.of("Por atender","Todos","En revisión","Observado","Validado","Devolución pendiente","Devuelto").contains(filtro))throw new IllegalArgumentException("Filtro de pagos inválido.");
        gestion.expirar();List<Fila> result=new ArrayList<>();
        String where="(p.tutoria_id is not null or t.estado='Pendiente de pago')"+(rol==Rol.ALUMNO?" and t.userId=?":"");
        if(filtro.equals("Por atender"))where+=" and (p.estado in ('En revisión','Observado','Devolución pendiente') or t.cancelacion_solicitada=true or (p.validado_en is not null and t.estado in ('Vencida','Cancelada','Rechazada','Incidencia') and p.estado='Validado' and not exists(select 1 from pago_devolucion d where d.tutoria_id=t.id)))";
        else if(!filtro.equals("Todos"))where+=" and p.estado=?";
        String from=" from tutoria t left join pago_alumno p on p.tutoria_id=t.id where "+where;
        try{
            int total;try(PreparedStatement s=conn.prepareStatement("select count(*)"+from)){parametros(s,actor,rol,filtro);try(ResultSet r=s.executeQuery()){r.next();total=r.getInt(1);}}
            int paginas=Math.max(1,(total+19)/20),actual=Math.max(1,Math.min(pagina,paginas));
            try(PreparedStatement s=conn.prepareStatement("select t.id,t.nombre,t.curso_nombre,t.estado,p.estado,p.enviado_en,t.ftutoria,t.hora_inicio,t.cancelacion_solicitada,coalesce(p.importe,t.precio)"+from+" order by case when p.estado='En revisión' then 0 else 1 end,p.enviado_en,t.id limit 20 offset ?")){
                int index=parametros(s,actor,rol,filtro);s.setInt(index,(actual-1)*20);
                try(ResultSet r=s.executeQuery()){while(r.next()){LocalDateTime inicio=null;try{if(r.getTime(8)!=null)inicio=LocalDate.parse(r.getString(7)).atTime(r.getTime(8).toLocalTime());}catch(DateTimeException ignored){}result.add(new Fila(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getObject(6,LocalDateTime.class),inicio,r.getBoolean(9),r.getBigDecimal(10)));}}
            }return new Pagina(result,total,actual,paginas);
        }catch(SQLException e){throw failure(e);}
    }
    private int parametros(PreparedStatement s,int actor,Rol rol,String filtro)throws SQLException{int i=1;if(rol==Rol.ALUMNO)s.setInt(i++,actor);if(!filtro.equals("Por atender")&&!filtro.equals("Todos"))s.setString(i++,filtro);return i;}
    private void evento(int id,Rol rol,int actor,String accion,String detalle,LocalDateTime now)throws SQLException{
        try(PreparedStatement s=conn.prepareStatement("insert into pago_evento(tutoria_id,actor_rol,actor_id,accion,detalle,ocurrido_en) values(?,?,?,?,?,?)")){s.setInt(1,id);s.setString(2,rol.name());actor(s,3,actor,rol);s.setString(4,accion);s.setString(5,detalle);s.setObject(6,now);s.executeUpdate();}
    }
    private void actor(PreparedStatement s,int index,int actor,Rol rol)throws SQLException{if(rol==Rol.ADMIN)s.setNull(index,Types.INTEGER);else s.setInt(index,actor);}
    private IllegalStateException failure(SQLException e){return new IllegalStateException("No se pudo completar la operación de pago.",e);}
}
