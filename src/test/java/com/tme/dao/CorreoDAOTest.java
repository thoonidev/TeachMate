package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.*;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.tools.RunScript;
import org.junit.*;
import static org.junit.Assert.*;

public class CorreoDAOTest {
    private Connection conn;
    private CorreoDAO cola;
    private final LocalDateTime ahora=LocalDateTime.of(2030,1,7,10,0);
    private Clock clock;
    private ConfiguracionCorreo configuracion(int limite,boolean activo){
        return new ConfiguracionCorreo(activo,"clave-de-prueba","equipo@example.test","TeachMate","admin@example.test","https://teachmate.example.test",limite);
    }
    @Before public void preparar()throws Exception {
        conn=DriverManager.getConnection("jdbc:h2:mem:correos;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        try(Reader r=Files.newBufferedReader(Path.of("database/schema.sql"),StandardCharsets.UTF_8)){RunScript.execute(conn,r);}
        clock=Clock.fixed(ahora.atZone(ReglasTutoria.ZONA).toInstant(),ReglasTutoria.ZONA);
        cola=new CorreoDAO(conn,"admin@example.test");
        sql("insert into user_details(nombre,email,clave) values('Alumno','alumno@example.test','test')");
        sql("insert into docente(nombre,fnacim,calificacion,email,telefono,clave) values('Profesor','','','profesor@example.test','','test')");
        sql("insert into especialidad(especialidad_nombre) values('Matematica')");
        sql("insert into docente_curso values(1,1)");
        sql("insert into tarifa_curso(curso_id,duracion_minutos,precio,porcentaje_docente) values(1,60,50,80)");
        sql("insert into disponibilidad(docente_id,fecha,hora_desde,hora_hasta,creado_por) values(1,'2030-01-08','09:00','18:00','DOCENTE')");
        new ConfiguracionReservasDAO(conn).guardar(120,1440,63,LocalTime.of(9,0),LocalTime.of(18,0),120);
        new ConfiguracionPagosDAO(conn).guardar("Banco","Equipo","Cuenta","Adjuntar",60);
    }
    @After public void cerrar()throws Exception {if(conn!=null)conn.close();}
    private void sql(String consulta)throws SQLException {try(Statement s=conn.createStatement()){s.execute(consulta);}}
    private int cantidad(String tabla,String condicion)throws SQLException {
        try(Statement s=conn.createStatement();ResultSet r=s.executeQuery("select count(*) from "+tabla+" where "+condicion)){r.next();return r.getInt(1);}
    }
    private Tutoria reservar(){
        Tutoria t=new Tutoria(1,"Alumno","","","2030-01-08","alumno@example.test","999999999","Algebra",1,"","Pendiente de pago");
        t.setCursoId(1);t.setTarifaId(1);t.setHoraInicio(LocalTime.of(10,0));t.setDuracionMinutos(60);t.setNivel("Secundaria");
        assertTrue(new TutoriaDAO(conn,clock).addTutoria(t));return t;
    }
    private ProcesadorCorreos procesador(ProcesadorCorreos.Transporte transporte,Clock reloj){
        return new ProcesadorCorreos(cola,configuracion(250,true),transporte,reloj);
    }
    private ProcesadorCorreos.Resultado aceptado(){return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.ACEPTADO,"Aceptado",0);}

    @Test public void reservaEncolaAlumnoYProfesorSinEnviar()throws Exception {
        reservar();assertEquals(2,cola.listar().size());
        assertEquals(Set.of("alumno@example.test","profesor@example.test"),new HashSet<>(cola.listar().stream().map(CorreoDAO.Fila::destino).toList()));
        assertEquals(2,cantidad("correo_pendiente","estado='Pendiente' and intentos=0"));
    }
    @Test public void mismoEventoNoDuplicaDestinatarios()throws Exception {
        Tutoria t=reservar();cola.evento(t.getId(),"t:prueba","Reserva",ahora);cola.evento(t.getId(),"t:prueba","Reserva",ahora);
        assertEquals(4,cantidad("correo_pendiente","1=1"));
    }
    @Test public void profesorNoRecibeInstruccionesDePagoDelAlumno()throws Exception {
        reservar();
        try(Statement s=conn.createStatement();ResultSet r=s.executeQuery("select asunto,cuerpo from correo_pendiente where rol='DOCENTE'")){
            assertTrue(r.next());assertTrue(r.getString(1).contains("Nueva reserva"));
            assertFalse(r.getString(2).contains("Envía el comprobante"));assertTrue(r.getString(2).contains("No necesitas aceptar"));
        }
    }
    @Test public void rollbackRevierteReservaEventoYCola()throws Exception {
        sql("alter table correo_pendiente add constraint rechazo_prueba check (1=0)");
        assertThrows(IllegalStateException.class,this::reservar);
        assertEquals(0,cantidad("tutoria","1=1"));
        assertEquals(0,cantidad("correo_pendiente","1=1"));
        assertEquals(0,cantidad("tutoria_evento","tutoria_id is not null"));
    }
    @Test public void avisoParticipaEnLaMismaTransaccion()throws Exception {
        Tutoria t=reservar();conn.setAutoCommit(false);
        cola.evento(t.getId(),"t:rollback","Ingreso validado",ahora);
        conn.rollback();conn.setAutoCommit(true);
        assertEquals(2,cantidad("correo_pendiente","1=1"));
    }
    @Test public void dosProcesadoresNoReclamanElMismoMensaje()throws Exception {
        reservar();var primero=cola.reclamar(ahora);
        try(Connection otra=DriverManager.getConnection("jdbc:h2:mem:correos;MODE=MySQL;DATABASE_TO_LOWER=TRUE")){
            var segundo=new CorreoDAO(otra,"admin@example.test").reclamar(ahora);
            assertNotEquals(primero.id(),segundo.id());assertNull(cola.reclamar(ahora));
        }
    }
    @Test public void envioDesactivadoNoUsaTransporte()throws Exception {
        reservar();new ProcesadorCorreos(cola,configuracion(250,false),m->{fail("No debe enviar");return aceptado();},clock).procesar();
        assertEquals(2,cantidad("correo_pendiente","estado='Pendiente' and intentos=0"));
    }
    @Test public void reintentosConservanClaveDelProveedor()throws Exception {
        reservar();Map<Long,String> claves=new HashMap<>();AtomicInteger llamadas=new AtomicInteger();
        procesador(m->{claves.put(m.id(),m.uuid());llamadas.incrementAndGet();return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.TEMPORAL,"Fallo simulado",0);},clock).procesar();
        procesador(m->{assertEquals(claves.get(m.id()),m.uuid());llamadas.incrementAndGet();return aceptado();},Clock.offset(clock,Duration.ofMinutes(1))).procesar();
        assertEquals(4,llamadas.get());assertEquals(2,cantidad("correo_pendiente","estado='Aceptado'"));
    }
    @Test public void pausaLargaNoReenviaUnResultadoIncierto()throws Exception {
        reservar();procesador(m->new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.TEMPORAL,"Sin respuesta",0),clock).procesar();
        procesador(m->{fail("La clave de deduplicación ya venció");return aceptado();},Clock.offset(clock,Duration.ofMinutes(31))).procesar();
        assertEquals(2,cantidad("correo_pendiente","estado='Dudoso'"));
    }
    @Test public void envioInterrumpidoRequiereRevisionYNoAceptaConfirmacionAntigua()throws Exception {
        reservar();var m=cola.reclamar(ahora);cola.mantener(ahora.plusMinutes(3));
        cola.resolver(m,"Aceptado",ahora,"Respuesta tardía",ahora.plusMinutes(3));
        assertEquals(1,cantidad("correo_pendiente","estado='Dudoso'"));
        assertThrows(IllegalArgumentException.class,()->cola.gestionar(m.id(),"reintentar",false,ahora.plusMinutes(3)));
        assertTrue(cola.gestionar(m.id(),"reintentar",true,ahora.plusMinutes(3)));
        assertEquals(2,cantidad("correo_pendiente","estado='Pendiente'"));
    }
    @Test public void limiteDiarioDejaElRestoPendiente()throws Exception {
        reservar();AtomicInteger llamadas=new AtomicInteger();
        new ProcesadorCorreos(cola,configuracion(1,true),m->{llamadas.incrementAndGet();return aceptado();},clock).procesar();
        assertEquals(1,llamadas.get());assertEquals(1,cantidad("correo_pendiente","estado='Pendiente'"));
    }
    @Test public void cuotaDelProveedorRespetaElTiempoDeEspera()throws Exception {
        reservar();procesador(m->new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.CUOTA,"Cuota",3600),clock).procesar();
        assertEquals(1,cantidad("correo_pendiente","intentos=1 and programado_en='2030-01-07 11:00:00'"));
        assertEquals(0,cantidad("correo_pendiente","estado='Aceptado'"));
    }
    @Test public void recordatorioDePagoSeGeneraUnaSolaVez()throws Exception {
        reservar();cola.recordatorios(ahora.plusMinutes(100));cola.recordatorios(ahora.plusMinutes(100));
        assertEquals(1,cantidad("correo_pendiente","tipo='Recordatorio pago'"));
    }
    @Test public void cancelarClaseImpideEnviarRecordatoriosViejos()throws Exception {
        reservar();sql("update tutoria set estado='Confirmada'");
        LocalDateTime cerca=LocalDateTime.of(2030,1,8,9,30);
        cola.recordatorios(cerca);assertEquals(2,cantidad("correo_pendiente","tipo='Recordatorio clase'"));
        sql("update tutoria set estado='Cancelada'");
        procesador(m->{fail("La clase fue cancelada");return aceptado();},Clock.fixed(cerca.atZone(ReglasTutoria.ZONA).toInstant(),ReglasTutoria.ZONA)).procesar();
        assertEquals(4,cantidad("correo_pendiente","estado='Cancelado'"));
    }
    @Test public void mensajeCaducadoNoSeEnvia()throws Exception {
        reservar();procesador(m->{fail("Aviso caducado");return aceptado();},Clock.offset(clock,Duration.ofDays(2))).procesar();
        assertEquals(2,cantidad("correo_pendiente","estado='Cancelado'"));
    }
    @Test public void recordatorioCanceladoPorAdministracionNoSeReactiva()throws Exception {
        reservar();sql("update tutoria set estado='Confirmada'");
        LocalDateTime cerca=LocalDateTime.of(2030,1,8,9,30);cola.recordatorios(cerca);
        long id;
        try(Statement s=conn.createStatement();ResultSet r=s.executeQuery("select id from correo_pendiente where tipo='Recordatorio clase' order by id limit 1")){assertTrue(r.next());id=r.getLong(1);}
        assertTrue(cola.gestionar(id,"cancelar",false,cerca));
        cola.recordatorios(cerca.plusMinutes(1));
        assertEquals(1,cantidad("correo_pendiente","id="+id+" and estado='Cancelado'"));
    }
    @Test public void avisosNoContienenComprobantesNiDatosBancarios()throws Exception {
        Tutoria t=reservar();cola.evento(t.getId(),"t:pago","Comprobante recibido",ahora);
        var m=cola.reclamar(ahora);
        assertFalse(m.cuerpo().contains("Cuenta"));assertFalse(m.cuerpo().contains("Banco"));
        assertEquals(1,cantidad("correo_pendiente","tipo='Comprobante recibido' and rol='ADMIN'"));
    }
}
