package com.tme.service;

import com.tme.dao.CorreoDAO;
import java.io.IOException;
import java.time.*;

/** Delivery happens outside business transactions. Unknown outcomes need explicit review. */
public final class ProcesadorCorreos {
    public enum Tipo { ACEPTADO, CUOTA, TEMPORAL, PERMANENTE }
    public record Resultado(Tipo tipo,String detalle,int esperaSegundos) { }
    @FunctionalInterface public interface Transporte {
        Resultado enviar(CorreoDAO.Mensaje mensaje)throws IOException,InterruptedException;
    }
    private final CorreoDAO cola;
    private final ConfiguracionCorreo configuracion;
    private final Transporte transporte;
    private final Clock clock;
    public ProcesadorCorreos(CorreoDAO cola,ConfiguracionCorreo configuracion,Transporte transporte,Clock clock){
        this.cola=cola;this.configuracion=configuracion;this.transporte=transporte;this.clock=clock;
    }
    private LocalDateTime ahora(){return LocalDateTime.ofInstant(clock.instant(),ReglasTutoria.ZONA);}
    public void procesar() {
        cola.mantener(ahora());
        if(!configuracion.habilitado())return;
        configuracion.validar();cola.recordatorios(ahora());
        for(int i=0;i<20&&cola.aceptadosHoy(ahora())<configuracion.limiteDiario();i++){
            CorreoDAO.Mensaje mensaje=cola.reclamar(ahora());if(mensaje==null)return;
            if(mensaje.incierto()&&!ahora().isBefore(mensaje.primero().plusMinutes(25))){
                cola.resolver(mensaje,"Dudoso",ahora(),"Ventana de reintento seguro vencida; verificar en Brevo",ahora());continue;
            }
            Resultado resultado;
            try { resultado=transporte.enviar(mensaje); }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();cola.resolver(mensaje,"Dudoso",ahora(),"Envío interrumpido; verificar en Brevo",ahora());return;
            }
            catch(IOException e){resultado=new Resultado(Tipo.TEMPORAL,"No se confirmó la respuesta del proveedor",0);}
            LocalDateTime ahora=ahora();
            switch(resultado.tipo()){
                case ACEPTADO->cola.resolver(mensaje,"Aceptado",ahora,"Aceptado por Brevo; entrega no verificada",ahora);
                case PERMANENTE->cola.resolver(mensaje,"Fallido",ahora,resultado.detalle(),ahora);
                case CUOTA->{
                    cola.resolver(mensaje,"Pendiente",ahora.plusSeconds(Math.max(60,resultado.esperaSegundos())),resultado.detalle(),ahora);return;
                }
                case TEMPORAL->{
                    LocalDateTime siguiente=ahora.plusMinutes(1L<<Math.min(mensaje.intentos()-1,4));
                    // Brevo deduplicates for 30 minutes; never automatically cross that window on an unknown outcome.
                    if(mensaje.intentos()>=5||siguiente.isAfter(mensaje.primero().plusMinutes(25)))
                        cola.resolver(mensaje,"Dudoso",ahora,"Sin confirmación tras reintentos; verificar en Brevo",ahora,true);
                    else cola.resolver(mensaje,"Pendiente",siguiente,resultado.detalle(),ahora,true);
                }
            }
        }
    }
}
