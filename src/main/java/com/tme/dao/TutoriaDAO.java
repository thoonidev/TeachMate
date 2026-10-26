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
    LocalDateTime ahora() { return LocalDateTime.ofInstant(clock.instant(), ReglasTutoria.ZONA); }

    public boolean addTutoria(Tutoria tutoria) {
        return transaction(() -> {
            // Lock both agendas before checking availability, including when no appointments exist yet.
            if (!lockAlumno(tutoria.getUserId()) || !lockDocente(tutoria.getDocenteId())) return false;
            LocalDateTime instante=ahora();
            ReglasTutoria.validarHorario(tutoria, instante);
            var plazos=new ConfiguracionReservasDAO(conn).exigir(ReglasTutoria.inicio(tutoria),instante);
            if (tutoria.getCursoId() == null || tutoria.getTarifaId() == null) throw new IllegalArgumentException("Selecciona un curso, una tarifa y un horario desde la oferta.");
            OfertaDAO.Tarifa tarifa = new OfertaDAO(conn).validarReserva(tutoria.getDocenteId(), tutoria.getCursoId(), tutoria.getTarifaId(),
                    java.time.LocalDate.parse(tutoria.getFtutoria()), tutoria.getHoraInicio(), tutoria.getDuracionMinutos());
            String cursoNombre;
            try (PreparedStatement s = conn.prepareStatement("select especialidad_nombre from especialidad where id=?")) {
                s.setInt(1, tutoria.getCursoId()); try (ResultSet r = s.executeQuery()) { r.next(); cursoNombre=r.getString(1); }
            }
            validarDisponibilidad(tutoria);
            LocalDateTime vencePago = instante.plusMinutes(plazos.pago());
            String sql = "insert into tutoria(userId,nombre,genero,edad,ftutoria,email,telefono,experiencia,docenteId,direccion,estado,hora_inicio,duracion_minutos,curso_id,tarifa_id,curso_nombre,precio,porcentaje_docente,nivel,creada_en,vence_pago,plazo_pago_minutos) "
                    + "select ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,? from docente where id=? and activo=true";
            try (PreparedStatement statement = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
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
                statement.setString(11, "Pendiente de pago");
                statement.setTime(12, Time.valueOf(tutoria.getHoraInicio()));
                statement.setInt(13, tutoria.getDuracionMinutos());
                statement.setInt(14, tutoria.getCursoId());
                statement.setInt(15, tarifa.id());
                statement.setString(16, cursoNombre);
                statement.setBigDecimal(17, tarifa.precio());
                statement.setBigDecimal(18, tarifa.porcentaje());
                statement.setString(19,tutoria.getNivel());
                statement.setObject(20,instante);
                statement.setObject(21,vencePago);
                statement.setInt(22,plazos.pago());
                statement.setInt(23, tutoria.getDocenteId());
                if(statement.executeUpdate()!=1)return false;
                try(ResultSet keys=statement.getGeneratedKeys()){if(!keys.next())throw new SQLException("No se obtuvo el identificador de la asesoría.");tutoria.setId(keys.getInt(1));}
                new ConfiguracionPagosDAO(conn).copiar(tutoria.getId());
                try(PreparedStatement revision=conn.prepareStatement("update tutoria set revision_dias=?,revision_desde=?,revision_hasta=?,revision_minutos=?,revision_prevista=? where id=?")) {
                    revision.setInt(1,plazos.dias());revision.setObject(2,plazos.desde());revision.setObject(3,plazos.hasta());
                    revision.setInt(4,plazos.revision());revision.setObject(5,plazos.calendario().sumarRevision(vencePago));
                    revision.setInt(6,tutoria.getId());revision.executeUpdate();
                }
                GestionAsesoriasDAO.evento(conn,tutoria.getId(),"ALUMNO",tutoria.getUserId(),"Reserva","Horario publicado reservado. Comprobante de pago hasta "+vencePago+" (Lima). La confirmación requiere validación administrativa del ingreso.",instante);
                return true;
            }
        });
    }

    boolean esPropietario(Tutoria tutoria, int actorId, Rol rol) {
        return rol == Rol.ADMIN || (rol == Rol.ALUMNO && tutoria.getUserId() == actorId)
                || (rol == Rol.DOCENTE && tutoria.getDocenteId() == actorId);
    }

    boolean lockAlumno(int id) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("select id from user_details where id=? for update")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    boolean lockDocente(int id) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("select activo from docente where id=? for update")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next() && result.getBoolean(1); }
        }
    }

    void validarDisponibilidad(Tutoria tutoria) throws SQLException {
        // Agenda locks serialize competing requests; this first consistent read sees their committed work.
        // Avoid range locks here so independent agendas can still reserve concurrently on MySQL.
        String sql = "select * from tutoria where (userId=? or docenteId=?) and estado in ('Pendiente','Pendiente de pago','Confirmada','Aceptada','Incidencia') and id<>?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, tutoria.getUserId());
            statement.setInt(2, tutoria.getDocenteId());
            statement.setInt(3, tutoria.getId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Tutoria otra = read(result);
                    if (ReglasTutoria.vigente(otra,ahora()) && ReglasTutoria.seCruzan(tutoria, otra)) {
                        throw new IllegalArgumentException(otra.getUserId() == tutoria.getUserId()
                                ? "Ya tienes una tutoría pendiente o aceptada en ese horario. Elige otro horario."
                                : "El docente tiene una tutoría pendiente o aceptada en ese horario. Elige otro horario o docente.");
                    }
                }
            }
        }
    }

    @FunctionalInterface interface Operation { boolean run() throws SQLException; }
    boolean transaction(Operation operation) {
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
        } catch (SQLException exception) { throw new IllegalStateException("No se pudo completar la operación de tutorías.",exception); }
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
                + (filtro.estado().isEmpty() ? "" : filtro.estado().equals("Confirmadas")?" and t.estado in ('Confirmada','Aceptada')":" and t.estado=?")
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
                    + (filtro.estado().equals("Pendiente") ? " order by t.vence_respuesta,t.ftutoria,t.hora_inicio,t.id" : filtro.estado().equals("Pendiente de pago") ? " order by t.vence_pago,t.ftutoria,t.hora_inicio,t.id" : " order by t.ftutoria desc,t.hora_inicio desc,t.id desc") + " limit ? offset ?")) {
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
        if (!filtro.estado().isEmpty()&&!filtro.estado().equals("Confirmadas")) statement.setString(index++, filtro.estado());
        if (!filtro.desde().isEmpty()) statement.setString(index++, filtro.desde());
        if (!filtro.hasta().isEmpty()) statement.setString(index++, filtro.hasta());
        if (!filtro.buscar().isEmpty()) {
            String term = "%" + filtro.buscar().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            for (int i = 0; i < 3; i++) statement.setString(index++, term);
        }
        return index;
    }

    public Resumen resumen(Rol rol, int actorId) {
        new GestionAsesoriasDAO(conn,clock).expirar();
        String sql = "select t.estado,count(*) as cantidad from tutoria t where " + alcance(rol, actorId) + " group by t.estado";
        int pendientes = 0, aceptadas = 0, completadas = 0, cerradas = 0, total = 0;
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            identidad(statement, rol, actorId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int cantidad = result.getInt("cantidad"); total += cantidad;
                    switch (EstadoTutoria.desdeValor(result.getString("estado"))) {
                        case PENDIENTE, PENDIENTE_PAGO -> pendientes += cantidad;
                        case ACEPTADA, CONFIRMADA -> aceptadas += cantidad;
                        case COMPLETADA -> completadas = cantidad;
                        case CANCELADA, RECHAZADA, VENCIDA -> cerradas += cantidad;
                        default -> { }
                    }
                }
            }
            return new Resumen(pendientes, aceptadas, completadas, cerradas, total);
        } catch (SQLException exception) { throw failure(); }
    }

    /** Includes sessions in progress; pending requests are never presented as confirmed appointments. */
    public Tutoria proxima(Rol rol, int actorId) {
        String sql = "select t.*,d.nombre as nombre_docente from tutoria t join docente d on d.id=t.docenteId where "
                + alcance(rol, actorId) + " and t.estado in ('Aceptada','Confirmada') and t.ftutoria>=? and t.hora_inicio is not null"
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

    Tutoria read(ResultSet result) throws SQLException {
        Tutoria tutoria = new Tutoria(result.getInt("id"), result.getInt("userId"), result.getString("nombre"),
                result.getString("genero"), result.getString("edad"), result.getString("ftutoria"), result.getString("email"),
                result.getString("telefono"), result.getString("experiencia"), result.getInt("docenteId"),
                result.getString("direccion"), result.getString("estado"));
        tutoria.setComentario(result.getString("comentario"));
        Time time = result.getTime("hora_inicio");
        tutoria.setHoraInicio(time == null ? null : time.toLocalTime());
        int duration = result.getInt("duracion_minutos");
        tutoria.setDuracionMinutos(result.wasNull() ? null : duration);
        int cursoId = result.getInt("curso_id"); tutoria.setCursoId(result.wasNull() ? null : cursoId);
        int tarifaId = result.getInt("tarifa_id"); tutoria.setTarifaId(result.wasNull() ? null : tarifaId);
        tutoria.setCursoNombre(result.getString("curso_nombre"));
        tutoria.setPrecio(result.getBigDecimal("precio"));
        tutoria.setPorcentajeDocente(result.getBigDecimal("porcentaje_docente"));
        tutoria.setNivel(result.getString("nivel"));
        tutoria.setEnlaceClase(result.getString("enlace_clase"));
        tutoria.setAsistencia(result.getString("asistencia"));
        tutoria.setCancelacionSolicitada(result.getBoolean("cancelacion_solicitada"));
        tutoria.setPagoRevision(result.getBoolean("pago_revision"));
        tutoria.setMotivoCancelacion(result.getString("motivo_cancelacion"));
        tutoria.setCreadaEn(fechaHora(result,"creada_en"));
        tutoria.setVenceRespuesta(fechaHora(result,"vence_respuesta"));
        tutoria.setVencePago(fechaHora(result,"vence_pago"));
        tutoria.setRevisionPrevista(fechaHora(result,"revision_prevista"));
        Integer revisionDias=(Integer)result.getObject("revision_dias");
        if(revisionDias!=null)tutoria.setCalendarioRevision(new com.tme.service.CalendarioAtencion(revisionDias,
            result.getObject("revision_desde",java.time.LocalTime.class),result.getObject("revision_hasta",java.time.LocalTime.class),result.getInt("revision_minutos")));
        int plazo=result.getInt("plazo_pago_minutos");tutoria.setPlazoPagoMinutos(result.wasNull()?null:plazo);
        return tutoria;
    }
    private LocalDateTime fechaHora(ResultSet r,String campo)throws SQLException { return r.getObject(campo,LocalDateTime.class); }
    public List<Tutoria> reservasActivas(int docente,int alumno) {
        List<Tutoria> result=new ArrayList<>();
        try(PreparedStatement s=conn.prepareStatement("select * from tutoria where (docenteId=? or userId=?) and estado in ('Pendiente','Pendiente de pago','Confirmada','Aceptada','Incidencia')")) {
            s.setInt(1,docente);s.setInt(2,alumno);try(ResultSet r=s.executeQuery()){while(r.next()){Tutoria t=read(r);if(ReglasTutoria.vigente(t,ahora()))result.add(t);}}
            return result;
        }catch(SQLException e){throw failure();}
    }
    private IllegalStateException failure() { return new IllegalStateException("No se pudo completar la operación de tutorías."); }
}
