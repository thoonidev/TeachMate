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
        new ConfiguracionReservasDAO(conn).guardar(120,1440,63,LocalTime.of(9,0),LocalTime.of(18,0),120);
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
        assertThrows(IllegalArgumentException.class,()->c.guardar(60,1439,63,LocalTime.of(9,0),LocalTime.of(18,0),120));
        assertThrows(IllegalArgumentException.class,()->c.guardar(1500,1440,63,LocalTime.of(9,0),LocalTime.of(18,0),120));
        assertThrows(IllegalArgumentException.class,()->c.guardar(0,1440,63,LocalTime.of(9,0),LocalTime.of(18,0),120));
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
    @Test public void finDeSemanaSoloOfreceClasesConRevisionSuficiente() throws Exception {
        LocalDateTime sabado=LocalDateTime.of(2030,1,12,23,0);
        TutoriaDAO finDeSemana=new TutoriaDAO(conn,Clock.fixed(sabado.atZone(ReglasTutoria.ZONA).toInstant(),ReglasTutoria.ZONA));
        ejecutar("insert into disponibilidad(docente_id,fecha,hora_desde,hora_hasta,creado_por) values(1,'2030-01-14','09:00','18:00','DOCENTE')");
        Clock relojSabado=Clock.fixed(sabado.atZone(ReglasTutoria.ZONA).toInstant(),ReglasTutoria.ZONA);
        var ofrecidos=new OfertaDAO(conn,relojSabado).horarios(1,60,1);
        assertFalse(ofrecidos.isEmpty());
        assertEquals(LocalTime.of(11,0),ofrecidos.get(0).inicio());
        assertEquals(LocalDate.of(2030,1,14),ofrecidos.get(0).fecha());
        Tutoria t=nueva(1,10);t.setFtutoria("2030-01-14");
        assertThrows(IllegalArgumentException.class,()->finDeSemana.addTutoria(t));
        t.setHoraInicio(LocalTime.of(11,0));assertTrue(finDeSemana.addTutoria(t));
        Tutoria guardada=finDeSemana.getTutoriaById(t.getId());
        assertEquals(LocalDateTime.of(2030,1,13,1,0),guardada.getVencePago());
        assertEquals(LocalDateTime.of(2030,1,14,11,0),guardada.getRevisionPrevista());
    }
    @Test public void cambiosDeConfiguracionNoReescribenReservas() {
        Tutoria t=nueva(1,10);assertTrue(reservas.addTutoria(t));
        LocalDateTime prevista=reservas.getTutoriaById(t.getId()).getRevisionPrevista();
        new ConfiguracionReservasDAO(conn).guardar(60,2880,64,LocalTime.of(10,0),LocalTime.of(12,0),180);
        Tutoria anterior=reservas.getTutoriaById(t.getId());
        assertEquals(63,anterior.getCalendarioRevision().dias());
        assertEquals(120,anterior.getCalendarioRevision().revisionMinutos());
        assertEquals(prevista,anterior.getRevisionPrevista());
        assertEquals(ahora.plusMinutes(120),anterior.getVencePago());
    }
    @Test public void corregirConservaMargenAntesDeClase() {
        new ConfiguracionPagosDAO(conn).guardar("Banco","Equipo","Cuenta","Adjuntar",10080);
        Tutoria t=nueva(1,10);assertTrue(reservas.addTutoria(t));
        PagosDAO pagos=new PagosDAO(conn,clock);
        assertTrue(pagos.registrar(t.getId(),1,Rol.ALUMNO,0,"50.00","PRUEBA-02",ahora,
            "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII),false,""));
        Clock tarde=Clock.offset(clock,Duration.ofHours(6));
        assertTrue(new PagosDAO(conn,tarde).gestionar(t.getId(),0,Rol.ADMIN,1,"observar","Corregir referencia","","",null,0));
        Tutoria observada=reservas.getTutoriaById(t.getId());
        assertEquals(ahora.withHour(17),observada.getVencePago());
        assertEquals(LocalDateTime.of(2030,1,8,10,0),observada.getRevisionPrevista());
        assertEquals(observada.getVencePago(),pagos.obtener(t.getId()).corregir());
    }
    @Test public void noPermiteCorreccionSinMargenDisponible() {
        Tutoria t=nueva(1,10);assertTrue(reservas.addTutoria(t));
        PagosDAO pagos=new PagosDAO(conn,clock);
        assertTrue(pagos.registrar(t.getId(),1,Rol.ALUMNO,0,"50.00","PRUEBA-03",ahora,
            "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII),false,""));
        Clock tarde=Clock.offset(clock,Duration.ofHours(7));
        assertThrows(IllegalArgumentException.class,()->new PagosDAO(conn,tarde)
            .gestionar(t.getId(),0,Rol.ADMIN,1,"observar","Falta referencia","","",null,0));
        assertEquals("En revisión",pagos.obtener(t.getId()).estado());
    }
    @Test public void vencimientoDeComprobanteEnRevisionGeneraIncidenciaUnaSolaVez() throws Exception {
        Tutoria t=nueva(1,10);assertTrue(reservas.addTutoria(t));
        assertTrue(new PagosDAO(conn,clock).registrar(t.getId(),1,Rol.ALUMNO,0,"50.00","PRUEBA-04",ahora,
            "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII),false,""));
        GestionAsesoriasDAO vencimientos=new GestionAsesoriasDAO(conn,Clock.offset(clock,Duration.ofDays(1)));
        vencimientos.expirar();vencimientos.expirar();
        assertEquals("Incidencia",reservas.getTutoriaById(t.getId()).getEstado());
        try(Statement s=conn.createStatement();ResultSet r=s.executeQuery("select count(*) from tutoria_evento where accion='Vencimiento'")){
            assertTrue(r.next());assertEquals(1,r.getInt(1));
        }
    }
    @Test public void procesoPeriodicoVencePropuestasSinAbrirElDetalle() throws Exception {
        Tutoria t=nueva(1,10);assertTrue(reservas.addTutoria(t));
        ejecutar("update tutoria set estado='Confirmada' where id="+t.getId());
        ejecutar("insert into tutoria_cambio(tutoria_id,version,fecha,hora,motivo,estado,vence_en) values("+t.getId()+",1,'2030-01-09','10:00','Prueba','Pendiente','2030-01-07 11:00:00')");
        GestionAsesoriasDAO vencimientos=new GestionAsesoriasDAO(conn,Clock.offset(clock,Duration.ofHours(2)));
        vencimientos.expirar();vencimientos.expirar();
        assertEquals("Vencida",vencimientos.cambio(t.getId()).estado());
        assertEquals("2030-01-08",reservas.getTutoriaById(t.getId()).getFtutoria());
        try(Statement s=conn.createStatement();ResultSet r=s.executeQuery("select count(*) from tutoria_evento where accion='Propuesta vencida'")){
            assertTrue(r.next());assertEquals(1,r.getInt(1));
        }
    }
    // H2 lacks MySQL's two-argument TIMESTAMP function used by the existing panel query.
    public static Timestamp mysqlTimestamp(String fecha,Time hora) {
        if(fecha==null||hora==null)return null;
        try { return Timestamp.valueOf(LocalDate.parse(fecha).atTime(hora.toLocalTime())); }
        catch(DateTimeException e){return null;}
    }
    @Test public void panelDePagosMuestraLaFechaDeRevisionYRespetaPropiedad() throws Exception {
        ejecutar("CREATE ALIAS timestamp FOR 'com.tme.dao.ReservaDirectaTest.mysqlTimestamp'");
        Tutoria t=nueva(1,10);assertTrue(reservas.addTutoria(t));
        PagosDAO pagos=new PagosDAO(conn,clock);
        var propios=pagos.listar(1,Rol.ALUMNO,"Todos",1);
        assertEquals(1,propios.total());
        assertEquals(ahora.plusHours(4),propios.filas().get(0).revision());
        assertEquals(0,pagos.listar(2,Rol.ALUMNO,"Todos",1).total());
    }
}
