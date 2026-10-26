package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.ReglasTutoria;
import com.tme.service.ReglasTutoria.Rol;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import org.h2.tools.RunScript;
import org.junit.*;
import static org.junit.Assert.*;

/** Local in-memory integration checks; never connects to the deployed database. */
public class ReservaDirectaTest {
    private Connection conn;
    private final LocalDateTime ahora = LocalDateTime.of(2030, 1, 7, 10, 0);
    private Clock clock;
    private TutoriaDAO reservas;

    @Before public void preparar() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:reservas;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        try (Reader reader = Files.newBufferedReader(Path.of("database/schema.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(conn, reader);
        }
        clock = Clock.fixed(ahora.atZone(ReglasTutoria.ZONA).toInstant(), ReglasTutoria.ZONA);
        reservas = new TutoriaDAO(conn, clock);
        ejecutar("insert into user_details(nombre,email,clave) values('Alumno','alumno@example.test','test')");
        ejecutar("insert into user_details(nombre,email,clave) values('Otro','otro@example.test','test')");
        ejecutar("insert into docente(nombre,fnacim,calificacion,email,telefono,clave) values('Profesor','','','profesor@example.test','','test')");
        ejecutar("insert into especialidad(especialidad_nombre) values('Matematica')");
        ejecutar("insert into docente_curso values(1,1)");
        ejecutar("insert into tarifa_curso(curso_id,duracion_minutos,precio,porcentaje_docente) values(1,60,50,80)");
        ejecutar("insert into disponibilidad(docente_id,fecha,hora_desde,hora_hasta,creado_por) values(1,'2030-01-08','09:00','18:00','DOCENTE')");
        new ConfiguracionReservasDAO(conn).guardar(120,1440);
        new ConfiguracionPagosDAO(conn).guardar("Banco", "Equipo", "Cuenta de prueba", "Adjuntar comprobante", 60);
    }
    @After public void cerrar() throws Exception { if (conn != null) conn.close(); }
    private void ejecutar(String sql) throws SQLException { try (Statement s=conn.createStatement()) { s.execute(sql); } }
    private Tutoria nueva(int alumno, int hora) {
        Tutoria t = new Tutoria(alumno,"Alumno","","","2030-01-08","alumno@example.test","999999999","Algebra",1,"","Pendiente");
        t.setHoraInicio(LocalTime.of(hora,0)); t.setDuracionMinutos(60);
        t.setCursoId(1); t.setTarifaId(1); t.setNivel("Secundaria"); return t;
    }
    @Test public void reservaHabilitaPagoSinAceptarProfesor() throws Exception {
        Tutoria t=nueva(1,10); assertTrue(reservas.addTutoria(t));
        Tutoria guardada=reservas.getTutoriaById(t.getId());
        assertEquals("Pendiente de pago",guardada.getEstado());
        assertEquals(ahora.plusMinutes(120),guardada.getVencePago());
        assertNull(guardada.getVenceRespuesta());
        assertTrue(new ConfiguracionPagosDAO(conn).condiciones(t.getId()).completa());
        assertFalse(ReglasTutoria.confirmada(guardada));
        assertEquals(1,reservas.resumen(Rol.DOCENTE,1).pendientes());
        for (Rol rol:Rol.values()) {
            assertFalse(GestionAsesoriasDAO.acciones(guardada,rol,ahora).contains("aceptar"));
            assertFalse(GestionAsesoriasDAO.acciones(guardada,rol,ahora).contains("rechazar"));
        }
        assertThrows(IllegalArgumentException.class,()->new GestionAsesoriasDAO(conn,clock)
            .gestionar(t.getId(),1,Rol.DOCENTE,"aceptar","","","","",0));
    }
    @Test public void bloqueaCruceHastaQueVenceLaRetencion() {
        Tutoria t=nueva(1,13); assertTrue(reservas.addTutoria(t));
        assertThrows(IllegalArgumentException.class,()->reservas.addTutoria(nueva(2,13)));
        Clock despues=Clock.offset(clock,Duration.ofMinutes(121));
        new GestionAsesoriasDAO(conn,despues).expirar();
        assertEquals("Vencida",reservas.getTutoriaById(t.getId()).getEstado());
        assertTrue(new TutoriaDAO(conn,despues).addTutoria(nueva(2,13)));
    }
    @Test public void respetaAnticipacionYDisponibilidad() throws Exception {
        assertThrows(IllegalArgumentException.class,()->reservas.addTutoria(nueva(1,9)));
        ejecutar("update disponibilidad set activa=false");
        assertThrows(IllegalArgumentException.class,()->reservas.addTutoria(nueva(1,10)));
    }
    @Test public void configuracionNoPermiteMenosDe24HorasNiPagoMayorQueAnticipacion() {
        ConfiguracionReservasDAO c=new ConfiguracionReservasDAO(conn);
        assertThrows(IllegalArgumentException.class,()->c.guardar(60,1439));
        assertThrows(IllegalArgumentException.class,()->c.guardar(1500,1440));
        assertThrows(IllegalArgumentException.class,()->c.guardar(0,1440));
    }
    @Test public void requiereConfiguracionDeCobro() throws Exception {
        ejecutar("update configuracion_pagos set destino=null");
        assertThrows(IllegalArgumentException.class,()->reservas.addTutoria(nueva(1,10)));
        assertTrue(reservas.getAllTutoria().isEmpty());
    }
    @Test public void profesorInactivoNoRecibeReservas() throws Exception {
        ejecutar("update docente set activo=false");
        assertFalse(reservas.addTutoria(nueva(1,10)));
        assertTrue(reservas.getAllTutoria().isEmpty());
    }
    @Test public void comprobanteNoConfirmaPeroValidacionAdministrativaSi() {
        Tutoria t=nueva(1,10); assertTrue(reservas.addTutoria(t));
        PagosDAO pagos=new PagosDAO(conn,clock);
        assertTrue(pagos.registrar(t.getId(),1,Rol.ALUMNO,0,"50.00","PRUEBA-01",ahora,
            "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII),false,""));
        assertEquals("Pendiente de pago",reservas.getTutoriaById(t.getId()).getEstado());
        Clock despues=Clock.offset(clock,Duration.ofMinutes(121));
        new GestionAsesoriasDAO(conn,despues).expirar();
        assertEquals("Pendiente de pago",reservas.getTutoriaById(t.getId()).getEstado());
        assertThrows(IllegalArgumentException.class,()->pagos.gestionar(t.getId(),1,Rol.ALUMNO,1,"validar","Ingreso revisado","50.00","",ahora,0));
        assertTrue(pagos.gestionar(t.getId(),0,Rol.ADMIN,1,"validar","Ingreso verificado en banco","50.00","",ahora,0));
        assertEquals("Confirmada",reservas.getTutoriaById(t.getId()).getEstado());
    }
    @Test public void retirarFranjaNoCancelaReserva() throws Exception {
        Tutoria t=nueva(1,10); assertTrue(reservas.addTutoria(t));
        ejecutar("update disponibilidad set activa=false");
        assertEquals("Pendiente de pago",reservas.getTutoriaById(t.getId()).getEstado());
        assertThrows(IllegalArgumentException.class,()->reservas.addTutoria(nueva(2,12)));
    }
}
