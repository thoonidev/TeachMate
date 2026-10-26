package com.tme.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import com.tme.service.ReglasTutoria;

/** Courses reuse the existing catalog so its identifiers are preserved. */
public final class OfertaDAO {
    public record Curso(int id, String nombre, String descripcion, boolean activo) { }
    public record Tarifa(int id, int cursoId, int minutos, BigDecimal precio, BigDecimal porcentaje, boolean activa) { }
    public record Franja(int id, int docenteId, LocalDate fecha, LocalTime desde, LocalTime hasta) { }
    public record Horario(LocalDate fecha, LocalTime inicio) { }
    private final Connection conn;
    private final Clock clock;
    public OfertaDAO(Connection conn) { this(conn,Clock.system(ReglasTutoria.ZONA)); }
    public OfertaDAO(Connection conn,Clock clock) { this.conn = conn;this.clock=clock; }
    private LocalDateTime ahora() { return LocalDateTime.ofInstant(clock.instant(),ReglasTutoria.ZONA); }

    public List<Curso> cursos(boolean soloActivos) {
        List<Curso> rows = new ArrayList<>();
        try (PreparedStatement s = conn.prepareStatement("select * from especialidad" + (soloActivos ? " where activo=true" : "") + " order by especialidad_nombre" ); ResultSet r = s.executeQuery()) {
            while (r.next()) rows.add(new Curso(r.getInt("id"), r.getString("especialidad_nombre"), r.getString("descripcion"), r.getBoolean("activo")));
            return rows;
        } catch (SQLException e) { throw failure(e); }
    }
    public void guardarCurso(int id, String nombre, String descripcion, boolean activo) {
        nombre = texto(nombre, 45, "Nombre del curso"); descripcion = opcional(descripcion, 500);
        final String n = nombre, d = descripcion;
        transaction(() -> {
            try (PreparedStatement s = conn.prepareStatement(id == 0
                    ? "insert into especialidad(especialidad_nombre,descripcion,activo) values(?,?,?)"
                    : "update especialidad set especialidad_nombre=?,descripcion=?,activo=? where id=?")) {
                s.setString(1, n); s.setString(2, d); s.setBoolean(3, activo); if (id > 0) s.setInt(4, id);
                if (s.executeUpdate() != 1) throw new IllegalArgumentException("El curso ya no existe.");
            }
        });
    }
    public Set<Integer> cursosDocente(int docenteId) {
        Set<Integer> ids = new HashSet<>();
        try (PreparedStatement s = conn.prepareStatement("select curso_id from docente_curso where docente_id=?")) {
            s.setInt(1, docenteId); try (ResultSet r = s.executeQuery()) { while (r.next()) ids.add(r.getInt(1)); }
            return ids;
        } catch (SQLException e) { throw failure(e); }
    }
    public void asignar(int docenteId, Set<Integer> cursos) {
        transaction(() -> {
            lockDocente(docenteId);
            List<Integer> ordered = cursos.stream().sorted().toList();
            for (int id : ordered) lockCurso(id, true);
            try (PreparedStatement s = conn.prepareStatement("delete from docente_curso where docente_id=? and curso_id in (select id from especialidad where activo=true)")) { s.setInt(1, docenteId); s.executeUpdate(); }
            try (PreparedStatement s = conn.prepareStatement("insert into docente_curso(docente_id,curso_id) values(?,?)")) {
                for (int id : ordered) { s.setInt(1, docenteId); s.setInt(2, id); s.addBatch(); } s.executeBatch();
            }
        });
    }
    public List<Tarifa> tarifas(int cursoId, boolean soloActivas) {
        List<Tarifa> rows = new ArrayList<>();
        try (PreparedStatement s = conn.prepareStatement("select * from tarifa_curso where curso_id=?" + (soloActivas ? " and activa=true" : "") + " order by duracion_minutos,id desc")) {
            s.setInt(1, cursoId); try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(tarifa(r)); } return rows;
        } catch (SQLException e) { throw failure(e); }
    }
    public void guardarTarifa(int cursoId, int minutos, String precio, String porcentaje) {
        if (minutos < 15 || minutos > 180) throw new IllegalArgumentException("La duración debe ser de 15 a 180 minutos.");
        BigDecimal importe = decimal(precio, false), parte = decimal(porcentaje, true);
        transaction(() -> {
            lockCurso(cursoId, true);
            try (PreparedStatement s = conn.prepareStatement("update tarifa_curso set activa=false where curso_id=? and duracion_minutos=? and activa=true")) { s.setInt(1, cursoId); s.setInt(2, minutos); s.executeUpdate(); }
            try (PreparedStatement s = conn.prepareStatement("insert into tarifa_curso(curso_id,duracion_minutos,precio,porcentaje_docente) values(?,?,?,?)")) {
                s.setInt(1, cursoId); s.setInt(2, minutos); s.setBigDecimal(3, importe); s.setBigDecimal(4, parte); s.executeUpdate();
            }
        });
    }
    public void retirarTarifa(int cursoId, int tarifaId) {
        transaction(() -> {
            lockCurso(cursoId, false);
            try (PreparedStatement s = conn.prepareStatement("update tarifa_curso set activa=false where id=? and curso_id=? and activa=true")) {
                s.setInt(1, tarifaId); s.setInt(2, cursoId); if (s.executeUpdate() != 1) throw new IllegalArgumentException("La tarifa ya no está activa.");
            }
        });
    }
    public List<Franja> franjas(int docenteId) {
        List<Franja> rows = new ArrayList<>();
        try (PreparedStatement s = conn.prepareStatement("select * from disponibilidad where docente_id=? and activa=true and fecha>=? order by fecha,hora_desde")) {
            s.setInt(1, docenteId); s.setDate(2, java.sql.Date.valueOf(ahora().toLocalDate()));
            try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(new Franja(r.getInt("id"), docenteId, r.getDate("fecha").toLocalDate(), r.getTime("hora_desde").toLocalTime(), r.getTime("hora_hasta").toLocalTime())); }
            return rows;
        } catch (SQLException e) { throw failure(e); }
    }
    public void agregarFranja(int docenteId, String fecha, String desde, String hasta, String actor) {
        LocalDate day; LocalTime a, b;
        try { day = LocalDate.parse(fecha); a = LocalTime.parse(desde); b = LocalTime.parse(hasta); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Indica una fecha y horas válidas."); }
        if (!day.atTime(a).isAfter(ahora()) || !a.isBefore(b) || Duration.between(a,b).toMinutes() < 15
                || a.getSecond()!=0 || b.getSecond()!=0 || a.getNano()!=0 || b.getNano()!=0) throw new IllegalArgumentException("La franja debe ser futura, durar al menos 15 minutos y terminar el mismo día.");
        transaction(() -> {
            lockDocente(docenteId);
            try (PreparedStatement s = conn.prepareStatement("select id from disponibilidad where docente_id=? and fecha=? and activa=true and hora_desde<? and hora_hasta>?")) {
                s.setInt(1, docenteId); s.setDate(2, java.sql.Date.valueOf(day)); s.setTime(3, Time.valueOf(b)); s.setTime(4, Time.valueOf(a));
                try (ResultSet r=s.executeQuery()) { if(r.next()) throw new IllegalArgumentException("La franja se cruza con otra disponibilidad. Los horarios contiguos sí se permiten."); }
            }
            try (PreparedStatement s=conn.prepareStatement("insert into disponibilidad(docente_id,fecha,hora_desde,hora_hasta,creado_por) values(?,?,?,?,?)")) {
                s.setInt(1,docenteId); s.setDate(2,java.sql.Date.valueOf(day)); s.setTime(3,Time.valueOf(a)); s.setTime(4,Time.valueOf(b)); s.setString(5,actor); s.executeUpdate();
            }
        });
    }
    public List<Horario> horarios(int docenteId, int minutos, int alumnoId) {
        if(minutos<15||minutos>180)throw new IllegalArgumentException("La duración debe ser de 15 a 180 minutos.");
        List<LocalDateTime[]> ocupados=new ArrayList<>();
        for(var reserva:new TutoriaDAO(conn,clock).reservasActivas(docenteId,alumnoId)) {
            LocalDateTime a=ReglasTutoria.inicio(reserva);if(a!=null)ocupados.add(new LocalDateTime[]{a,a.plusMinutes(reserva.getDuracionMinutos())});
        }
        List<Horario> result=new ArrayList<>();LocalDateTime ahora=ahora();
        var plazos=new ConfiguracionReservasDAO(conn).obtener();if(!plazos.completa()||!new ConfiguracionPagosDAO(conn).obtener().completa())return result;
        LocalDateTime minimo=plazos.minimo(ahora);
        for(Franja f:franjas(docenteId)) {
            for(LocalDateTime a=f.fecha().atTime(f.desde());!a.plusMinutes(minutos).isAfter(f.fecha().atTime(f.hasta()));a=a.plusMinutes(minutos)) {
                if(a.isBefore(minimo))continue;LocalDateTime b=a.plusMinutes(minutos);boolean libre=true;
                for(var rango:ocupados)if(a.isBefore(rango[1])&&rango[0].isBefore(b)){libre=false;break;}
                if(libre)result.add(new Horario(a.toLocalDate(),a.toLocalTime()));
            }
        }
        return result;
    }
    public void retirarFranja(int docenteId, int id, String actor) {
        transaction(() -> {
            lockDocente(docenteId);
            try (PreparedStatement s = conn.prepareStatement("update disponibilidad set activa=false,retirado_por=?,retirada_en=CURRENT_TIMESTAMP where id=? and docente_id=? and activa=true")) {
                s.setString(1, actor); s.setInt(2,id); s.setInt(3,docenteId); if(s.executeUpdate()!=1) throw new IllegalArgumentException("La franja ya no está disponible.");
            }
        });
    }
    /** Caller holds the teacher agenda lock inside the reservation transaction. */
    public Tarifa validarReserva(int docenteId, int cursoId, int tarifaId, LocalDate fecha, LocalTime inicio, int minutos) throws SQLException {
        // Lock order: student, teacher, course. Tariff changes use the same course lock.
        lockCurso(cursoId, true);
        try (PreparedStatement s=conn.prepareStatement("select curso_id from docente_curso where docente_id=? and curso_id=?")) {
            s.setInt(1,docenteId); s.setInt(2,cursoId); try(ResultSet r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("El profesor no está asignado a ese curso."); }
        }
        Tarifa tarifa;
        try (PreparedStatement s=conn.prepareStatement("select * from tarifa_curso where id=? and curso_id=? and activa=true for update")) {
            s.setInt(1,tarifaId); s.setInt(2,cursoId); try(ResultSet r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("La tarifa cambió. Revisa el curso y vuelve a solicitar."); tarifa=tarifa(r); }
        }
        if(tarifa.minutos()!=minutos) throw new IllegalArgumentException("La duración no corresponde a la tarifa seleccionada.");
        validarHorarioAsignado(docenteId,cursoId,fecha,inicio,minutos);
        return tarifa;
    }
    public void validarHorarioAsignado(int docenteId,Integer cursoId,LocalDate fecha,LocalTime inicio,int minutos)throws SQLException {
        if(cursoId!=null) {
            lockCurso(cursoId,true);
            try(PreparedStatement s=conn.prepareStatement("select curso_id from docente_curso where docente_id=? and curso_id=?")){s.setInt(1,docenteId);s.setInt(2,cursoId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("El profesor ya no está asignado a ese curso.");}}
        }
        if(!fecha.equals(fecha.atTime(inicio).plusMinutes(minutos).toLocalDate())) throw new IllegalArgumentException("La sesión debe terminar el mismo día.");
        try (PreparedStatement s=conn.prepareStatement("select id from disponibilidad where docente_id=? and fecha=? and activa=true and hora_desde<=? and hora_hasta>=?")) {
            s.setInt(1,docenteId); s.setDate(2,java.sql.Date.valueOf(fecha)); s.setTime(3,Time.valueOf(inicio)); s.setTime(4,Time.valueOf(inicio.plusMinutes(minutos)));
            try(ResultSet r=s.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("El horario está fuera de la disponibilidad del profesor."); }
        }
    }
    private Tarifa tarifa(ResultSet r)throws SQLException { return new Tarifa(r.getInt("id"),r.getInt("curso_id"),r.getInt("duracion_minutos"),r.getBigDecimal("precio"),r.getBigDecimal("porcentaje_docente"),r.getBoolean("activa")); }
    private void lockDocente(int id)throws SQLException {
        try(PreparedStatement s=conn.prepareStatement("select activo from docente where id=? for update")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new IllegalArgumentException("El profesor no está activo.");}}
    }
    private void lockCurso(int id,boolean activo)throws SQLException {
        try(PreparedStatement s=conn.prepareStatement("select activo from especialidad where id=? for update")){s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next()||(activo&&!r.getBoolean(1)))throw new IllegalArgumentException("El curso no está activo.");}}
    }
    private interface Action { void run()throws SQLException; }
    private void transaction(Action action) {
        try {
            if(!conn.getAutoCommit()) throw new IllegalStateException("La operación requiere su propia transacción.");
            conn.setAutoCommit(false);
            try { action.run();conn.commit(); } catch(SQLException|RuntimeException e){conn.rollback();throw e;} finally{conn.setAutoCommit(true);}
        }catch(SQLException e){throw failure(e);}
    }
    public static String texto(String s,int max,String nombre){s=opcional(s,max);if(s.isBlank())throw new IllegalArgumentException(nombre+" es obligatorio.");return s;}
    public static String opcional(String s,int max){s=s==null?"":s.strip();if(s.length()>max||s.indexOf('\0')>=0)throw new IllegalArgumentException("El texto supera la longitud permitida.");return s;}
    private static BigDecimal decimal(String s,boolean porcentaje){
        try {if(s==null||!s.matches("[0-9]{1,7}(\\.[0-9]{1,2})?"))throw new NumberFormatException();BigDecimal n=new BigDecimal(s);if(n.signum()<0||(!porcentaje&&n.signum()==0)||n.compareTo(new BigDecimal(porcentaje?"100":"9999999.99"))>0)throw new NumberFormatException();return n.setScale(2);}
        catch(NumberFormatException e){throw new IllegalArgumentException("Indica un precio positivo y un porcentaje entre 0 y 100, con hasta dos decimales.");}
    }
    private RuntimeException failure(SQLException e){return "23000".equals(e.getSQLState())||"23505".equals(e.getSQLState())?new IllegalArgumentException("El registro ya existe o tiene relaciones que deben conservarse."):new IllegalStateException("No se pudo guardar o consultar la oferta académica.");}
}
