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
    }
    @Override public void contextDestroyed(ServletContextEvent event) {
        if(ejecutor!=null){
            ejecutor.shutdownNow();
            try { ejecutor.awaitTermination(5,TimeUnit.SECONDS); }
            catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }
}
