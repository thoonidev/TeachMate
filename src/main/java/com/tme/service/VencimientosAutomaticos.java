package com.tme.service;

import com.tme.dao.GestionAsesoriasDAO;
import com.tme.db.DBConnection;
import java.util.concurrent.*;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Catch up at startup, then process deadlines every minute while Tomcat is running. */
@WebListener
public final class VencimientosAutomaticos implements ServletContextListener {
    private ScheduledExecutorService ejecutor;
    private ScheduledExecutorService correos;
    private BrevoCliente clienteCorreo;
    @Override public void contextInitialized(ServletContextEvent event) {
        ServletContext contexto=event.getServletContext();
        ejecutor=Executors.newSingleThreadScheduledExecutor(tarea->{
            Thread hilo=new Thread(tarea,"teachmate-vencimientos");hilo.setDaemon(true);return hilo;
        });
        ejecutor.scheduleWithFixedDelay(()->{
            try { new GestionAsesoriasDAO(DBConnection.getConn()).expirar(); }
            catch(RuntimeException e){contexto.log("No se pudieron procesar los vencimientos; se reintentará en el siguiente ciclo.");}
            finally {
                try { DBConnection.closeCurrentConnection(); }
                catch(RuntimeException e){contexto.log("No se pudo cerrar la conexión de vencimientos.");}
            }
        },0,60,TimeUnit.SECONDS);
        correos=Executors.newSingleThreadScheduledExecutor(tarea->{
            Thread hilo=new Thread(tarea,"teachmate-correos");hilo.setDaemon(true);return hilo;
        });
        correos.scheduleWithFixedDelay(()->{
            try {
                ConfiguracionCorreo configuracion=ConfiguracionCorreo.entorno();
                if(configuracion.habilitado()&&clienteCorreo==null)clienteCorreo=new BrevoCliente(configuracion);
                var cola=new com.tme.dao.CorreoDAO(DBConnection.getConn());
                new ProcesadorCorreos(cola,configuracion,clienteCorreo,
                    java.time.Clock.system(ReglasTutoria.ZONA)).procesar();
            }catch(RuntimeException e){contexto.log("No se pudo procesar el correo; revisa la configuración y la cola de avisos.");}
            finally {
                try { DBConnection.closeCurrentConnection(); }
                catch(RuntimeException e){contexto.log("No se pudo cerrar la conexión de correos.");}
            }
        },5,60,TimeUnit.SECONDS);
    }
    @Override public void contextDestroyed(ServletContextEvent event) {
        if(correos!=null)correos.shutdownNow();
        if(ejecutor!=null){
            ejecutor.shutdownNow();
            try { ejecutor.awaitTermination(5,TimeUnit.SECONDS); }
            catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
        if(correos!=null)try{correos.awaitTermination(5,TimeUnit.SECONDS);}
        catch(InterruptedException e){Thread.currentThread().interrupt();}
        clienteCorreo=null;
    }
}
