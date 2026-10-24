package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.ReglasTutoria;
import com.tme.service.ReglasTutoria.Rol;
import java.sql.*;
import java.time.*;
import java.math.*;
import java.util.*;

/** Immutable closing lines; active assignments prevent paying a session or adjustment twice. */
public final class LiquidacionesDAO {
    public record Linea(int tutoria,Integer ajuste,String tipo,String curso,LocalDate fecha,BigDecimal precio,BigDecimal porcentaje,BigDecimal devuelto,BigDecimal importe,String descripcion) { }
    public record Retenida(int id,String curso,String motivo) { }
    public record Vista(List<Linea> lineas,List<Retenida> retenidas,BigDecimal total,String huella) { }
    public record Lote(int id,int docente,String nombre,LocalDate corte,LocalDate previsto,LocalDateTime creado,String estado,int version,BigDecimal total,String motivo,LocalDateTime transferida,String referencia,String destino) { }
    public record Ajuste(int id,int tutoria,int docente,int devolucion,Integer origen,String estado,int version,BigDecimal sugerido,BigDecimal importe,String motivo,String respuesta,LocalDateTime creado) { }
    public record Resumen(BigDecimal disponible,BigDecimal pendiente,BigDecimal pagado,int ajustes) { public BigDecimal generado(){return disponible.add(pendiente).add(pagado);} }
    public record Recaudacion(BigDecimal ingreso,BigDecimal devolucion,BigDecimal transferido) { }
    private final Connection conn;
    private final TutoriaDAO base;
    public LiquidacionesDAO(Connection c){this(c,Clock.system(ReglasTutoria.ZONA));}
    public LiquidacionesDAO(Connection c,Clock clock){conn=c;base=new TutoriaDAO(c,clock);}
    public static BigDecimal parte(BigDecimal precio,BigDecimal porcentaje){return precio.multiply(porcentaje).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);}
    private static BigDecimal cero(){return new BigDecimal("0.00");}
    private void permiso(int docente,int actor,Rol rol){if(rol!=Rol.ADMIN&&(rol!=Rol.DOCENTE||actor!=docente))throw new IllegalArgumentException("Tu cuenta no tiene acceso a estos ingresos.");}
    private void admin(Rol rol){if(rol!=Rol.ADMIN)throw new IllegalArgumentException("Solo administración puede gestionar liquidaciones.");}
    private LocalDate corte(String valor){try{return LocalDate.parse(valor);}catch(RuntimeException e){throw new IllegalArgumentException("Indica una fecha de corte válida.");}}
    private ConfiguracionLiquidacionesDAO.Configuracion validarCorte(LocalDate corte){
        var c=new ConfiguracionLiquidacionesDAO(conn).obtener();
        if(!c.completa())throw new IllegalArgumentException("Configura el día de corte y de pago antes de preparar una liquidación.");
        if(corte==null||!corte.isBefore(base.ahora().toLocalDate())||corte.getDayOfWeek().getValue()!=c.corte())throw new IllegalArgumentException("El corte debe ser un día semanal ya terminado, según el calendario configurado.");
        return c;
    }
    // All flows use student agendas, teacher agenda, session, payment, then closing/adjustment.
    private List<Integer> alumnos(int docente){
        List<Integer> alumnos=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select distinct userId from tutoria where docenteId=? order by userId")){s.setInt(1,docente);try(ResultSet r=s.executeQuery()){while(r.next())alumnos.add(r.getInt(1));}return alumnos;}catch(SQLException e){throw failure(e);}
    }
    private void bloquearProfesor(int docente,List<Integer> alumnos)throws SQLException{
        for(int alumno:alumnos)base.lockAlumno(alumno);base.lockDocente(docente);
        try(PreparedStatement s=conn.prepareStatement("select id from docente where id=? for update")){s.setInt(1,docente);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("No se encuentra al profesor.");}}
        try(PreparedStatement s=conn.prepareStatement("select id from tutoria where docenteId=? order by id for update")){s.setInt(1,docente);try(ResultSet r=s.executeQuery()){while(r.next()){} }}
        try(PreparedStatement s=conn.prepareStatement("select p.tutoria_id from pago_alumno p join tutoria t on t.id=p.tutoria_id where t.docenteId=? order by p.tutoria_id for update")){s.setInt(1,docente);try(ResultSet r=s.executeQuery()){while(r.next()){} }}
    }
    private String impedimento(Tutoria t,PagosDAO.Pago p){
        LocalDateTime inicio=ReglasTutoria.inicio(t);
        if(t.getPrecio()==null||t.getPorcentajeDocente()==null||t.getCursoNombre()==null)return "Registro anterior sin tarifa o reparto verificable";
        if(!t.getEstado().equals("Completada")||!t.getAsistencia().equals("Realizada")||inicio==null||base.ahora().isBefore(inicio.plusMinutes(t.getDuracionMinutos())))return "La sesión no está registrada como realizada y terminada";
        if(p==null||p.validado()==null||p.importe().compareTo(t.getPrecio())!=0)return "El ingreso del alumno no está validado por el total";
        return bloqueoEconomico(t,p);
    }
    private String bloqueoEconomico(Tutoria t,PagosDAO.Pago p){
        if(t.isCancelacionSolicitada())return "Solicitud de cancelación o devolución sin resolver";
        if(t.getEstado().equals("Incidencia"))return "Incidencia pendiente de resolución";
        if(p==null||p.validado()==null||!Set.of("Validado","Devuelto").contains(p.estado())||t.isPagoRevision())return "Pago o devolución pendiente de resolución";
        try(PreparedStatement s=conn.prepareStatement("select id from liquidacion_ajuste where tutoria_id=? and estado not in ('Aceptado','Asumido por equipo') limit 1")){s.setInt(1,t.getId());try(ResultSet r=s.executeQuery()){if(r.next())return "Hay un ajuste de devolución que debe acordarse primero";}}
        catch(SQLException e){throw failure(e);}return null;
    }
    private Integer asignacion(int tutoria){try(PreparedStatement s=conn.prepareStatement("select liquidacion_id from liquidacion_sesion where tutoria_id=?")){s.setInt(1,tutoria);try(ResultSet r=s.executeQuery()){return r.next()?r.getInt(1):null;}}catch(SQLException e){throw failure(e);}}
    private Vista disponibles(int docente,LocalDate hasta,String calendario){
        List<Linea> lineas=new ArrayList<>();List<Retenida> retenidas=new ArrayList<>();PagosDAO pagos=new PagosDAO(conn);
        List<Ajuste> ajustes=ajustes(docente,docente,Rol.DOCENTE);
        for(Tutoria t:base.getAllTutoriaByLoginDocente(docente)){
            LocalDateTime inicio=ReglasTutoria.inicio(t);if(inicio==null||inicio.toLocalDate().isAfter(hasta)||asignacion(t.getId())!=null)continue;
            var p=pagos.obtener(t.getId());String bloqueo=impedimento(t,p);
            if(bloqueo==null&&ajustes.stream().anyMatch(a->a.tutoria()==t.getId()&&a.estado().equals("Aceptado")&&!ajusteAsignado(a.id())&&a.creado().toLocalDate().isAfter(hasta)))bloqueo="El ajuste de esta sesión corresponde a un corte posterior";
            if(bloqueo!=null){if(t.getEstado().equals("Completada")||(p!=null&&p.validado()!=null))retenidas.add(new Retenida(t.getId(),t.getCursoNombre(),bloqueo));continue;}
            lineas.add(new Linea(t.getId(),null,"Sesión",t.getCursoNombre(),inicio.toLocalDate(),t.getPrecio(),t.getPorcentajeDocente(),pagos.devuelto(t.getId()),parte(t.getPrecio(),t.getPorcentajeDocente()),"Asesoría realizada e ingreso validado"));
        }
        for(Ajuste a:ajustes){
            if(!a.estado().equals("Aceptado")||a.creado().toLocalDate().isAfter(hasta)||ajusteAsignado(a.id()))continue;
            Tutoria t=base.getTutoriaById(a.tutoria());var p=pagos.obtener(t.getId());if(bloqueoEconomico(t,p)!=null)continue;
            // Do not recover an adjustment from another closing before its original remuneration is settled.
            Integer original=asignacion(t.getId());Lote loteOriginal=original==null?null:obtener(original,0,Rol.ADMIN);
            boolean originalLiquidado=loteOriginal!=null&&Set.of("Pagada","Compensada").contains(loteOriginal.estado());
            if(!originalLiquidado&&lineas.stream().noneMatch(l->l.tutoria()==t.getId()&&l.ajuste()==null))continue;
            lineas.add(new Linea(t.getId(),a.id(),"Ajuste",t.getCursoNombre(),ReglasTutoria.inicio(t).toLocalDate(),t.getPrecio(),t.getPorcentajeDocente(),pagos.devuelto(t.getId()),a.importe(),"Ajuste #"+a.id()+" aceptado por el profesor; devolución #"+a.devolucion()));
        }
        lineas.sort(Comparator.comparingInt(Linea::tutoria).thenComparing(l->l.ajuste()==null?0:l.ajuste()));
        BigDecimal total=lineas.stream().map(Linea::importe).reduce(cero(),BigDecimal::add);
        return new Vista(List.copyOf(lineas),List.copyOf(retenidas),total,PagosDAO.hash(docente+"|"+hasta+"|"+calendario+"|"+lineas));
    }
    public Vista preparar(int docente,String fecha,int actor,Rol rol){permiso(docente,actor,rol);LocalDate corte=corte(fecha);var c=validarCorte(corte);return disponibles(docente,corte,c.corte()+"/"+c.pago());}
    public Vista disponiblesProfesor(int docente,int actor,Rol rol){permiso(docente,actor,rol);return disponibles(docente,base.ahora().toLocalDate(),"Consulta de ingresos");}
    public int crear(int docente,String fecha,String huella,Rol rol){
        admin(rol);LocalDate corte=corte(fecha);int[] id={0};List<Integer> alumnos=alumnos(docente);
        base.transaction(()->{
            bloquearProfesor(docente,alumnos);var c=validarCorte(corte);Vista v=disponibles(docente,corte,c.corte()+"/"+c.pago());
            if(huella==null||!huella.equals(v.huella()))throw new IllegalArgumentException("Cambió el detalle desde la revisión. Vuelve a preparar el cierre antes de confirmarlo.");
            if(v.lineas().isEmpty())throw new IllegalArgumentException("Este profesor no tiene sesiones o ajustes aptos para este corte.");
            if(v.total().signum()<0)throw new IllegalArgumentException("El ajuste aceptado supera los ingresos disponibles. Se conserva para compensarlo con futuras sesiones; no se registra una transferencia negativa.");
            String nombre=new DocenteDAO(conn).getDocenteById(docente).getNombre();
            try(PreparedStatement s=conn.prepareStatement("insert into liquidacion(docente_id,docente_nombre,corte,pago_previsto,creada_en,total) values(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                s.setInt(1,docente);s.setString(2,nombre);s.setObject(3,corte);s.setObject(4,c.pagoPrevisto(corte));s.setObject(5,base.ahora());s.setBigDecimal(6,v.total());s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();id[0]=r.getInt(1);}
            }
            for(Linea l:v.lineas()){
                try(PreparedStatement s=conn.prepareStatement("insert into liquidacion_linea(liquidacion_id,tutoria_id,ajuste_id,tipo,curso,fecha_sesion,precio,porcentaje,devuelto_corte,importe,descripcion) values(?,?,?,?,?,?,?,?,?,?,?)")){
                    s.setInt(1,id[0]);s.setInt(2,l.tutoria());s.setObject(3,l.ajuste());s.setString(4,l.tipo());s.setString(5,l.curso());s.setObject(6,l.fecha());s.setBigDecimal(7,l.precio());s.setBigDecimal(8,l.porcentaje());s.setBigDecimal(9,l.devuelto());s.setBigDecimal(10,l.importe());s.setString(11,l.descripcion());s.executeUpdate();
                }
                String sql=l.ajuste()==null?"insert into liquidacion_sesion(tutoria_id,liquidacion_id) values(?,?)":"insert into liquidacion_ajuste_asignacion(ajuste_id,liquidacion_id) values(?,?)";
                try(PreparedStatement s=conn.prepareStatement(sql)){s.setInt(1,l.ajuste()==null?l.tutoria():l.ajuste());s.setInt(2,id[0]);s.executeUpdate();}
            }
            evento(conn,id[0],null,docente,"ADMIN","Cierre preparado","Cierre hasta "+corte+". "+v.lineas().size()+" líneas; total S/ "+v.total()+". No acredita una transferencia.",base.ahora());return true;
        });return id[0];
    }
    public Lote obtener(int id,int actor,Rol rol){
        if(rol!=Rol.ADMIN&&rol!=Rol.DOCENTE)return null;
        try(PreparedStatement s=conn.prepareStatement("select * from liquidacion where id=?"+(rol==Rol.DOCENTE?" and docente_id=?":""))){s.setInt(1,id);if(rol==Rol.DOCENTE)s.setInt(2,actor);try(ResultSet r=s.executeQuery()){return r.next()?lote(r):null;}}catch(SQLException e){throw failure(e);}
    }
    private Lote lote(ResultSet r)throws SQLException{return new Lote(r.getInt("id"),r.getInt("docente_id"),r.getString("docente_nombre"),r.getObject("corte",LocalDate.class),r.getObject("pago_previsto",LocalDate.class),r.getObject("creada_en",LocalDateTime.class),r.getString("estado"),r.getInt("version"),r.getBigDecimal("total"),r.getString("motivo"),r.getObject("transferida_en",LocalDateTime.class),r.getString("referencia"),r.getString("destino"));}
    public List<Linea> lineas(int id,int actor,Rol rol){if(obtener(id,actor,rol)==null)return List.of();List<Linea> result=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select * from liquidacion_linea where liquidacion_id=? order by id")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new Linea(r.getInt("tutoria_id"),(Integer)r.getObject("ajuste_id"),r.getString("tipo"),r.getString("curso"),r.getObject("fecha_sesion",LocalDate.class),r.getBigDecimal("precio"),r.getBigDecimal("porcentaje"),r.getBigDecimal("devuelto_corte"),r.getBigDecimal("importe"),r.getString("descripcion")));}return result;}catch(SQLException e){throw failure(e);}}
    private void verificar(Lote lote){
        PagosDAO pagos=new PagosDAO(conn);
        for(Linea l:lineas(lote.id(),0,Rol.ADMIN)){
            Tutoria t=base.getTutoriaById(l.tutoria());var p=pagos.obtener(l.tutoria());String error=l.ajuste()==null?impedimento(t,p):bloqueoEconomico(t,p);
            if(error!=null)throw new IllegalArgumentException("No se puede pagar: asesoría #"+l.tutoria()+". "+error+". Resuelve el caso o anula y prepara otro cierre.");
            if(l.precio().compareTo(t.getPrecio())!=0||l.porcentaje().compareTo(t.getPorcentajeDocente())!=0||l.devuelto().compareTo(pagos.devuelto(t.getId()))!=0)throw new IllegalArgumentException("Cambió el importe o una devolución de la asesoría #"+t.getId()+". Anula este cierre y prepara uno actualizado.");
            if(l.ajuste()!=null){
                Ajuste a=ajuste(l.ajuste());if(!a.estado().equals("Aceptado")||a.importe().compareTo(l.importe())!=0)throw new IllegalArgumentException("El ajuste ya no coincide con el cierre.");
                Integer original=asignacion(t.getId());Lote origen=original==null?null:obtener(original,0,Rol.ADMIN);
                if(origen==null||(origen.id()!=lote.id()&&!Set.of("Pagada","Compensada").contains(origen.estado())))throw new IllegalArgumentException("Primero debe resolverse el cierre original de este ajuste.");
            }
        }
    }
    public boolean gestionar(int id,int version,Rol rol,String accion,String monto,String referencia,String destino,LocalDateTime fecha,String motivo){
        admin(rol);Lote initial=obtener(id,0,Rol.ADMIN);if(initial==null)return false;
        List<Integer> alumnos=alumnos(initial.docente());
        return base.transaction(()->{
            bloquearProfesor(initial.docente(),alumnos);Lote l;
            try(PreparedStatement s=conn.prepareStatement("select * from liquidacion where id=? for update")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){r.next();l=lote(r);}}
            if(l.version()!=version||!l.estado().equals("Pendiente"))throw new IllegalArgumentException("Este cierre ya cambió o dejó de estar pendiente. Actualiza la página.");
            String detalle;
            if("anular".equals(accion)){
                detalle=OfertaDAO.texto(motivo,850,"Motivo de la anulación");
                try(PreparedStatement s=conn.prepareStatement("update liquidacion set estado='Anulada',motivo=?,version=version+1 where id=?")){s.setString(1,detalle);s.setInt(2,id);s.executeUpdate();}
                for(String tabla:List.of("liquidacion_sesion","liquidacion_ajuste_asignacion"))try(PreparedStatement s=conn.prepareStatement("delete from "+tabla+" where liquidacion_id=?")){s.setInt(1,id);s.executeUpdate();}
                detalle="Cierre anulado sin transferencia; las líneas se conservan y quedan disponibles para otro cierre. "+detalle;
            }else if("pagar".equals(accion)){
                verificar(l);if(l.total().signum()==0)throw new IllegalArgumentException("Este cierre tiene saldo cero. Registra la compensación sin transferencia.");
                if(PagosDAO.dinero(monto).compareTo(l.total())!=0)throw new IllegalArgumentException("La transferencia debe coincidir con el total del cierre.");
                String ref=OfertaDAO.texto(referencia,100,"Referencia de la transferencia"),cuenta=OfertaDAO.texto(destino,200,"Destino de la transferencia");
                if(!ref.matches("[\\p{L}\\p{N} ._/#-]{2,100}")||!ref.matches(".*[\\p{L}\\p{N}].*"))throw new IllegalArgumentException("Indica una referencia válida de la transferencia.");
                if(fecha==null||fecha.isAfter(base.ahora())||fecha.isBefore(l.creado().withSecond(0).withNano(0)))throw new IllegalArgumentException("La fecha real de la transferencia debe estar entre la creación del cierre y el momento actual (Lima).");
                try(PreparedStatement s=conn.prepareStatement("update liquidacion set estado='Pagada',transferida_en=?,registrada_en=?,referencia=?,transferencia_clave=?,destino=?,version=version+1 where id=?")){
                    s.setObject(1,fecha);s.setObject(2,base.ahora());s.setString(3,ref);s.setString(4,PagosDAO.hash(ref.toUpperCase(Locale.ROOT).replaceAll("[\\s._/#-]","")));s.setString(5,cuenta);s.setInt(6,id);s.executeUpdate();
                }catch(SQLIntegrityConstraintViolationException e){throw new IllegalArgumentException("La referencia de transferencia ya está registrada en otra liquidación.");}
                detalle="Transferencia externa registrada por S/ "+l.total()+"; referencia "+ref+"; fecha real "+fecha+" (Lima).";
            }else if("compensar".equals(accion)){
                verificar(l);if(l.total().signum()!=0)throw new IllegalArgumentException("Solo puede compensarse sin transferencia un cierre con total cero.");
                detalle=OfertaDAO.texto(motivo,850,"Explica la compensación");
                try(PreparedStatement s=conn.prepareStatement("update liquidacion set estado='Compensada',motivo=?,registrada_en=?,version=version+1 where id=?")){s.setString(1,detalle);s.setObject(2,base.ahora());s.setInt(3,id);s.executeUpdate();}
                detalle="Saldo cero compensado con ajustes aceptados. No hubo transferencia. "+detalle;
            }else throw new IllegalArgumentException("Acción de liquidación inválida.");
            evento(conn,id,null,l.docente(),"ADMIN",accion,detalle,base.ahora());return true;
        });
    }
    public List<Lote> listar(int docente,int actor,Rol rol,int pagina){
        permiso(docente,actor,rol);List<Lote> result=new ArrayList<>();
        int offset=(Math.max(1,Math.min(pagina,100000))-1)*20;
        try(PreparedStatement s=conn.prepareStatement("select * from liquidacion where docente_id=? order by id desc limit 21 offset ?")){s.setInt(1,docente);s.setInt(2,offset);try(ResultSet r=s.executeQuery()){while(r.next())result.add(lote(r));}return result;}catch(SQLException e){throw failure(e);}
    }
    private boolean ajusteAsignado(int id){try(PreparedStatement s=conn.prepareStatement("select ajuste_id from liquidacion_ajuste_asignacion where ajuste_id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next();}}catch(SQLException e){throw failure(e);}}
    private Ajuste ajuste(ResultSet r)throws SQLException{return new Ajuste(r.getInt("id"),r.getInt("tutoria_id"),r.getInt("docenteId"),r.getInt("devolucion_id"),(Integer)r.getObject("origen_liquidacion_id"),r.getString("estado"),r.getInt("version"),r.getBigDecimal("sugerido"),r.getBigDecimal("importe"),r.getString("motivo"),r.getString("respuesta"),r.getObject("creado_en",LocalDateTime.class));}
    private Ajuste ajuste(int id){try(PreparedStatement s=conn.prepareStatement("select a.*,t.docenteId from liquidacion_ajuste a join tutoria t on t.id=a.tutoria_id where a.id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){return r.next()?ajuste(r):null;}}catch(SQLException e){throw failure(e);}}
    public List<Ajuste> ajustes(int docente,int actor,Rol rol){permiso(docente,actor,rol);List<Ajuste> rows=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select a.*,t.docenteId from liquidacion_ajuste a join tutoria t on t.id=a.tutoria_id where t.docenteId=? order by a.id desc")){s.setInt(1,docente);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(ajuste(r));}return rows;}catch(SQLException e){throw failure(e);}}
    public boolean gestionarAjuste(int id,int actor,Rol rol,int version,String accion,String monto,String motivo){
        if(rol!=Rol.ADMIN&&rol!=Rol.DOCENTE)throw new IllegalArgumentException("Tu cuenta no puede responder ajustes.");
        Ajuste initial=ajuste(id);if(initial==null||(rol==Rol.DOCENTE&&initial.docente()!=actor))return false;
        List<Integer> alumnos=alumnos(initial.docente());
        return base.transaction(()->{
            bloquearProfesor(initial.docente(),alumnos);Ajuste a=ajuste(id);
            if(a.version()!=version||ajusteAsignado(id)||a.estado().equals("Asumido por equipo"))throw new IllegalArgumentException("El ajuste cambió o ya forma parte de un cierre. Actualiza la página.");
            String texto=OfertaDAO.texto(motivo,850,"Motivo de la decisión"),estado;
            if("proponer".equals(accion)){
                admin(rol);if(a.estado().equals("Aceptado"))throw new IllegalArgumentException("El ajuste ya fue aceptado. Se conserva el acuerdo registrado.");
                BigDecimal importe;
                try{if(monto==null||!monto.matches("-?[0-9]{1,8}(\\.[0-9]{1,2})?"))throw new NumberFormatException();importe=new BigDecimal(monto).setScale(2);}catch(RuntimeException e){throw new IllegalArgumentException("Indica cero si asume el equipo o un ajuste negativo con hasta dos decimales.");}
                if(importe.signum()>0||importe.compareTo(a.sugerido())<0)throw new IllegalArgumentException("El ajuste debe estar entre "+a.sugerido()+" y cero. No puede reducir más que la parte proporcional de esta devolución.");
                estado=importe.signum()==0?"Asumido por equipo":"Propuesto";
                try(PreparedStatement s=conn.prepareStatement("update liquidacion_ajuste set estado=?,importe=?,motivo=?,respuesta='',aceptado_en=null,version=version+1 where id=?")){s.setString(1,estado);s.setBigDecimal(2,importe);s.setString(3,texto);s.setInt(4,id);s.executeUpdate();}
                texto=(importe.signum()==0?"El equipo asume el costo, sin reducir la remuneración del profesor. ":"Propuesta de ajuste S/ "+importe+". Requiere aceptación del profesor. ")+texto;
            }else if(("aceptar".equals(accion)||"revisar".equals(accion))&&rol==Rol.DOCENTE&&a.estado().equals("Propuesto")){
                estado=accion.equals("aceptar")?"Aceptado":"En revisión";
                try(PreparedStatement s=conn.prepareStatement("update liquidacion_ajuste set estado=?,respuesta=?,aceptado_en=?,version=version+1 where id=?")){s.setString(1,estado);s.setString(2,texto);s.setObject(3,estado.equals("Aceptado")?base.ahora():null);s.setInt(4,id);s.executeUpdate();}
                texto="Respuesta del profesor: "+estado+". "+texto;
            }else throw new IllegalArgumentException("La acción no está disponible para este ajuste.");
            evento(conn,null,id,a.docente(),rol.name(),accion,texto,base.ahora());return true;
        });
    }
    /** Called inside the student's refund transaction, with the same agenda locks held. */
    static void registrarDevolucion(Connection c,Tutoria t,int devolucion,LocalDateTime now)throws SQLException{
        Integer origen=null;try(PreparedStatement s=c.prepareStatement("select liquidacion_id from liquidacion_sesion where tutoria_id=?")){s.setInt(1,t.getId());try(ResultSet r=s.executeQuery()){if(r.next())origen=r.getInt(1);}}
        if((!t.getEstado().equals("Completada")&&origen==null)||t.getPrecio()==null||t.getPorcentajeDocente()==null)return;
        BigDecimal total=cero(),anterior=cero();
        try(PreparedStatement s=c.prepareStatement("select id,importe from pago_devolucion where tutoria_id=? and ejecutada_en is not null and id<=? order by id")){s.setInt(1,t.getId());s.setInt(2,devolucion);try(ResultSet r=s.executeQuery()){while(r.next()){total=total.add(r.getBigDecimal(2));if(r.getInt(1)!=devolucion)anterior=anterior.add(r.getBigDecimal(2));}}}
        BigDecimal sugerido=parte(t.getPrecio().subtract(total),t.getPorcentajeDocente()).subtract(parte(t.getPrecio().subtract(anterior),t.getPorcentajeDocente()));
        int id;try(PreparedStatement s=c.prepareStatement("insert into liquidacion_ajuste(tutoria_id,devolucion_id,origen_liquidacion_id,sugerido,creado_en) values(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){s.setInt(1,t.getId());s.setInt(2,devolucion);s.setObject(3,origen);s.setBigDecimal(4,sugerido);s.setObject(5,now);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();id=r.getInt(1);}}
        evento(c,null,id,t.getDocenteId(),"SISTEMA","Devolución por revisar","La devolución #"+devolucion+" requiere decidir quién asume su efecto económico. No se aplica un descuento automático al profesor.",now);
    }
    public Resumen resumen(int docente,int actor,Rol rol){
        permiso(docente,actor,rol);BigDecimal pendiente=cero(),pagado=cero();
        try(PreparedStatement s=conn.prepareStatement("select estado,sum(total) from liquidacion where docente_id=? and estado in ('Pendiente','Pagada') group by estado")){s.setInt(1,docente);try(ResultSet r=s.executeQuery()){while(r.next())if(r.getString(1).equals("Pendiente"))pendiente=r.getBigDecimal(2);else pagado=r.getBigDecimal(2);}}
        catch(SQLException e){throw failure(e);}
        int pendientes=0;for(Ajuste a:ajustes(docente,actor,rol))if(!Set.of("Aceptado","Asumido por equipo").contains(a.estado()))pendientes++;
        return new Resumen(disponibles(docente,base.ahora().toLocalDate(),"Resumen").total(),pendiente,pagado,pendientes);
    }
    public Recaudacion recaudacion(Rol rol){admin(rol);try{return new Recaudacion(suma("select coalesce(sum(importe),0) from pago_alumno where validado_en is not null"),suma("select coalesce(sum(importe),0) from pago_devolucion where ejecutada_en is not null"),suma("select coalesce(sum(total),0) from liquidacion where estado='Pagada'"));}catch(SQLException e){throw failure(e);}}
    private BigDecimal suma(String sql)throws SQLException{try(PreparedStatement s=conn.prepareStatement(sql);ResultSet r=s.executeQuery()){r.next();return r.getBigDecimal(1);}}
    public List<GestionAsesoriasDAO.Evento> historial(int lote,Integer ajuste,int actor,Rol rol){
        if(ajuste==null){if(obtener(lote,actor,rol)==null)return List.of();}else{Ajuste a=ajuste(ajuste);if(a==null)return List.of();permiso(a.docente(),actor,rol);}
        List<GestionAsesoriasDAO.Evento> rows=new ArrayList<>();try(PreparedStatement s=conn.prepareStatement("select actor_rol,accion,detalle,ocurrido_en from liquidacion_evento where "+(ajuste==null?"liquidacion_id=?":"ajuste_id=?")+" order by id desc")){s.setInt(1,ajuste==null?lote:ajuste);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new GestionAsesoriasDAO.Evento(r.getString(1),r.getString(2),r.getString(3),r.getObject(4,LocalDateTime.class)));}return rows;}catch(SQLException e){throw failure(e);}
    }
    static void evento(Connection c,Integer lote,Integer ajuste,Integer docente,String rol,String accion,String detalle,LocalDateTime now)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("insert into liquidacion_evento(liquidacion_id,ajuste_id,docente_id,actor_rol,accion,detalle,ocurrido_en) values(?,?,?,?,?,?,?)")){s.setObject(1,lote);s.setObject(2,ajuste);s.setObject(3,docente);s.setString(4,rol);s.setString(5,accion);s.setString(6,detalle);s.setObject(7,now);s.executeUpdate();}
    }
    private IllegalStateException failure(SQLException e){return new IllegalStateException("No se pudo completar la liquidación.",e);}
}
