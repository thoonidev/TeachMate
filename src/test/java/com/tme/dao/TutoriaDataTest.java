package com.tme.dao;

import static org.junit.Assert.*;
import com.tme.modal.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalTime;
import org.junit.*;

public class TutoriaDataTest {
    private Connection connection;
    private TestDatabase database;
    private static final java.time.Clock CLOCK = java.time.Clock.fixed(java.time.Instant.parse("2026-10-22T15:00:00Z"), java.time.ZoneOffset.UTC);

    @Before public void createDatabase() throws Exception {
        database = new TestDatabase();
        connection = database.connection;
        run(Files.readString(Path.of("database/schema.sql")));
        run("insert into user_details(id,nombre,email,clave) values(1,'Alumno','a@example.invalid','unused');"
                + "insert into docente(id,nombre,fnacim,calificacion,especialidad,email,telefono,clave) "
                + "values(1,'Docente','1990-01-01','Prueba','Algebra','d@example.invalid','0','unused');");
    }
    @After public void closeDatabase() throws Exception { if (database != null) database.close(); }

    private void run(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String part : sql.replace("\uFEFF", "").replaceAll("(?m)^--.*$", "").split(";"))
                if (!part.isBlank()) statement.execute(part);
        }
    }
    private int scalar(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next(); return result.getInt(1);
        }
    }
    private Tutoria request() {
        Tutoria tutoria = new Tutoria(1, "Alumno", "Prueba", "20", "2026-12-01", "a@example.invalid",
                "0", "Algebra", 1, "Prueba", "Aceptada");
        tutoria.setHoraInicio(LocalTime.of(10, 30));
        tutoria.setDuracionMinutos(60);
        return tutoria;
    }

    @Test public void newRequestStoresScheduleAndAlwaysStartsPending() throws Exception {
        TutoriaDAO dao = new TutoriaDAO(connection, CLOCK);
        assertTrue(dao.addTutoria(request()));
        Tutoria saved = dao.getAllTutoria().get(0);
        assertEquals("Pendiente", saved.getEstado());
        assertEquals("", saved.getComentario());
        assertEquals(LocalTime.of(10, 30), saved.getHoraInicio());
        assertEquals(Integer.valueOf(60), saved.getDuracionMinutos());
        assertEquals("10:30 · 60 min", saved.getHorario());
    }

    @Test public void commentsPreserveStateAndEnforceOwnership() throws Exception {
        TutoriaDAO dao = new TutoriaDAO(connection, CLOCK);
        assertTrue(dao.addTutoria(request()));
        int id = dao.getAllTutoria().get(0).getId();
        assertFalse(dao.updateComentario(id, 999, "Otro docente"));
        assertTrue(dao.updateComentario(id, 1, "Repasar ecuaciones"));
        assertEquals("Pendiente", dao.getTutoriaById(id).getEstado());
        assertEquals("Repasar ecuaciones", dao.getTutoriaById(id).getComentario());
    }

    @Test public void deactivationPreservesHistoryAndRejectsNewRequests() throws Exception {
        TutoriaDAO tutorias = new TutoriaDAO(connection, CLOCK);
        DocenteDAO docentes = new DocenteDAO(connection);
        assertTrue(tutorias.addTutoria(request()));
        assertTrue(docentes.deactivateDocenteById(1));
        assertFalse(docentes.deactivateDocenteById(1));
        assertFalse(docentes.getDocenteById(1).isActivo());
        assertEquals(1, docentes.getAllDocente().size());
        assertTrue(docentes.getActiveDocentes().isEmpty());
        assertEquals(1, tutorias.getAllTutoria().size());
        assertFalse(tutorias.addTutoria(request()));
        assertNull(docentes.loginDocente("d@example.invalid", "unused"));
    }

    @Test public void databasePreventsDeletingDocenteWithHistory() throws Exception {
        assertTrue(new TutoriaDAO(connection, CLOCK).addTutoria(request()));
        try { run("delete from docente where id=1"); fail("Expected foreign key restriction"); }
        catch (SQLException expected) { }
        assertEquals(1, scalar("select count(*) from tutoria"));
        assertEquals(1, scalar("select count(*) from docente"));
    }

    @Test public void databaseRejectsInvalidStateAndIncompleteSchedule() throws Exception {
        assertTrue(new TutoriaDAO(connection, CLOCK).addTutoria(request()));
        for (String sql : new String[] {"update tutoria set estado='Texto libre'",
                "update tutoria set duracion_minutos=0", "update tutoria set hora_inicio=null"}) {
            try { run(sql); fail("Expected constraint violation: " + sql); }
            catch (SQLException expected) { }
        }
    }

    @Test public void requestsRequireScheduleButLegacyRowsCanHaveNoTime() throws Exception {
        Tutoria request = request();
        request.setHoraInicio(null);
        try { new TutoriaDAO(connection).addTutoria(request); fail("Expected schedule validation"); }
        catch (IllegalArgumentException expected) { }
        run("insert into tutoria(userId,nombre,genero,edad,ftutoria,email,telefono,experiencia,docenteId,direccion) "
                + "values(1,'Antigua','Prueba','20','2024-06-01','a@example.invalid','0','Prueba',1,'Prueba')");
        assertEquals("Por definir", new TutoriaDAO(connection).getAllTutoria().get(0).getHorario());
    }

    @Test public void stateTransitionsDoNotReopenTerminalStates() {
        assertTrue(EstadoTutoria.PENDIENTE.puedeCambiarA(EstadoTutoria.ACEPTADA));
        assertTrue(EstadoTutoria.PENDIENTE.puedeCambiarA(EstadoTutoria.RECHAZADA));
        assertTrue(EstadoTutoria.ACEPTADA.puedeCambiarA(EstadoTutoria.COMPLETADA));
        assertTrue(EstadoTutoria.ACEPTADA.puedeCambiarA(EstadoTutoria.CANCELADA));
        assertFalse(EstadoTutoria.PENDIENTE.puedeCambiarA(EstadoTutoria.COMPLETADA));
        for (EstadoTutoria terminal : new EstadoTutoria[] {EstadoTutoria.RECHAZADA, EstadoTutoria.CANCELADA, EstadoTutoria.COMPLETADA})
            for (EstadoTutoria next : EstadoTutoria.values()) assertFalse(terminal.puedeCambiarA(next));
    }

    @Test public void migrationPreservesLegacyCommentsIdsAndRelationships() throws Exception {
        database.clearTables();
        run("create table user_details(id int primary key,nombre varchar(45),email varchar(45),clave varchar(255));"
                + "create table docente(id int primary key,nombre varchar(45),fnacim varchar(45),calificacion varchar(45),especialidad varchar(45),email varchar(45),telefono varchar(45),clave varchar(255));"
                + "create table tutoria(id int primary key,userId int,nombre varchar(45),genero varchar(45),edad varchar(45),ftutoria varchar(45),email varchar(45),telefono varchar(45),experiencia varchar(45),docenteId int,direccion varchar(500),estado varchar(500) not null,"
                + "constraint uid foreign key(userId) references user_details(id) on delete cascade,"
                + "constraint did foreign key(docenteId) references docente(id) on delete cascade);"
                + "insert into user_details values(7,'Alumno','a@example.invalid','stored-hash');"
                + "insert into docente values(9,'Docente','1990-01-01','Prueba','Algebra','d@example.invalid','0','stored-hash');");
        String[] legacy = {"Pendiente", "Repasar ecuaciones y práctica", "Aceptada", "Completada"};
        for (int i = 0; i < legacy.length; i++) {
            try (PreparedStatement statement = connection.prepareStatement("insert into tutoria values(?,7,'Alumno','Prueba','20','2024-06-01','a@example.invalid','0','Prueba',9,'Prueba',?)")) {
                statement.setInt(1, 100 + i); statement.setString(2, legacy[i]); statement.executeUpdate();
            }
        }
        run(Files.readString(Path.of("database/tutorias-migration.sql")));
        TutoriaDAO dao = new TutoriaDAO(connection);
        assertEquals(4, dao.getAllTutoria().size());
        for (int i = 0; i < legacy.length; i++) {
            Tutoria migrated = dao.getTutoriaById(100 + i);
            assertEquals(7, migrated.getUserId());
            assertEquals(9, migrated.getDocenteId());
            assertEquals(i == 0 ? "Pendiente" : "Completada", migrated.getEstado());
            assertEquals(i == 0 ? "" : legacy[i], migrated.getComentario());
            assertNull(migrated.getHoraInicio());
            assertNull(migrated.getDuracionMinutos());
        }
        assertTrue(new DocenteDAO(connection).isActiveDocente(9));
        try { run("delete from docente where id=9"); fail("Expected preserved foreign key"); }
        catch (SQLException expected) { }
        assertEquals(4, scalar("select count(*) from tutoria"));
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("select clave from user_details where id=7")) {
            assertTrue(result.next()); assertEquals("stored-hash", result.getString(1));
        }
    }
}
