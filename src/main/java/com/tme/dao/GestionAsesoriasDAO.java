package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.ReglasTutoria;
import com.tme.service.ReglasTutoria.Rol;
import java.net.URI;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Every change locks student, teacher, then session; history is committed with the change. */
public final class GestionAsesoriasDAO {
    public record Evento(String rol, String accion, String detalle, LocalDateTime fecha) { }
    public record Cambio(int version, LocalDate fecha, LocalTime hora, String motivo, String estado,
                         boolean alumno, boolean docente, LocalDateTime vence) { }
    private final Connection conn;
    private final TutoriaDAO base;
    public GestionAsesoriasDAO(Connection conn) { this(conn,Clock.system(ReglasTutoria.ZONA)); }
    public GestionAsesoriasDAO(Connection conn,Clock clock) { this.conn=conn;this.base=new TutoriaDAO(conn,clock); }

    public Tutoria obtener(int id,int actor,Rol rol) {
        Tutoria t=base.getTutoriaById(id);
        if(t==null||!base.esPropietario(t,actor,rol))return null;
        expirarUna(t);
        Cambio cambio=cambio(id);
        if(cambio!=null&&cambio.estado().equals("Vencida"))base.transaction(()->{
            bloquear(t);
            try(PreparedStatement s=conn.prepareStatement("update tutoria_cambio set estado='Vencida' where tutoria_id=? and estado='Pendiente' and vence_en<=?")) {
                s.setInt(1,id);s.setObject(2,base.ahora());
                if(s.executeUpdate()==1)evento(conn,id,"SISTEMA",null,"Propuesta vencida","La propuesta venció sin completar ambas aceptaciones. Se conserva el horario original.",base.ahora());
            }
            return true;
        });
        return base.getTutoriaById(id);
    }
    public void expirar() {
        List<Integer> ids=new ArrayList<>();
        try(PreparedStatement s=conn.prepareStatement("select id from tutoria where (estado='Pendiente' and vence_respuesta<=?) or (estado='Pendiente de pago' and pago_revision=false and vence_pago<=?) or (estado in ('Pendiente','Pendiente de pago') and (ftutoria<? or (ftutoria=? and hora_inicio<=?))) order by id limit 1000")) {
            LocalDateTime now=base.ahora();s.setObject(1,now);s.setObject(2,now);s.setString(3,now.toLocalDate().toString());s.setString(4,now.toLocalDate().toString());s.setTime(5,Time.valueOf(now.toLocalTime()));
            try(ResultSet r=s.executeQuery()){while(r.next())ids.add(r.getInt(1));}
        }catch(SQLException e){throw failure(e);}
        for(int id:ids){Tutoria t=base.getTutoriaById(id);if(t!=null)expirarUna(t);}
    }
    private void expirarUna(Tutoria initial) {
        if(!pendiente(initial)||ReglasTutoria.vigente(initial,base.ahora()))return;
        base.transaction(()->{
            Tutoria t=bloquear(initial);
            if(pendiente(t)&&!ReglasTutoria.vigente(t,base.ahora())) {
                boolean revision=t.isPagoRevision()&&t.getEstado().equals("Pendiente de pago");
                estado(t,revision?"Incidencia":"Vencida");cerrarCambio(t.getId(),"Anulada");
                evento(conn,t.getId(),"SISTEMA",null,"Vencimiento",revision?"La clase comenzó con un comprobante sin validar. Administración debe resolver el pago y la sesión.":"Terminó el plazo de respuesta o pago, o comenzó el horario sin confirmar.",base.ahora());
            }
            return true;
        });
    }
    private boolean pendiente(Tutoria t){return t.getEstado().equals("Pendiente")||t.getEstado().equals("Pendiente de pago");}
    private Tutoria bloquear(Tutoria t)throws SQLException {
        if(!base.lockAlumno(t.getUserId()))throw new IllegalArgumentException("El alumno ya no existe.");
        base.lockDocente(t.getDocenteId());
        try(PreparedStatement s=conn.prepareStatement("select * from tutoria where id=? for update")){s.setInt(1,t.getId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("La asesoría ya no existe.");return base.read(r);}}
    }

    public boolean gestionar(int id,int actor,Rol rol,String accion,String motivo,String valor,String fecha,String hora,int version) {
        Tutoria initial=obtener(id,actor,rol);if(initial==null)return false;
        return base.transaction(()->{
            Tutoria t=bloquear(initial);LocalDateTime now=base.ahora();
            if(!base.esPropietario(t,actor,rol))return false;
            if(!acciones(t,rol,now).contains(accion))throw new IllegalArgumentException("La acción ya no está disponible. Actualiza la asesoría.");
            String detalle;
            switch(accion) {
                case "cancelar" -> {
                    detalle=motivo(motivo);
                    if(rol==Rol.ALUMNO&&(ReglasTutoria.confirmada(t)||t.isPagoRevision())) {
                        try(PreparedStatement s=conn.prepareStatement("update tutoria set cancelacion_solicitada=true,motivo_cancelacion=? where id=?")){s.setString(1,detalle);s.setInt(2,id);s.executeUpdate();}
                        cerrarCambio(id,"Anulada");detalle="Solicitud de cancelación para revisión administrativa: "+detalle;
                    } else {estado(t,"Cancelada");cerrarCambio(id,"Anulada");}
                }
                case "mantener" -> {
                    detalle=motivo(motivo);
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set cancelacion_solicitada=false,motivo_cancelacion='' where id=?")){s.setInt(1,id);s.executeUpdate();}
                    detalle="Cancelación no aprobada; la sesión se mantiene. "+detalle;
                }
                case "enlace" -> {
                    String enlace=enlace(valor);
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set enlace_clase=? where id=?")){s.setString(1,enlace);s.setInt(2,id);s.executeUpdate();}
                    detalle="Enlace privado de la clase actualizado.";
                }
                case "completar" -> {
                    detalle=OfertaDAO.texto(motivo,500,"Resumen de la asesoría");
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set estado='Completada',asistencia='Realizada',comentario=?,cancelacion_solicitada=false where id=?")){s.setString(1,detalle);s.setInt(2,id);s.executeUpdate();}
                    cerrarCambio(id,"Anulada");
                }
                case "comentario" -> {
                    detalle=OfertaDAO.texto(motivo,500,"Seguimiento");
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set comentario=? where id=?")){s.setString(1,detalle);s.setInt(2,id);s.executeUpdate();}
                    detalle="Seguimiento actualizado: "+detalle;
                }
                case "incidencia" -> {
                    detalle=motivo(motivo);
                    Set<String> resultados=rol==Rol.ALUMNO?Set.of("Profesor ausente","Problema técnico"):Set.of("Alumno ausente","Profesor ausente","Problema técnico");
                    if(!resultados.contains(valor))throw new IllegalArgumentException("Selecciona un resultado válido.");
                    try(PreparedStatement s=conn.prepareStatement("update tutoria set estado='Incidencia',asistencia=? where id=?")){s.setString(1,valor);s.setInt(2,id);s.executeUpdate();}
                    cerrarCambio(id,"Anulada");detalle=valor+": "+detalle;
                }
                case "resolver" -> {
                    detalle=motivo(motivo);
                    if(t.isPagoRevision())throw new IllegalArgumentException("Primero resuelve la revisión del pago desde el apartado de pagos.");
                    if(!Set.of("Cancelada","Completada").contains(valor))throw new IllegalArgumentException("Selecciona cómo resolver la incidencia.");
                    LocalDateTime a=ReglasTutoria.inicio(t);
                    if(valor.equals("Completada")&&(a==null||now.isBefore(a.plusMinutes(t.getDuracionMinutos()))))throw new IllegalArgumentException("La sesión todavía no terminó.");
                    estado(t,valor);
                    if(valor.equals("Completada"))try(PreparedStatement s=conn.prepareStatement("update tutoria set asistencia='Realizada' where id=?")){s.setInt(1,id);s.executeUpdate();}
                    detalle="Incidencia resuelta como "+valor+": "+detalle;
                }
                case "proponer" -> {
                    detalle=motivo(motivo);Cambio previo=cambio(id);
                    if(previo!=null&&previo.estado().equals("Pendiente"))throw new IllegalArgumentException("Responde o retira la propuesta actual antes de crear otra.");
                    LocalDateTime nuevo=horario(fecha,hora);validarNuevo(t,nuevo,now);
                    int nuevaVersion=previo==null?1:previo.version()+1;
                    LocalDateTime limite=ReglasTutoria.inicio(t).isBefore(nuevo)?ReglasTutoria.inicio(t):nuevo;
                    if(previo==null) {
                        try(PreparedStatement s=conn.prepareStatement("insert into tutoria_cambio(tutoria_id,version,fecha,hora,motivo,estado,acepta_alumno,acepta_docente,vence_en) values(?,?,?,?,?,'Pendiente',?,?,?)")) {
                            s.setInt(1,id);s.setInt(2,nuevaVersion);s.setDate(3,java.sql.Date.valueOf(nuevo.toLocalDate()));s.setTime(4,Time.valueOf(nuevo.toLocalTime()));s.setString(5,detalle);s.setBoolean(6,rol==Rol.ALUMNO);s.setBoolean(7,rol==Rol.DOCENTE);s.setObject(8,limite);s.executeUpdate();
                        }
                    }else try(PreparedStatement s=conn.prepareStatement("update tutoria_cambio set version=?,fecha=?,hora=?,motivo=?,estado='Pendiente',acepta_alumno=?,acepta_docente=?,vence_en=? where tutoria_id=?")) {
                        s.setInt(1,nuevaVersion);s.setDate(2,java.sql.Date.valueOf(nuevo.toLocalDate()));s.setTime(3,Time.valueOf(nuevo.toLocalTime()));s.setString(4,detalle);s.setBoolean(5,rol==Rol.ALUMNO);s.setBoolean(6,rol==Rol.DOCENTE);s.setObject(7,limite);s.setInt(8,id);s.executeUpdate();
                    }
                    detalle="Propuesta #"+nuevaVersion+" para "+nuevo+" (Lima). El horario original se conserva. "+detalle;
                }
                case "aceptar_cambio", "rechazar_cambio" -> {
                    Cambio c=cambio(id);
                    if(c==null||!c.estado().equals("Pendiente")||c.version()!=version||!now.isBefore(c.vence()))throw new IllegalArgumentException("La propuesta venció o ya fue respondida.");
                    if(accion.equals("rechazar_cambio")) {detalle=motivo(motivo);cerrarCambio(id,"Rechazada");}
                    else {
                        if(rol==Rol.ADMIN)throw new IllegalArgumentException("El alumno y el profesor deben aceptar por sí mismos.");
                        if((rol==Rol.ALUMNO&&c.alumno())||(rol==Rol.DOCENTE&&c.docente()))throw new IllegalArgumentException("Ya aceptaste esta propuesta.");
                        boolean alumno=c.alumno()||rol==Rol.ALUMNO,docente=c.docente()||rol==Rol.DOCENTE;
                        if(alumno&&docente) {
                            LocalDateTime nuevo=c.fecha().atTime(c.hora());validarNuevo(t,nuevo,now);
                            String anterior=t.getFtutoria()+" "+t.getHorario();
                            try(PreparedStatement s=conn.prepareStatement("update tutoria set ftutoria=?,hora_inicio=?,enlace_clase='' where id=?")){s.setString(1,c.fecha().toString());s.setTime(2,Time.valueOf(c.hora()));s.setInt(3,id);s.executeUpdate();}
                            cerrarCambio(id,"Aceptada");detalle="Cambio #"+version+" confirmado por ambos. De "+anterior+" a "+nuevo+" (Lima). Precio y reparto conservados. Se debe revisar el enlace de clase.";
                        }else detalle="Aceptación de la propuesta #"+version+"; falta la otra confirmación.";
                        try(PreparedStatement s=conn.prepareStatement("update tutoria_cambio set acepta_alumno=?,acepta_docente=? where tutoria_id=?")){s.setBoolean(1,alumno);s.setBoolean(2,docente);s.setInt(3,id);s.executeUpdate();}
                    }
                }
                default -> throw new IllegalArgumentException("Acción inválida.");
            }
            evento(conn,id,rol.name(),rol==Rol.ADMIN?null:actor,accion,detalle,now);return true;
        });
    }

    public static Set<String> acciones(Tutoria t,Rol rol,LocalDateTime now) {
        Set<String> result=new LinkedHashSet<>();LocalDateTime inicio=ReglasTutoria.inicio(t);
        boolean futura=inicio!=null&&inicio.isAfter(now),confirmada=ReglasTutoria.confirmada(t);
        boolean sinResolver=Set.of("Pendiente","Pendiente de pago","Confirmada","Aceptada","Incidencia").contains(t.getEstado());
        if(rol==Rol.ADMIN&&sinResolver)result.add("cancelar");
        if(rol==Rol.ADMIN&&t.isCancelacionSolicitada())result.add("mantener");
        if(rol==Rol.ADMIN&&t.getEstado().equals("Incidencia"))result.add("resolver");
        if(rol==Rol.DOCENTE&&Set.of("Confirmada","Aceptada","Completada","Incidencia").contains(t.getEstado()))result.add("comentario");
        if(rol==Rol.ALUMNO&&Set.of("Pendiente","Pendiente de pago").contains(t.getEstado())&&ReglasTutoria.vigente(t,now))result.add("cancelar");
        if(confirmada) {
            if(futura&&!t.isCancelacionSolicitada()) {if(rol!=Rol.ADMIN)result.add("cancelar");result.add("proponer");result.add("rechazar_cambio");if(rol!=Rol.ADMIN)result.add("aceptar_cambio");}
            if((rol==Rol.DOCENTE||rol==Rol.ADMIN)&&inicio!=null&&now.isBefore(inicio.plusMinutes(t.getDuracionMinutos())))result.add("enlace");
            if(inicio!=null&&!now.isBefore(inicio.plusMinutes(t.getDuracionMinutos()))&&!t.isCancelacionSolicitada()&&(rol==Rol.DOCENTE||rol==Rol.ADMIN))result.add("completar");
            if(inicio!=null&&!now.isBefore(inicio)&&rol!=Rol.ADMIN)result.add("incidencia");
        }
        return result;
    }
    private void validarNuevo(Tutoria t,LocalDateTime nuevo,LocalDateTime now)throws SQLException {
        exigirProfesorActivo(t);
        LocalDateTime anterior=ReglasTutoria.inicio(t);
        if(anterior==null||!anterior.isAfter(now))throw new IllegalArgumentException("La sesión original ya comenzó o no tiene horario.");
        if(nuevo.equals(anterior))throw new IllegalArgumentException("El nuevo horario debe ser diferente.");
        Tutoria prueba=new Tutoria();prueba.setId(t.getId());prueba.setUserId(t.getUserId());prueba.setDocenteId(t.getDocenteId());prueba.setFtutoria(nuevo.toLocalDate().toString());prueba.setHoraInicio(nuevo.toLocalTime());prueba.setDuracionMinutos(t.getDuracionMinutos());
        ReglasTutoria.validarHorario(prueba,now);
        new OfertaDAO(conn).validarHorarioAsignado(t.getDocenteId(),t.getCursoId(),nuevo.toLocalDate(),nuevo.toLocalTime(),t.getDuracionMinutos());
        base.validarDisponibilidad(prueba);
    }
    private LocalDateTime horario(String fecha,String hora){try{return LocalDate.parse(fecha).atTime(LocalTime.parse(hora));}catch(RuntimeException e){throw new IllegalArgumentException("Indica una fecha y hora válidas.");}}
    private void exigirProfesorActivo(Tutoria t){if(!new DocenteDAO(conn).isActiveDocente(t.getDocenteId()))throw new IllegalArgumentException("El profesor está inactivo. Contacta al administrador.");}
    private String motivo(String texto){return OfertaDAO.texto(texto,850,"Motivo");}
    private String enlace(String valor){
        String text=OfertaDAO.texto(valor,1000,"Enlace");
        try{URI u=URI.create(text);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.toASCIIString().length()>1000)throw new IllegalArgumentException();return u.toASCIIString();}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("Usa un enlace HTTPS válido de la plataforma de videollamada.");}
    }
    private void estado(Tutoria t,String estado)throws SQLException {try(PreparedStatement s=conn.prepareStatement("update tutoria set estado=?,cancelacion_solicitada=false where id=?")){s.setString(1,estado);s.setInt(2,t.getId());s.executeUpdate();}}
    private void cerrarCambio(int id,String estado)throws SQLException {try(PreparedStatement s=conn.prepareStatement("update tutoria_cambio set estado=? where tutoria_id=? and estado='Pendiente'")){s.setString(1,estado);s.setInt(2,id);s.executeUpdate();}}
    public Cambio cambio(int id) {
        try(PreparedStatement s=conn.prepareStatement("select * from tutoria_cambio where tutoria_id=?")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())return null;String estado=r.getString("estado");LocalDateTime vence=r.getObject("vence_en",LocalDateTime.class);if(estado.equals("Pendiente")&&!base.ahora().isBefore(vence))estado="Vencida";return new Cambio(r.getInt("version"),r.getDate("fecha").toLocalDate(),r.getTime("hora").toLocalTime(),r.getString("motivo"),estado,r.getBoolean("acepta_alumno"),r.getBoolean("acepta_docente"),vence);}}
        catch(SQLException e){throw failure(e);}
    }
    public List<Evento> historial(int id) {
        List<Evento> rows=new ArrayList<>();
        try(PreparedStatement s=conn.prepareStatement("select actor_rol,accion,detalle,ocurrido_en from tutoria_evento where tutoria_id=? order by id desc")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Evento(r.getString(1),r.getString(2),r.getString(3),r.getObject(4,LocalDateTime.class)));}return rows;}
        catch(SQLException e){throw failure(e);}
    }
    static void evento(Connection conn,Integer id,String rol,Integer actor,String accion,String detalle,LocalDateTime fecha)throws SQLException {
        try(PreparedStatement s=conn.prepareStatement("insert into tutoria_evento(tutoria_id,actor_rol,actor_id,accion,detalle,ocurrido_en) values(?,?,?,?,?,?)")) {
            if(id==null)s.setNull(1,Types.INTEGER);else s.setInt(1,id);s.setString(2,rol);if(actor==null)s.setNull(3,Types.INTEGER);else s.setInt(3,actor);s.setString(4,accion);s.setString(5,detalle);s.setObject(6,fecha);s.executeUpdate();
        }
    }
    private IllegalStateException failure(SQLException e){return new IllegalStateException("No se pudo gestionar la asesoría.",e);}
}
