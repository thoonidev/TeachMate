package com.tme.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.regex.Pattern;

/** HTTPS API, so no SMTP port or additional SDK is required. */
public final class BrevoCliente implements ProcesadorCorreos.Transporte {
    private final ConfiguracionCorreo configuracion;
    private final HttpClient cliente;
    private final URI endpoint;
    public BrevoCliente(ConfiguracionCorreo configuracion) {
        this(configuracion,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build(),URI.create("https://api.brevo.com/v3/smtp/email"));
    }
    BrevoCliente(ConfiguracionCorreo configuracion,HttpClient cliente,URI endpoint) {
        configuracion.validar();this.configuracion=configuracion;this.cliente=cliente;this.endpoint=endpoint;
    }
    @Override public ProcesadorCorreos.Resultado enviar(com.tme.dao.CorreoDAO.Mensaje mensaje)throws IOException,InterruptedException {
        String texto=mensaje.cuerpo()+"\n\nConsulta el estado actualizado e inicia sesión en TeachMate:\n"
            +configuracion.urlBase()+"/asesoria.jsp?id="+mensaje.tutoria();
        String json="{\"sender\":{\"name\":"+json(configuracion.nombre())+",\"email\":"+json(configuracion.remitente())
            +"},\"to\":[{\"email\":"+json(mensaje.destino())+"}],\"subject\":"+json(mensaje.asunto())
            +",\"textContent\":"+json(texto)+",\"headers\":{\"idempotencyKey\":"+json(mensaje.uuid())+"}}";
        HttpRequest solicitud=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(15))
            .header("api-key",configuracion.apiKey()).header("Accept","application/json")
            .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> respuesta=cliente.send(solicitud,HttpResponse.BodyHandlers.ofString());
        int codigo=respuesta.statusCode();
        if(codigo==201||(codigo==400&&Pattern.compile("\"code\"\\s*:\\s*\"duplicate_parameter\"").matcher(respuesta.body()).find()))
            return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.ACEPTADO,"Aceptado por Brevo",0);
        if(codigo==429)return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.CUOTA,"Límite temporal del proveedor (HTTP 429)",reintento(respuesta));
        if(codigo>=500)return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.TEMPORAL,"Fallo temporal del proveedor (HTTP "+codigo+")",0);
        return new ProcesadorCorreos.Resultado(ProcesadorCorreos.Tipo.PERMANENTE,"Solicitud rechazada por el proveedor (HTTP "+codigo+")",0);
    }
    private int reintento(HttpResponse<?> respuesta) {
        try { return Math.max(60,Math.min(86400,Integer.parseInt(respuesta.headers().firstValue("Retry-After").orElse("3600")))); }
        catch(NumberFormatException e){return 3600;}
    }
    static String json(String valor) {
        StringBuilder s=new StringBuilder("\"");
        for(char c:valor.toCharArray())switch(c){
            case '"'->s.append("\\\"");case '\\'->s.append("\\\\");case '\n'->s.append("\\n");
            case '\r'->s.append("\\r");case '\t'->s.append("\\t");
            default->{if(c<32)s.append(String.format("\\u%04x",(int)c));else s.append(c);}
        }
        return s.append('"').toString();
    }
}
