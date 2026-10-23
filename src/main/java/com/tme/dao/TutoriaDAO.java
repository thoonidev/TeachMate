package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.modal.EstadoTutoria;
import com.tme.service.ReglasTutoria;
import com.tme.service.ReglasTutoria.Rol;
import com.tme.service.FiltroTutorias;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TutoriaDAO {
    private final Connection conn;
    private final Clock clock;
    public TutoriaDAO(Connection conn) { this(conn, Clock.system(ReglasTutoria.ZONA)); }
    public TutoriaDAO(Connection conn, Clock clock) { this.conn = conn; this.clock = clock; }
    private LocalDateTime ahora() { return LocalDateTime.ofInstant(clock.instant(), ReglasTutoria.ZONA); }

    public boolean addTutoria(Tutoria tutoria) {
        return transaction(() -> {
            // Lock both agendas before checking availability, including when no appointments exist yet.
            if (!lockAlumno(tutoria.getUserId()) || !lockDocente(tutoria.getDocenteId())) return false;
            ReglasTutoria.validarHorario(tutoria, ahora());
            validarDisponibilidad(tutoria);
            String sql = "insert into tutoria(userId,nombre,genero,edad,ftutoria,email,telefono,experiencia,docenteId,direccion,estado,hora_inicio,duracion_minutos) "
                    + "select ?,?,?,?,?,?,?,?,?,?,?,?,? from docente where id=? and activo=true";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setInt(1, tutoria.getUserId());
                statement.setString(2, tutoria.getNombre());
                statement.setString(3, tutoria.getGenero());
                statement.setString(4, tutoria.getEdad());
                statement.setString(5, tutoria.getFtutoria());
                statement.setString(6, tutoria.getEmail());
                statement.setString(7, tutoria.getTelefono());
                statement.setString(8, tutoria.getExperiencia());
                statement.setInt(9, tutoria.getDocenteId());
                statement.setString(10, tutoria.getDireccion());
                statement.setString(11, "Pendiente");
                statement.setTime(12, Time.valueOf(tutoria.getHoraInicio()));
                statement.setInt(13, tutoria.getDuracionMinutos());
                statement.setInt(14, tutoria.getDocenteId());
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean cambiarEstado(int id, int actorId, Rol rol, EstadoTutoria siguiente) {
        Tutoria initial = getTutoriaById(id);
        if (initial == null || !esPropietario(initial, actorId, rol)) return false;
        return transaction(() -> {
            if (!lockAlumno(initial.getUserId())) return false;
            boolean docenteActivo = lockDocente(initial.getDocenteId());
            List<Tutoria> found = list("select * from tutoria where id=? for update", id);
            if (found.isEmpty() || !esPropietario(found.get(0), actorId, rol)) return false;
            Tutoria actual = found.get(0);
            ReglasTutoria.validarCambio(actual, siguiente, rol, ahora());
            if (siguiente == EstadoTutoria.ACEPTADA) {
                if (!docenteActivo) throw new IllegalArgumentException("El docente ya no está disponible.");
                validarDisponibilidad(actual);
            }
            try (PreparedStatement statement = conn.prepareStatement("update tutoria set estado=? where id=? and estado=?")) {
                statement.setString(1, siguiente.getValor());
                statement.setInt(2, id);
                statement.setString(3, actual.getEstado());
                return statement.executeUpdate() == 1;
            }
        });
    }

    private boolean esPropietario(Tutoria tutoria, int actorId, Rol rol) {
        return rol == Rol.ADMIN || (rol == Rol.ALUMNO && tutoria.getUserId() == actorId)
                || (rol == Rol.DOCENTE && tutoria.getDocenteId() == actorId);
    }

    private boolean lockAlumno(int id) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("select id from user_details where id=? for update")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private boolean lockDocente(int id) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("select activo from docente where id=? for update")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next() && result.getBoolean(1); }
        }
    }

    private void validarDisponibilidad(Tutoria tutoria) throws SQLException {
        // Agenda locks serialize competing requests; this first consistent read sees their committed work.
        // Avoid range locks here so independent agendas can still reserve concurrently on MySQL.
        String sql = "select * from tutoria where (userId=? or docenteId=?) and estado in ('Pendiente','Aceptada') and id<>?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, tutoria.getUserId());
            statement.setInt(2, tutoria.getDocenteId());
            statement.setInt(3, tutoria.getId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Tutoria otra = read(result);
                    if (ReglasTutoria.seCruzan(tutoria, otra)) {
                        throw new IllegalArgumentException(otra.getUserId() == tutoria.getUserId()
                                ? "Ya tienes una tutoría pendiente o aceptada en ese horario. Elige otro horario."
                                : "El docente tiene una tutoría pendiente o aceptada en ese horario. Elige otro horario o docente.");
                    }
                }
            }
        }
    }

    @FunctionalInterface private interface Operation { boolean run() throws SQLException; }
    private boolean transaction(Operation operation) {
        try {
            if (!conn.getAutoCommit()) throw new IllegalStateException("La operación requiere su propia transacción.");
            conn.setAutoCommit(false);
            try {
                boolean result = operation.run();
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                conn.rollback();
                throw exception;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException exception) { throw failure(); }
    }

    public List<Tutoria> getAllTutoriaByLoginUser(int userId) {
        return list("select * from tutoria where userId=? order by id desc", userId);
    }
    public List<Tutoria> getAllTutoriaByLoginDocente(int docenteId) {
        return list("select * from tutoria where docenteId=? order by id desc", docenteId);
    }
    public List<Tutoria> getAllTutoria() {
        return list("select * from tutoria order by id desc", null);
    }
    public Tutoria getTutoriaById(int id) {
        List<Tutoria> found = list("select * from tutoria where id=?", id);
        return found.isEmpty() ? null : found.get(0);
    }

    public record Pagina(List<Tutoria> tutorias, int total, int pagina, int paginas) { }
    public record Resumen(int pendientes, int aceptadas, int completadas, int cerradas, int total) { }

    private String alcance(Rol rol, int actorId) {
        if (rol == Rol.ADMIN) return "1=1";
        if (rol == null || actorId < 1) throw new IllegalArgumentException("Cuenta inválida.");
        return rol == Rol.ALUMNO ? "t.userId=?" : "t.docenteId=?";
    }
    private int identidad(PreparedStatement statement, Rol rol, int actorId) throws SQLException {
        if (rol != Rol.ADMIN) { statement.setInt(1, actorId); return 2; }
        return 1;
    }

    public Pagina buscar(Rol rol, int actorId, FiltroTutorias filtro) {
        String where = alcance(rol, actorId)
                + (filtro.estado().isEmpty() ? "" : " and t.estado=?")
                + (filtro.desde().isEmpty() ? "" : " and t.ftutoria>=?")
                + (filtro.hasta().isEmpty() ? "" : " and t.ftutoria<=?")
                + (filtro.buscar().isEmpty() ? "" : " and (lower(t.nombre) like ? escape '!' or lower(d.nombre) like ? escape '!' or lower(t.experiencia) like ? escape '!')");
        String from = " from tutoria t join docente d on d.id=t.docenteId where " + where;
        try {
            int total;
            try (PreparedStatement statement = conn.prepareStatement("select count(*)" + from)) {
                filtros(statement, rol, actorId, filtro);
                try (ResultSet result = statement.executeQuery()) { result.next(); total = result.getInt(1); }
            }
            int paginas = Math.max(1, (int) Math.ceil(total / (double) FiltroTutorias.TAMANO));
            int pagina = Math.min(filtro.pagina(), paginas);
            List<Tutoria> tutorias = new ArrayList<>();
            try (PreparedStatement statement = conn.prepareStatement("select t.*, d.nombre as nombre_docente" + from
                    + " order by t.ftutoria desc,t.hora_inicio desc,t.id desc limit ? offset ?")) {
                int index = filtros(statement, rol, actorId, filtro);
                statement.setInt(index++, FiltroTutorias.TAMANO);
                statement.setInt(index, (pagina - 1) * FiltroTutorias.TAMANO);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) tutorias.add(readConDocente(result));
                }
            }
            return new Pagina(tutorias, total, pagina, paginas);
        } catch (SQLException exception) { throw failure(); }
    }

    private int filtros(PreparedStatement statement, Rol rol, int actorId, FiltroTutorias filtro) throws SQLException {
        int index = identidad(statement, rol, actorId);
        if (!filtro.estado().isEmpty()) statement.setString(index++, filtro.estado());
        if (!filtro.desde().isEmpty()) statement.setString(index++, filtro.desde());
        if (!filtro.hasta().isEmpty()) statement.setString(index++, filtro.hasta());
        if (!filtro.buscar().isEmpty()) {
            String term = "%" + filtro.buscar().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            for (int i = 0; i < 3; i++) statement.setString(index++, term);
        }
        return index;
    }

    public Resumen resumen(Rol rol, int actorId) {
        String sql = "select t.estado,count(*) as cantidad from tutoria t where " + alcance(rol, actorId) + " group by t.estado";
        int pendientes = 0, aceptadas = 0, completadas = 0, cerradas = 0, total = 0;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            identidad(statement, rol, actorId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int cantidad = result.getInt("cantidad"); total += cantidad;
                    switch (EstadoTutoria.desdeValor(result.getString("estado"))) {
                        case PENDIENTE -> pendientes = cantidad;
                        case ACEPTADA -> aceptadas = cantidad;
                        case COMPLETADA -> completadas = cantidad;
                        default -> cerradas += cantidad;
                    }
                }
            }
            return new Resumen(pendientes, aceptadas, completadas, cerradas, total);
        } catch (SQLException exception) { throw failure(); }
    }

    /** Includes sessions in progress; pending requests are never presented as confirmed appointments. */
    public Tutoria proxima(Rol rol, int actorId) {
        String sql = "select t.*,d.nombre as nombre_docente from tutoria t join docente d on d.id=t.docenteId where "
                + alcance(rol, actorId) + " and t.estado='Aceptada' and t.ftutoria>=? and t.hora_inicio is not null"
                + " order by t.ftutoria,t.hora_inicio,t.id";
        LocalDateTime now = ahora();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            int index = identidad(statement, rol, actorId);
            statement.setString(index, now.toLocalDate().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Tutoria tutoria = readConDocente(result);
                    LocalDateTime start = ReglasTutoria.inicio(tutoria);
                    if (start != null && start.plusMinutes(tutoria.getDuracionMinutos()).isAfter(now)) return tutoria;
                }
            }
            return null;
        } catch (SQLException exception) { throw failure(); }
    }

    private Tutoria readConDocente(ResultSet result) throws SQLException {
        Tutoria tutoria = read(result);
        tutoria.setNombreDocente(result.getString("nombre_docente"));
        return tutoria;
    }

    /** A comment never changes the workflow state. Ownership is also enforced in SQL. */
    public boolean updateComentario(int tutoriaId, int docenteId, String comentario) {
        if (comentario == null || comentario.isBlank() || comentario.length() > 500) {
            throw new IllegalArgumentException("El comentario debe contener entre 1 y 500 caracteres.");
        }
        try (PreparedStatement statement = conn.prepareStatement("update tutoria set comentario=? where id=? and docenteId=?")) {
            statement.setString(1, comentario);
            statement.setInt(2, tutoriaId);
            statement.setInt(3, docenteId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    private List<Tutoria> list(String sql, Integer id) {
        List<Tutoria> list = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            if (id != null) statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) list.add(read(result));
            }
            return list;
        } catch (SQLException exception) { throw failure(); }
    }

    private Tutoria read(ResultSet result) throws SQLException {
        Tutoria tutoria = new Tutoria(result.getInt("id"), result.getInt("userId"), result.getString("nombre"),
                result.getString("genero"), result.getString("edad"), result.getString("ftutoria"), result.getString("email"),
                result.getString("telefono"), result.getString("experiencia"), result.getInt("docenteId"),
                result.getString("direccion"), result.getString("estado"));
        tutoria.setComentario(result.getString("comentario"));
        Time time = result.getTime("hora_inicio");
        tutoria.setHoraInicio(time == null ? null : time.toLocalTime());
        int duration = result.getInt("duracion_minutos");
        tutoria.setDuracionMinutos(result.wasNull() ? null : duration);
        return tutoria;
    }
    private IllegalStateException failure() { return new IllegalStateException("No se pudo completar la operación de tutorías."); }
}
