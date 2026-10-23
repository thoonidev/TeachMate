package com.tme.dao;

import static org.junit.Assert.*;
import com.tme.modal.*;
import com.tme.service.ReglasTutoria;
import com.tme.service.ReglasTutoria.Rol;
import com.tme.service.FiltroTutorias;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.*;

public class TutoriaReglasTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T15:00:00Z"), ZoneOffset.UTC);
    private Connection connection;
    private TestDatabase database;
    private String jdbc;

    @Before public void createDatabase() throws Exception {
        database = new TestDatabase();
        jdbc = database.url;
        connection = database.connection;
        try (Statement statement = connection.createStatement()) {
            for (String sql : Files.readString(Path.of("database/schema.sql")).replaceAll("(?m)^--.*$", "").split(";"))
                if (!sql.isBlank()) statement.execute(sql);
            for (int id = 1; id <= 2; id++) {
                statement.execute("insert into user_details values(" + id + ",'Alumno','a" + id + "@example.invalid','unused')");
                statement.execute("insert into docente(id,nombre,fnacim,calificacion,especialidad,email,telefono,clave) values("
                        + id + ",'Docente','1990-01-01','Prueba','Algebra','d" + id + "@example.invalid','0','unused')");
            }
        }
    }
    @After public void closeDatabase() throws Exception { if (database != null) database.close(); }
    private TutoriaDAO dao() { return new TutoriaDAO(connection, CLOCK); }
    private Tutoria request(int userId, int docenteId, String hora) {
        Tutoria tutoria = new Tutoria(userId, "Alumno", "Prueba", "20", "2030-01-02", "a@example.invalid", "0", "Prueba", docenteId, "Prueba", "Pendiente");
        tutoria.setHoraInicio(LocalTime.parse(hora));
        tutoria.setDuracionMinutos(60);
        return tutoria;
    }
    private int add(Tutoria tutoria) {
        assertTrue(dao().addTutoria(tutoria));
        return dao().getAllTutoria().get(0).getId();
    }
    private void setState(int id, String state) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("update tutoria set estado=? where id=?")) {
            statement.setString(1, state); statement.setInt(2, id); statement.executeUpdate();
        }
    }

    @Test public void validatesFutureLimaTimeDurationAndSameDayEnd() throws Exception {
        for (String date : new String[] {"2029-12-31", "2030-01-01", "invalid"}) {
            Tutoria tutoria = request(1, 1, "10:00"); tutoria.setFtutoria(date);
            assertThrows(IllegalArgumentException.class, () -> dao().addTutoria(tutoria));
        }
        for (int duration : new int[] {14, 181}) {
            Tutoria tutoria = request(1, 1, "11:00"); tutoria.setDuracionMinutos(duration);
            assertThrows(IllegalArgumentException.class, () -> dao().addTutoria(tutoria));
        }
        assertThrows(IllegalArgumentException.class, () -> dao().addTutoria(request(1, 1, "23:30")));
        Tutoria seconds = request(1, 1, "11:00:30");
        assertThrows(IllegalArgumentException.class, () -> dao().addTutoria(seconds));
        Tutoria today = request(1, 1, "10:01"); today.setFtutoria("2030-01-01");
        add(today); // 15:00 UTC is 10:00 in Lima, independent of the server timezone.
        assertTrue(connection.getAutoCommit());
    }

    @Test public void blocksBothAgendasAndDuplicatesButAllowsAdjacentSlots() {
        add(request(1, 1, "10:00"));
        for (Tutoria clash : new Tutoria[] {request(1, 1, "10:00"), request(2, 1, "10:30"), request(1, 2, "09:30"), request(2, 1, "09:30")})
            assertThrows(IllegalArgumentException.class, () -> dao().addTutoria(clash));
        add(request(2, 2, "10:00"));
        add(request(1, 1, "11:00"));
        add(request(1, 1, "09:00"));
        assertEquals(4, dao().getAllTutoria().size());
    }

    @Test public void rejectedCancelledAndCompletedSlotsAreReleased() throws Exception {
        for (String state : new String[] {"Rechazada", "Cancelada", "Completada"}) {
            int id = add(request(1, 1, "10:00"));
            if (state.equals("Rechazada")) assertTrue(dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.RECHAZADA));
            else if (state.equals("Cancelada")) assertTrue(dao().cambiarEstado(id, 1, Rol.ALUMNO, EstadoTutoria.CANCELADA));
            else setState(id, state);
        }
        int id = add(request(1, 1, "10:00"));
        assertTrue(dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertThrows(IllegalArgumentException.class, () -> dao().addTutoria(request(2, 1, "10:00")));
        assertTrue(dao().cambiarEstado(id, 1, Rol.ALUMNO, EstadoTutoria.CANCELADA));
        add(request(2, 1, "10:00"));
    }

    @Test public void workflowEnforcesOwnershipRolesAndTerminalStates() {
        int id = add(request(1, 1, "10:00"));
        assertFalse(dao().cambiarEstado(id, 2, Rol.ALUMNO, EstadoTutoria.CANCELADA));
        assertFalse(dao().cambiarEstado(id, 2, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 1, Rol.ALUMNO, EstadoTutoria.ACEPTADA));
        assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 0, Rol.ADMIN, EstadoTutoria.COMPLETADA));
        assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.COMPLETADA));
        assertTrue(dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.RECHAZADA));
        assertTrue(dao().cambiarEstado(id, 0, Rol.ADMIN, EstadoTutoria.CANCELADA));
        for (EstadoTutoria next : EstadoTutoria.values())
            assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 1, Rol.DOCENTE, next));
        assertEquals("Cancelada", dao().getTutoriaById(id).getEstado());
    }

    @Test public void completionRequiresEndAndOnlyAdminCanCancelStartedAppointments() {
        int id = add(request(1, 1, "10:00"));
        assertTrue(dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        TutoriaDAO started = new TutoriaDAO(connection, Clock.fixed(Instant.parse("2030-01-02T15:00:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> started.cambiarEstado(id, 1, Rol.ALUMNO, EstadoTutoria.CANCELADA));
        assertThrows(IllegalArgumentException.class, () -> started.cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.CANCELADA));
        assertThrows(IllegalArgumentException.class, () -> started.cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.COMPLETADA));
        TutoriaDAO end = new TutoriaDAO(connection, Clock.fixed(Instant.parse("2030-01-02T16:00:00Z"), ZoneOffset.UTC));
        assertTrue(end.cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.COMPLETADA));
        int other = add(request(2, 2, "10:00"));
        assertTrue(started.cambiarEstado(other, 0, Rol.ADMIN, EstadoTutoria.CANCELADA));
        int stale = add(request(1, 1, "12:00"));
        TutoriaDAO nextDay = new TutoriaDAO(connection, Clock.fixed(Instant.parse("2030-01-03T15:00:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> nextDay.cambiarEstado(stale, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertTrue(nextDay.cambiarEstado(stale, 1, Rol.DOCENTE, EstadoTutoria.RECHAZADA));
    }

    @Test public void legacySchedulesCannotBeAcceptedButCanBeClosedWithoutInventingTimes() throws Exception {
        int id = add(request(1, 1, "10:00"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update tutoria set hora_inicio=null,duracion_minutos=null,ftutoria='2020-01-01'");
        }
        assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertTrue(dao().cambiarEstado(id, 1, Rol.ALUMNO, EstadoTutoria.CANCELADA));
        assertNull(dao().getTutoriaById(id).getHoraInicio());
    }

    @Test public void inactiveTeacherCannotAcceptButAdminCanResolveTheirRequests() {
        int id = add(request(1, 1, "10:00"));
        assertTrue(new DocenteDAO(connection).deactivateDocenteById(1));
        assertThrows(IllegalArgumentException.class, () -> dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertTrue(dao().cambiarEstado(id, 0, Rol.ADMIN, EstadoTutoria.CANCELADA));
    }

    @Test public void sqlFailureRollsBackAndRestoresConnectionForNextRequest() throws Exception {
        Tutoria invalid = request(1, 1, "10:00"); invalid.setNombre("x".repeat(46));
        assertThrows(IllegalStateException.class, () -> dao().addTutoria(invalid));
        assertTrue(connection.getAutoCommit());
        assertTrue(dao().getAllTutoria().isEmpty());
        add(request(1, 1, "10:00"));
    }

    @Test public void simultaneousRequestsCannotReserveSameTeacher() throws Exception {
        simultaneous(request(1, 1, "10:00"), request(2, 1, "10:30"));
    }
    @Test public void simultaneousRequestsCannotReserveSameStudent() throws Exception {
        simultaneous(request(1, 1, "10:00"), request(1, 2, "10:30"));
    }
    @Test public void simultaneousDuplicateSubmissionsOnlyCreateOneRequest() throws Exception {
        simultaneous(request(1, 1, "10:00"), request(1, 1, "10:00"));
    }

    @Test public void summariesAndFiltersRemainScopedToTheAuthenticatedRole() {
        add(request(1, 1, "10:00"));
        int id = add(request(2, 1, "12:00"));
        assertTrue(dao().cambiarEstado(id, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        add(request(1, 2, "14:00"));
        assertEquals(2, dao().resumen(Rol.ALUMNO, 1).total());
        assertEquals(0, dao().resumen(Rol.ALUMNO, 1).aceptadas());
        assertEquals(1, dao().resumen(Rol.ALUMNO, 2).total());
        assertEquals(2, dao().resumen(Rol.DOCENTE, 1).total());
        assertEquals(1, dao().resumen(Rol.DOCENTE, 1).aceptadas());
        assertEquals(3, dao().resumen(Rol.ADMIN, 0).total());
        FiltroTutorias accepted = new FiltroTutorias("Aceptada", "2030-01-02", "2030-01-02", null, 1);
        assertEquals(0, dao().buscar(Rol.ALUMNO, 1, accepted).total());
        assertEquals(1, dao().buscar(Rol.DOCENTE, 1, accepted).total());
        assertEquals("Docente", dao().buscar(Rol.ADMIN, 0, accepted).tutorias().get(0).getNombreDocente());
        assertThrows(IllegalArgumentException.class, () -> dao().buscar(Rol.ALUMNO, 0, accepted));
        assertThrows(IllegalArgumentException.class, () -> dao().resumen(null, 1));
    }

    @Test public void searchTreatsWildcardsAsLiteralAndNeverChangesOwnershipScope() {
        Tutoria request = request(1, 1, "10:00"); request.setExperiencia("Repasar 50%_! de álgebra");
        add(request);
        add(request(2, 2, "10:00"));
        for (String term : new String[] {"50%_!", "ÁLGEBRA", "Docente"}) {
            assertEquals(1, dao().buscar(Rol.ALUMNO, 1, new FiltroTutorias(null, null, null, term, 1)).total());
        }
        assertEquals(0, dao().buscar(Rol.ALUMNO, 2, new FiltroTutorias(null, null, null, "50%_!", 1)).total());
        assertEquals(0, dao().buscar(Rol.ADMIN, 0, new FiltroTutorias(null, null, null, "' or 1=1 --", 1)).total());
        assertEquals(1, dao().buscar(Rol.ADMIN, 0, new FiltroTutorias(null, null, null, "%", 1)).total());
    }

    @Test public void paginationIsBoundedDeterministicAndPreservesDateFilters() {
        for (int i = 0; i < 12; i++) {
            Tutoria request = request(1, 1, "10:00");
            request.setFtutoria(LocalDate.of(2030, 1, 2).plusDays(i).toString()); add(request);
        }
        TutoriaDAO.Pagina first = dao().buscar(Rol.ALUMNO, 1, new FiltroTutorias(null, null, null, null, 1));
        TutoriaDAO.Pagina last = dao().buscar(Rol.ALUMNO, 1, new FiltroTutorias(null, null, null, null, Integer.MAX_VALUE));
        assertEquals(12, first.total()); assertEquals(10, first.tutorias().size()); assertEquals(2, first.paginas());
        assertEquals(2, last.pagina()); assertEquals(2, last.tutorias().size());
        assertEquals("2030-01-13", first.tutorias().get(0).getFtutoria());
        assertEquals("2030-01-02", last.tutorias().get(1).getFtutoria());
        assertEquals(2, dao().buscar(Rol.ALUMNO, 1, new FiltroTutorias(null, "2030-01-05", "2030-01-06", null, 1)).total());
        assertEquals(0, dao().buscar(Rol.ALUMNO, 2, new FiltroTutorias(null, null, null, null, 1)).total());
    }

    @Test public void nextSessionIncludesOngoingAcceptedSessionButNotPendingOrEndedOnes() throws Exception {
        int pending = add(request(1, 1, "09:00"));
        int later = add(request(1, 1, "11:00"));
        assertTrue(dao().cambiarEstado(later, 1, Rol.DOCENTE, EstadoTutoria.ACEPTADA));
        assertEquals(later, dao().proxima(Rol.ALUMNO, 1).getId());
        assertNull(dao().proxima(Rol.ALUMNO, 2));
        int ongoing = add(request(1, 1, "13:00"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update tutoria set estado='Aceptada',ftutoria='2030-01-01',hora_inicio='09:30:00' where id=" + ongoing);
        }
        assertEquals(ongoing, dao().proxima(Rol.DOCENTE, 1).getId());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update tutoria set hora_inicio='09:00:00' where id=" + ongoing);
        }
        assertEquals(later, dao().proxima(Rol.ALUMNO, 1).getId()); // ends exactly at now
        assertEquals("Pendiente", dao().getTutoriaById(pending).getEstado());
    }

    @Test public void invalidFiltersFailBeforeTheyCanBecomeSqlOrUnboundedPages() {
        for (String date : new String[] {"bad", "2030-02-30", "+12030-01-01"}) {
            assertThrows(IllegalArgumentException.class, () -> new FiltroTutorias(null, date, null, null, 1));
        }
        assertThrows(IllegalArgumentException.class, () -> new FiltroTutorias(null, "2030-02-01", "2030-01-01", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new FiltroTutorias("bad", null, null, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new FiltroTutorias(null, null, null, "x".repeat(61), 1));
        assertThrows(IllegalArgumentException.class, () -> new FiltroTutorias(null, null, null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> FiltroTutorias.pagina("2147483648"));
    }

    @Test public void reactivatingTeacherPreservesHistoryAndDoesNotReopenCancelledRequests() {
        int id = add(request(1, 1, "10:00"));
        assertTrue(dao().cambiarEstado(id, 1, Rol.ALUMNO, EstadoTutoria.CANCELADA));
        DocenteDAO docentes = new DocenteDAO(connection);
        assertTrue(docentes.deactivateDocenteById(1));
        assertEquals(1, docentes.countActiveDocente());
        assertTrue(docentes.reactivateDocenteById(1));
        assertFalse(docentes.reactivateDocenteById(1));
        assertFalse(docentes.reactivateDocenteById(999));
        assertEquals(2, docentes.countActiveDocente());
        assertEquals("Cancelada", dao().getTutoriaById(id).getEstado());
        assertTrue(dao().addTutoria(request(1, 1, "10:00")));
    }

    private void simultaneous(Tutoria first, Tutoria second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            Future<Boolean> a = executor.submit(() -> reserve(first, ready, start));
            Future<Boolean> b = executor.submit(() -> reserve(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, dao().getAllTutoria().size());
        } finally { start.countDown(); executor.shutdownNow(); }
    }
    private boolean reserve(Tutoria tutoria, CountDownLatch ready, CountDownLatch start) throws Exception {
        try (Connection parallel = DriverManager.getConnection(jdbc, database.user, database.password)) {
            ready.countDown(); assertTrue(start.await(5, TimeUnit.SECONDS));
            try { return new TutoriaDAO(parallel, CLOCK).addTutoria(tutoria); }
            catch (IllegalArgumentException expectedConflict) { return false; }
            finally { assertTrue(parallel.getAutoCommit()); }
        }
    }
}
