package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.ConfiguracionCorreo;
import com.tme.service.ReglasTutoria;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Transactional outbox: enqueue with the business change, send only after commit. */
public final class CorreoDAO {
    public record Mensaje(long id,int tutoria,String tipo,LocalDateTime referencia,String destino,String rol,
            String asunto,String cuerpo,String uuid,String token,int intentos,LocalDateTime primero,LocalDateTime caduca,boolean incierto) { }
    public record Fila(long id,int tutoria,String destino,String asunto,String estado,int intentos,LocalDateTime proximo,String error) { }
    private final Connection conn;
    private final String administrador;
    public CorreoDAO(Connection conn){this(conn,System.getenv("ADMIN_EMAIL"));}
    public CorreoDAO(Connection conn,String administrador){this.conn=conn;this.administrador=administrador;}
    private static final Set<String> TODOS=Set.of("ALUMNO","DOCENTE","ADMIN");
    private static final Map<String,String> TITULOS=Map.ofEntries(
        Map.entry("Reserva","Horario reservado: completa el pago"),
        Map.entry("Comprobante recibido","Comprobante recibido para revisión"),
        Map.entry("Ingreso validado","Ingreso verificado por administración"),
        Map.entry("Vencimiento","Actualización por vencimiento de la reserva"),
        Map.entry("observar","Comprobante observado: consulta la corrección"),
        Map.entry("aprobar_devolucion","Devolución aprobada, pendiente de transferencia"),
        Map.entry("ejecutar_devolucion","Transferencia de devolución registrada"),
        Map.entry("cancelar","Actualización de cancelación de asesoría"),
        Map.entry("mantener","Respuesta a la solicitud de cancelación"),
        Map.entry("Solicitud de revisión","Solicitud de cancelación o devolución recibida"),
        Map.entry("proponer","Propuesta de cambio de horario"),
        Map.entry("aceptar_cambio","Actualización de la propuesta de horario"),
        Map.entry("rechazar_cambio","Propuesta de horario cerrada"),
        Map.entry("Propuesta vencida","Propuesta de horario vencida"),
        Map.entry("enlace","Enlace de clase disponible o actualizado"),
        Map.entry("incidencia","Incidencia de asesoría para revisar"),
        Map.entry("resolver","Resolución de incidencia registrada"),
        Map.entry("completar","Realización de asesoría registrada"),
        Map.entry("Cobro habilitado","Instrucciones de pago disponibles")
    );
    public void evento(int tutoria,String origen,String accion,LocalDateTime ahora)throws SQLException {
        String titulo=TITULOS.get(accion);if(titulo==null)return;
        Set<String> roles=switch(accion){
            case "Reserva","Ingreso validado","Propuesta vencida","proponer","aceptar_cambio","rechazar_cambio","completar"->Set.of("ALUMNO","DOCENTE");
            case "Comprobante recibido","observar","aprobar_devolucion","ejecutar_devolucion"->Set.of("ALUMNO","ADMIN");
            case "enlace","Cobro habilitado"->Set.of("ALUMNO");
            default->TODOS;
        };
        Tutoria t=new TutoriaDAO(conn).getTutoriaById(tutoria);if(t==null)return;
        if(accion.equals("Ingreso validado")&&t.getEstado().equals("Incidencia"))roles=TODOS;
        String cuerpo=titulo+".\nAsesoría #"+tutoria+".\n"
            +"Consulta los detalles y el estado actualizado en tu cuenta.";
        if(accion.equals("Reserva"))cuerpo+="\nEnvía el comprobante antes de "+fecha(t.getVencePago())+" (Lima). La clase aún no está confirmada.";
        if(accion.equals("Comprobante recibido"))cuerpo+="\nLa clase se confirma después de verificar el ingreso real.";
        encolar(t,origen,accion,null,titulo,cuerpo,roles,ahora,ahora.plusHours(24));
    }
    private String fecha(LocalDateTime fecha){return fecha==null?"consultar en la plataforma":fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
    private Map<String,String> destinatarios(int tutoria,Set<String> roles)throws SQLException {
        Map<String,String> destinos=new LinkedHashMap<>();
        try(PreparedStatement s=conn.prepareStatement("select u.email,d.email from tutoria t join user_details u on u.id=t.userId join docente d on d.id=t.docenteId where t.id=?")){
            s.setInt(1,tutoria);try(ResultSet r=s.executeQuery()){if(r.next()){
                if(roles.contains("ALUMNO"))agregar(destinos,r.getString(1),"ALUMNO");
                if(roles.contains("DOCENTE"))agregar(destinos,r.getString(2),"DOCENTE");
            }}
        }
        if(roles.contains("ADMIN"))agregar(destinos,administrador,"ADMIN");return destinos;
    }
    private void agregar(Map<String,String> destinos,String correo,String rol){
        if(ConfiguracionCorreo.emailValido(correo))destinos.putIfAbsent(correo.strip().toLowerCase(Locale.ROOT),rol);
    }
    private void encolar(Tutoria t,String origen,String tipo,LocalDateTime referencia,String asunto,String cuerpo,
            Set<String> roles,LocalDateTime ahora,LocalDateTime caduca)throws SQLException {
        for(var destino:destinatarios(t.getId(),roles).entrySet()){
            String clave=hash(origen+"|"+destino.getKey());
            String asuntoPersonal=asunto,cuerpoPersonal=cuerpo;
            if(tipo.equals("Reserva")&&destino.getValue().equals("DOCENTE")){
                asuntoPersonal="Nueva reserva pendiente de pago";
                cuerpoPersonal="Se reservó uno de tus horarios para la asesoría #"+t.getId()+". No necesitas aceptar esta reserva. Consulta la fecha y el estado en tu cuenta; la clase se confirma cuando administración valida el ingreso.";
            }
            if(tipo.equals("Comprobante recibido")&&destino.getValue().equals("ADMIN")){
                asuntoPersonal="Comprobante de alumno pendiente de revisión";
                cuerpoPersonal="Revisa el ingreso real y registra la validación u observación del comprobante de la asesoría #"+t.getId()+". Fecha prevista de revisión: "+fecha(t.getRevisionPrevista())+" (Lima).";
            }
            try(PreparedStatement s=conn.prepareStatement("insert into correo_pendiente(clave,tutoria_id,tipo,referencia,destino,rol,asunto,cuerpo,uuid,creado_en,programado_en,caduca_en) values(?,?,?,?,?,?,?,?,?,?,?,?) on duplicate key update clave=clave")){
                s.setString(1,clave);s.setInt(2,t.getId());s.setString(3,tipo);s.setObject(4,referencia);s.setString(5,destino.getKey());s.setString(6,destino.getValue());
                s.setString(7,"TeachMate · "+asuntoPersonal+" · #"+t.getId());s.setString(8,cuerpoPersonal);s.setString(9,UUID.randomUUID().toString());
                s.setObject(10,ahora);s.setObject(11,ahora);s.setObject(12,caduca);s.executeUpdate();
            }
            // Returning to a previous schedule can make an unsent, automatically discarded reminder valid again.
            // Never revive an accepted, manually cancelled or uncertain delivery.
            if(tipo.startsWith("Recordatorio "))try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado='Pendiente',intentos=0,primer_intento=null,uuid=?,programado_en=?,ultimo_error='' where clave=? and estado='Cancelado' and ultimo_error='El aviso ya no corresponde al estado o destinatario actual' and incierto=false and caduca_en>?")){
                s.setString(1,UUID.randomUUID().toString());s.setObject(2,ahora);s.setString(3,clave);s.setObject(4,ahora);s.executeUpdate();
            }
        }
    }
    private String hash(String texto){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public void recordatorios(LocalDateTime ahora) {
        List<Integer> ids=new ArrayList<>();
        try(PreparedStatement s=conn.prepareStatement("select id from tutoria where (estado='Confirmada' and ftutoria>=? and ftutoria<=?) or (estado='Pendiente de pago' and pago_revision=false and vence_pago>? and vence_pago<=?) or (estado='Pendiente de pago' and pago_revision=true and revision_prevista<=?) order by ftutoria,hora_inicio,id limit 1000")){
            s.setString(1,ahora.toLocalDate().toString());s.setString(2,ahora.plusHours(1).toLocalDate().toString());
            s.setObject(3,ahora);s.setObject(4,ahora.plusMinutes(30));s.setObject(5,ahora.plusMinutes(30));
            try(ResultSet r=s.executeQuery()){while(r.next())ids.add(r.getInt(1));}
            for(int id:ids){
                Tutoria t=new TutoriaDAO(conn).getTutoriaById(id);LocalDateTime inicio=ReglasTutoria.inicio(t);
                if(t.getEstado().equals("Confirmada")&&inicio!=null&&inicio.isAfter(ahora)&&!inicio.isAfter(ahora.plusHours(1)))
                    recordar(t,"Recordatorio clase",inicio,"Tu asesoría se acerca","Tu clase confirmada comienza el "+fecha(inicio)+" (Lima). Consulta el enlace y cualquier actualización en la plataforma.",Set.of("ALUMNO","DOCENTE"),ahora,inicio);
                if(t.getEstado().equals("Pendiente de pago")&&!t.isPagoRevision()&&t.getVencePago()!=null&&t.getVencePago().isAfter(ahora)&&!t.getVencePago().isAfter(ahora.plusMinutes(30)))
                    recordar(t,"Recordatorio pago",t.getVencePago(),"Tu plazo de pago está por vencer","Envía el comprobante antes de "+fecha(t.getVencePago())+" (Lima). Si ya lo enviaste, consulta el estado actualizado.",Set.of("ALUMNO"),ahora,t.getVencePago());
                if(t.getEstado().equals("Pendiente de pago")&&t.isPagoRevision()&&t.getRevisionPrevista()!=null&&!t.getRevisionPrevista().isAfter(ahora.plusMinutes(30))&&inicio!=null&&inicio.isAfter(ahora))
                    recordar(t,"Recordatorio revisión",t.getRevisionPrevista(),"Revisión de comprobante pendiente","Revisa el ingreso real. Fecha prevista de revisión: "+fecha(t.getRevisionPrevista())+" (Lima).",Set.of("ADMIN"),ahora,inicio);
            }
        }catch(SQLException e){throw fallo(e);}
    }
    private void recordar(Tutoria t,String tipo,LocalDateTime referencia,String titulo,String cuerpo,Set<String> roles,LocalDateTime ahora,LocalDateTime caduca)throws SQLException {
        encolar(t,"r:"+tipo+":"+t.getId()+":"+referencia,tipo,referencia,titulo,cuerpo,roles,ahora,caduca);
    }
    public void mantener(LocalDateTime ahora) {
        try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado='Dudoso',incierto=true,token=null,bloqueo_hasta=null,ultimo_error='Envío interrumpido; verificar en Brevo antes de reintentar' where estado='Enviando' and bloqueo_hasta<=?")){
            s.setObject(1,ahora);s.executeUpdate();
        }catch(SQLException e){throw fallo(e);}
        try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado='Cancelado',ultimo_error='Aviso fuera de plazo' where estado in ('Pendiente','Fallido','Dudoso') and caduca_en<=?")){
            s.setObject(1,ahora);s.executeUpdate();
        }catch(SQLException e){throw fallo(e);}
    }
    public Mensaje reclamar(LocalDateTime ahora) {
        for(int i=0;i<20;i++){
            Mensaje[] encontrado=new Mensaje[1];
            new TutoriaDAO(conn).transaction(()->{
                try(PreparedStatement s=conn.prepareStatement("select * from correo_pendiente where estado='Pendiente' and programado_en<=? and caduca_en>? order by programado_en,id limit 1 for update")){
                    s.setObject(1,ahora);s.setObject(2,ahora);
                    try(ResultSet r=s.executeQuery()){if(!r.next())return true;
                        String token=UUID.randomUUID().toString();
                        encontrado[0]=new Mensaje(r.getLong("id"),r.getInt("tutoria_id"),r.getString("tipo"),r.getObject("referencia",LocalDateTime.class),r.getString("destino"),r.getString("rol"),r.getString("asunto"),r.getString("cuerpo"),r.getString("uuid"),token,r.getInt("intentos")+1,r.getObject("primer_intento",LocalDateTime.class)==null?ahora:r.getObject("primer_intento",LocalDateTime.class),r.getObject("caduca_en",LocalDateTime.class),r.getBoolean("incierto"));
                    }
                }
                Mensaje m=encontrado[0];
                try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado='Enviando',intentos=intentos+1,token=?,bloqueo_hasta=?,primer_intento=coalesce(primer_intento,?) where id=?")){
                    s.setString(1,m.token());s.setObject(2,ahora.plusMinutes(2));s.setObject(3,ahora);s.setLong(4,m.id());s.executeUpdate();
                }return true;
            });
            Mensaje m=encontrado[0];if(m==null)return null;
            if(vigente(m,ahora))return m;
            resolver(m,"Cancelado",ahora,"El aviso ya no corresponde al estado o destinatario actual",ahora);
        }return null;
    }
    private boolean vigente(Mensaje m,LocalDateTime ahora) {
        Tutoria t=new TutoriaDAO(conn).getTutoriaById(m.tutoria());if(t==null)return false;
        try{if(!destinatarios(t.getId(),Set.of(m.rol())).containsKey(m.destino()))return false;}
        catch(SQLException e){throw fallo(e);}
        LocalDateTime inicio=ReglasTutoria.inicio(t);
        return switch(m.tipo()){
            case "Reserva"->t.getEstado().equals("Pendiente de pago")&&!t.isPagoRevision();
            case "Comprobante recibido"->t.isPagoRevision();
            case "observar"->new PagosDAO(conn).obtener(t.getId())!=null&&new PagosDAO(conn).obtener(t.getId()).estado().equals("Observado");
            case "Recordatorio clase"->t.getEstado().equals("Confirmada")&&Objects.equals(inicio,m.referencia())&&inicio.isAfter(ahora)&&!t.isCancelacionSolicitada();
            case "Recordatorio pago"->t.getEstado().equals("Pendiente de pago")&&!t.isPagoRevision()&&Objects.equals(t.getVencePago(),m.referencia())&&m.referencia().isAfter(ahora)&&!t.isCancelacionSolicitada();
            case "Recordatorio revisión"->t.getEstado().equals("Pendiente de pago")&&t.isPagoRevision()&&Objects.equals(t.getRevisionPrevista(),m.referencia())&&inicio!=null&&inicio.isAfter(ahora);
            default->true;
        };
    }
    public void resolver(Mensaje m,String estado,LocalDateTime proximo,String error,LocalDateTime ahora) {
        resolver(m,estado,proximo,error,ahora,m.incierto());
    }
    public void resolver(Mensaje m,String estado,LocalDateTime proximo,String error,LocalDateTime ahora,boolean incierto) {
        try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado=?,programado_en=?,ultimo_error=?,aceptado_en=?,incierto=?,token=null,bloqueo_hasta=null where id=? and estado='Enviando' and token=?")){
            s.setString(1,estado);s.setObject(2,proximo);s.setString(3,error);s.setObject(4,estado.equals("Aceptado")?ahora:null);s.setBoolean(5,incierto);s.setLong(6,m.id());s.setString(7,m.token());s.executeUpdate();
        }catch(SQLException e){throw fallo(e);}
    }
    public int aceptadosHoy(LocalDateTime ahora) {
        try(PreparedStatement s=conn.prepareStatement("select count(*) from correo_pendiente where estado='Aceptado' and aceptado_en>=?")){
            s.setObject(1,ahora.toLocalDate().atStartOfDay());try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}
        }catch(SQLException e){throw fallo(e);}
    }
    public List<Fila> listar() {
        List<Fila> filas=new ArrayList<>();
        try(PreparedStatement s=conn.prepareStatement("select id,tutoria_id,destino,asunto,estado,intentos,programado_en,ultimo_error from correo_pendiente order by id desc limit 100");ResultSet r=s.executeQuery()){
            while(r.next())filas.add(new Fila(r.getLong(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6),r.getObject(7,LocalDateTime.class),r.getString(8)));return filas;
        }catch(SQLException e){throw fallo(e);}
    }
    public boolean gestionar(long id,String accion,boolean confirmar,LocalDateTime ahora) {
        if(!Set.of("reintentar","cancelar").contains(accion))throw new IllegalArgumentException("Acción de correo inválida.");
        return new TutoriaDAO(conn).transaction(()->{
            String estado;LocalDateTime caduca;
            try(PreparedStatement s=conn.prepareStatement("select estado,caduca_en from correo_pendiente where id=? for update")){
                s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())return false;estado=r.getString(1);caduca=r.getObject(2,LocalDateTime.class);}
            }
            if(accion.equals("reintentar")){
                if(!Set.of("Fallido","Dudoso").contains(estado)||!caduca.isAfter(ahora))throw new IllegalArgumentException("Este aviso no admite reintento.");
                if(estado.equals("Dudoso")&&!confirmar)throw new IllegalArgumentException("Comprueba el envío en Brevo y confirma el posible duplicado antes de reintentar.");
                try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado='Pendiente',intentos=0,incierto=false,primer_intento=null,uuid=?,programado_en=?,ultimo_error='' where id=?")){
                    s.setString(1,UUID.randomUUID().toString());s.setObject(2,ahora);s.setLong(3,id);s.executeUpdate();
                }
            }else{
                if(!Set.of("Pendiente","Fallido","Dudoso").contains(estado))throw new IllegalArgumentException("Este aviso ya no se puede cancelar.");
                try(PreparedStatement s=conn.prepareStatement("update correo_pendiente set estado='Cancelado',ultimo_error='Cancelado por administración' where id=?")){s.setLong(1,id);s.executeUpdate();}
            }return true;
        });
    }
    private IllegalStateException fallo(SQLException e){return new IllegalStateException("No se pudo gestionar la cola de correos.",e);}
}
