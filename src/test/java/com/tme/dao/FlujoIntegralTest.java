package com.tme.dao;

import com.tme.modal.Tutoria;
import com.tme.service.*;
import com.tme.service.ReglasTutoria.Rol;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.h2.tools.RunScript;
import org.junit.*;
import static org.junit.Assert.*;

/** Full business journeys on a disposable database; no real bank or email service. */
public class FlujoIntegralTest {
    private Connection conn;
    private LocalDateTime ahora;
    private Clock reloj(){return Clock.fixed(ahora.atZone(ReglasTutoria.ZONA).toInstant(),ReglasTutoria.ZONA);}
    private TutoriaDAO reservas(){return new TutoriaDAO(conn,reloj());}
    private PagosDAO pagos(){return new PagosDAO(conn,reloj());}
    private GestionAsesoriasDAO gestion(){return new GestionAsesoriasDAO(conn,reloj());}
    private LiquidacionesDAO liquidaciones(){return new LiquidacionesDAO(conn,reloj());}
    @Before public void preparar()throws Exception {
        conn=DriverManager.getConnection("jdbc:h2:mem:integral;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        try(Reader r=Files.newBufferedReader(Path.of("database/schema.sql"),StandardCharsets.UTF_8)){RunScript.execute(conn,r);}
        ahora=LocalDateTime.of(2030,1,7,10,0);
        sql("insert into user_details(nombre,email,clave) values('Alumno','alumno@example.test','test'),('Otro','otro@example.test','test')");
        sql("insert into docente(nombre,fnacim,calificacion,email,telefono,clave) values('Profesor','','','profesor@example.test','','test'),('Otro profesor','','','otroprof@example.test','','test')");
        sql("insert into especialidad(especialidad_nombre) values('Matematica')");
        sql("insert into docente_curso values(1,1)");
        sql("insert into tarifa_curso(curso_id,duracion_minutos,precio,porcentaje_docente) values(1,60,50,80)");
        sql("insert into disponibilidad(docente_id,fecha,hora_desde,hora_hasta,creado_por) values(1,'2030-01-08','09:00','18:00','DOCENTE')");
        new ConfiguracionReservasDAO(conn).guardar(120,1440,63,LocalTime.of(9,0),LocalTime.of(18,0),120);
        new ConfiguracionPagosDAO(conn).guardar("Banco","Equipo","Cuenta","Adjuntar",60);
        new ConfiguracionLiquidacionesDAO(conn).guardar(2,3);
    }
    @After public void cerrar()throws Exception {if(conn!=null)conn.close();}
    private void sql(String consulta)throws SQLException {try(Statement s=conn.createStatement()){s.execute(consulta);}}
    private Tutoria reservar(){
        Tutoria t=new Tutoria(1,"Alumno","","","2030-01-08","alumno@example.test","999999999","Algebra",1,"","Pendiente de pago");
        t.setCursoId(1);t.setTarifaId(1);t.setHoraInicio(LocalTime.of(10,0));t.setDuracionMinutos(60);t.setNivel("Secundaria");
        assertTrue(reservas().addTutoria(t));return t;
    }
    private void comprobante(Tutoria t,int version,String ref){
        assertTrue(pagos().registrar(t.getId(),1,Rol.ALUMNO,version,"50.00",ref,ahora,
            ("%PDF-1.4 comprobante "+ref).getBytes(StandardCharsets.US_ASCII),false,""));
    }
    private void validar(Tutoria t){assertTrue(pagos().gestionar(t.getId(),0,Rol.ADMIN,pagos().obtener(t.getId()).version(),"validar","Ingreso verificado en banco","50.00","",ahora,0));}
    private void accion(Tutoria t,int actor,Rol rol,String accion,String motivo,String valor,String fecha,String hora,int version){
        assertTrue(gestion().gestionar(t.getId(),actor,rol,accion,motivo,valor,fecha,hora,version));
    }
    private Tutoria confirmar(){Tutoria t=reservar();comprobante(t,0,"PAGO-01");validar(t);return t;}
    private int completarYLiquidar(Tutoria t){
        ahora=LocalDateTime.of(2030,1,8,14,0);accion(t,1,Rol.DOCENTE,"completar","Algebra y ejercicios","","","",0);
        ahora=LocalDateTime.of(2030,1,9,10,0);
        var vista=liquidaciones().preparar(1,"2030-01-08",0,Rol.ADMIN);
        assertEquals(new BigDecimal("40.00"),vista.total());
        int lote=liquidaciones().crear(1,"2030-01-08",vista.huella(),Rol.ADMIN);
        assertTrue(liquidaciones().gestionar(lote,1,Rol.ADMIN,"pagar","40.00","TRF-PROF-01","Cuenta del profesor",ahora,""));
        return lote;
    }
    @Test public void reservaCorreccionReprogramacionClaseYCierreCompleto()throws Exception {
        Tutoria t=reservar();comprobante(t,0,"PAGO-01");
        assertEquals("Pendiente de pago",reservas().getTutoriaById(t.getId()).getEstado());
        assertTrue(pagos().gestionar(t.getId(),0,Rol.ADMIN,1,"observar","Referencia ilegible","","",null,0));
        comprobante(t,2,"PAGO-02");validar(t);
        assertEquals(2,pagos().comprobantes(t.getId()).size());
        accion(t,1,Rol.DOCENTE,"enlace","","https://video.example.test/clase","","",0);
        accion(t,1,Rol.ALUMNO,"proponer","Cambiar horario","","2030-01-08","12:00",0);
        assertEquals(LocalTime.of(10,0),reservas().getTutoriaById(t.getId()).getHoraInicio());
        accion(t,1,Rol.DOCENTE,"aceptar_cambio","","","","",1);
        Tutoria nueva=reservas().getTutoriaById(t.getId());
        assertEquals(LocalTime.of(12,0),nueva.getHoraInicio());assertEquals("",nueva.getEnlaceClase());
        assertEquals(new BigDecimal("50.00"),nueva.getPrecio());assertEquals(new BigDecimal("80.00"),nueva.getPorcentajeDocente());
        accion(t,1,Rol.DOCENTE,"enlace","","https://video.example.test/nueva-clase","","",0);
        int lote=completarYLiquidar(t);
        assertEquals("Pagada",liquidaciones().obtener(lote,1,Rol.DOCENTE).estado());
        assertTrue(liquidaciones().preparar(1,"2030-01-08",0,Rol.ADMIN).lineas().isEmpty());
        var repetida=liquidaciones().preparar(1,"2030-01-08",0,Rol.ADMIN);
        assertThrows(IllegalArgumentException.class,()->liquidaciones().crear(1,"2030-01-08",repetida.huella(),Rol.ADMIN));
        assertNull(liquidaciones().obtener(lote,2,Rol.DOCENTE));
        assertFalse(gestion().historial(t.getId()).isEmpty());
    }
    @Test public void cancelacionPagadaRequiereRevisionYDevolucionSeparada(){
        Tutoria t=confirmar();accion(t,1,Rol.ALUMNO,"cancelar","No podré asistir","","","",0);
        assertEquals("Confirmada",reservas().getTutoriaById(t.getId()).getEstado());
        assertTrue(reservas().getTutoriaById(t.getId()).isCancelacionSolicitada());
        accion(t,0,Rol.ADMIN,"cancelar","Cancelación aprobada","","","",0);
        assertEquals("Validado",pagos().obtener(t.getId()).estado());
        assertTrue(pagos().gestionar(t.getId(),0,Rol.ADMIN,2,"aprobar_devolucion","Cancelación aprobada","50.00","",null,0));
        assertEquals("Devolución pendiente",pagos().obtener(t.getId()).estado());
        int devolucion=pagos().devoluciones(t.getId()).get(0).id();
        assertTrue(pagos().gestionar(t.getId(),0,Rol.ADMIN,3,"ejecutar_devolucion","","50.00","DEV-01",ahora,devolucion));
        assertEquals("Devuelto",pagos().obtener(t.getId()).estado());
        assertEquals("Cancelada",reservas().getTutoriaById(t.getId()).getEstado());
        assertEquals(new BigDecimal("50.00"),pagos().devuelto(t.getId()));
        assertThrows(IllegalArgumentException.class,()->pagos().gestionar(t.getId(),0,Rol.ADMIN,3,"ejecutar_devolucion","","50.00","DEV-01",ahora,devolucion));
    }
    @Test public void devolucionPosteriorNoReescribeTransferenciaNiDescuentaSinAcuerdo(){
        Tutoria t=confirmar();int lote=completarYLiquidar(t);
        assertTrue(pagos().gestionar(t.getId(),1,Rol.ALUMNO,2,"solicitar_devolucion","Revisión posterior","","",null,0));
        assertTrue(pagos().gestionar(t.getId(),0,Rol.ADMIN,3,"aprobar_devolucion","Devolución parcial acordada","25.00","",null,0));
        int devolucion=pagos().devoluciones(t.getId()).get(0).id();
        assertTrue(pagos().gestionar(t.getId(),0,Rol.ADMIN,4,"ejecutar_devolucion","","25.00","DEV-PARCIAL",ahora,devolucion));
        var anterior=liquidaciones().obtener(lote,0,Rol.ADMIN);
        assertEquals("Pagada",anterior.estado());assertEquals(new BigDecimal("40.00"),anterior.total());
        var ajuste=liquidaciones().ajustes(1,0,Rol.ADMIN).get(0);
        assertEquals("Pendiente",ajuste.estado());assertEquals(new BigDecimal("-20.00"),ajuste.sugerido());
        assertTrue(liquidaciones().gestionarAjuste(ajuste.id(),0,Rol.ADMIN,1,"proponer","-20.00","Reparto proporcional"));
        assertTrue(liquidaciones().gestionarAjuste(ajuste.id(),1,Rol.DOCENTE,2,"aceptar","","Acuerdo registrado"));
        ahora=LocalDateTime.of(2030,1,16,10,0);
        var futura=liquidaciones().preparar(1,"2030-01-15",0,Rol.ADMIN);
        assertEquals(new BigDecimal("-20.00"),futura.total());
        assertThrows(IllegalArgumentException.class,()->liquidaciones().crear(1,"2030-01-15",futura.huella(),Rol.ADMIN));
        assertEquals(new BigDecimal("40.00"),liquidaciones().obtener(lote,0,Rol.ADMIN).total());
    }
    @Test public void alumnoYProfesorAjenosNoAccedenAReservaNiComprobante(){
        Tutoria t=confirmar();int archivo=pagos().comprobantes(t.getId()).get(0).id();
        assertNull(gestion().obtener(t.getId(),2,Rol.ALUMNO));assertNull(gestion().obtener(t.getId(),2,Rol.DOCENTE));
        assertNull(pagos().descargar(archivo,2,Rol.ALUMNO));assertNull(pagos().descargar(archivo,1,Rol.DOCENTE));
        assertNotNull(pagos().descargar(archivo,1,Rol.ALUMNO));assertNotNull(pagos().descargar(archivo,0,Rol.ADMIN));
        assertFalse(gestion().gestionar(t.getId(),2,Rol.ALUMNO,"cancelar","Prueba","","","",0));
    }
    @Test public void correosConservanReservaSiProveedorFalla(){
        Tutoria t=reservar();var cola=new CorreoDAO(conn,"admin@example.test");
        var cfg=new ConfiguracionCorreo(true,"clave-falsa","equipo@example.test","TeachMate","admin@example.test","https://teachmate.example.test",250);
        new ProcesadorCorreos(cola,cfg,m->new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.TEMPORAL,"Fallo simulado",0),reloj()).procesar();
        assertEquals("Pendiente de pago",reservas().getTutoriaById(t.getId()).getEstado());
        comprobante(t,0,"PAGO-01");validar(t);
        assertEquals("Confirmada",reservas().getTutoriaById(t.getId()).getEstado());
        assertTrue(cola.listar().stream().anyMatch(m->m.estado().equals("Pendiente")));
    }
    @Test public void volverAlHorarioOriginalRecuperaRecordatoriosQueNuncaSeEnviaron(){
        Tutoria t=confirmar();var cola=new CorreoDAO(conn,"admin@example.test");
        var cfg=new ConfiguracionCorreo(true,"clave-falsa","equipo@example.test","TeachMate","admin@example.test","https://teachmate.example.test",250);
        ahora=LocalDateTime.of(2030,1,8,9,0);cola.recordatorios(ahora);
        accion(t,1,Rol.ALUMNO,"proponer","Mover media hora","","2030-01-08","10:30",0);
        accion(t,1,Rol.DOCENTE,"aceptar_cambio","","","","",1);
        List<CorreoDAO.Mensaje> enviados=new ArrayList<>();
        ProcesadorCorreos.Transporte transporte=m->{enviados.add(m);return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.ACEPTADO,"Simulado",0);};
        new ProcesadorCorreos(cola,cfg,transporte,reloj()).procesar();
        assertEquals(0,enviados.stream().filter(m->m.tipo().equals("Recordatorio clase")).count());
        ahora=ahora.plusMinutes(5);
        accion(t,1,Rol.ALUMNO,"proponer","Volver al horario original","","2030-01-08","10:00",0);
        accion(t,1,Rol.DOCENTE,"aceptar_cambio","","","","",2);
        new ProcesadorCorreos(cola,cfg,transporte,reloj()).procesar();
        assertEquals(2,enviados.stream().filter(m->m.tipo().equals("Recordatorio clase")).count());
        new ProcesadorCorreos(cola,cfg,transporte,reloj()).procesar();
        assertEquals(2,enviados.stream().filter(m->m.tipo().equals("Recordatorio clase")).count());
    }
}
