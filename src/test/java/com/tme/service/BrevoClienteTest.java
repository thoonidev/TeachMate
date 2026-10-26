package com.tme.service;

import com.sun.net.httpserver.HttpServer;
import com.tme.dao.CorreoDAO;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Local HTTP stub only. This class never calls Brevo or sends real email. */
public class BrevoClienteTest {
    private HttpServer servidor;
    private BrevoCliente brevo;
    private final AtomicInteger codigo=new AtomicInteger(201);
    private final AtomicReference<String> respuesta=new AtomicReference<>("{\"messageId\":\"prueba\"}");
    private final AtomicReference<String> cuerpo=new AtomicReference<>();
    private final AtomicReference<String> clave=new AtomicReference<>();
    @Before public void preparar()throws Exception {
        servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        servidor.createContext("/email",intercambio->{
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            clave.set(intercambio.getRequestHeaders().getFirst("api-key"));
            intercambio.getResponseHeaders().set("Content-Type","application/json");
            intercambio.getResponseHeaders().set("Retry-After","120");
            byte[] bytes=respuesta.get().getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(codigo.get(),bytes.length);
            intercambio.getResponseBody().write(bytes);intercambio.close();
        });servidor.start();
        var config=new ConfiguracionCorreo(true,"clave-falsa-local","equipo@example.test","TeachMate","admin@example.test","https://teachmate.example.test",250);
        brevo=new BrevoCliente(config,HttpClient.newHttpClient(),URI.create("http://127.0.0.1:"+servidor.getAddress().getPort()+"/email"));
    }
    @After public void cerrar(){if(servidor!=null)servidor.stop(0);}
    private CorreoDAO.Mensaje mensaje(){
        LocalDateTime ahora=LocalDateTime.of(2030,1,7,10,0);
        return new CorreoDAO.Mensaje(1,7,"Reserva",null,"alumno@example.test","ALUMNO","Título \"de prueba\"","Línea 1\nLínea 2\\fin",
            UUID.randomUUID().toString(),"token",1,ahora,ahora.plusDays(1),false);
    }
    @Test public void enviaFormatoApiYEscapaTexto()throws Exception {
        var m=mensaje();assertEquals(ProcesadorCorreos.Tipo.ACEPTADO,brevo.enviar(m).tipo());
        assertEquals("clave-falsa-local",clave.get());
        assertTrue(cuerpo.get().contains("\"idempotencyKey\":\""+m.uuid()+"\""));
        assertTrue(cuerpo.get().contains("Título \\\"de prueba\\\""));
        assertTrue(cuerpo.get().contains("Línea 1\\nLínea 2\\\\fin"));
        assertTrue(cuerpo.get().contains("https://teachmate.example.test/asesoria.jsp?id=7"));
        assertFalse(cuerpo.get().contains("clave-falsa-local"));
    }
    @Test public void reconoceClaveYaAceptadaSinReenviarOtroCorreo()throws Exception {
        codigo.set(400);respuesta.set("{\"code\":\"duplicate_parameter\",\"message\":\"Duplicate\"}");
        assertEquals(ProcesadorCorreos.Tipo.ACEPTADO,brevo.enviar(mensaje()).tipo());
    }
    @Test public void clasificaCuotaErroresTemporalesYRechazos()throws Exception {
        codigo.set(429);var cuota=brevo.enviar(mensaje());assertEquals(ProcesadorCorreos.Tipo.CUOTA,cuota.tipo());assertEquals(120,cuota.esperaSegundos());
        codigo.set(503);assertEquals(ProcesadorCorreos.Tipo.TEMPORAL,brevo.enviar(mensaje()).tipo());
        codigo.set(401);assertEquals(ProcesadorCorreos.Tipo.PERMANENTE,brevo.enviar(mensaje()).tipo());
    }
    @Test public void noGuardaMensajesDelProveedorQuePuedanContenerDatosPrivados()throws Exception {
        codigo.set(400);respuesta.set("{\"message\":\"dato-privado-de-prueba\"}");
        assertFalse(brevo.enviar(mensaje()).detalle().contains("dato-privado"));
    }
}
