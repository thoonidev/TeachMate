package com.tme.security;

import com.tme.modal.Docente;
import com.tme.modal.User;
import java.lang.reflect.*;
import java.util.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise the actual role/CSRF filter without starting a server or touching the database. */
public class AccesoWebTest {
    private static final class Peticion {
        final Map<String,Object> sesion=new HashMap<>();
        final Map<String,String> parametros=new HashMap<>();
        final Map<String,String> cabeceras=new HashMap<>();
        final String ruta,metodo;int estado=200;boolean continuo;String redireccion;
        Peticion(String ruta,String metodo){this.ruta=ruta;this.metodo=metodo;sesion.put("csrfToken","token-de-prueba");}
        @SuppressWarnings("unchecked") <T> T proxy(Class<T> tipo,InvocationHandler manejador){return (T)Proxy.newProxyInstance(tipo.getClassLoader(),new Class<?>[]{tipo},manejador);}
        void ejecutar()throws Exception {
            HttpSession session=proxy(HttpSession.class,(p,m,a)->switch(m.getName()){
                case "getAttribute"->sesion.get(a[0]);
                case "setAttribute"->{sesion.put((String)a[0],a[1]);yield null;}
                default->null;
            });
            RequestDispatcher despacho=proxy(RequestDispatcher.class,(p,m,a)->null);
            HttpServletRequest request=proxy(HttpServletRequest.class,(p,m,a)->switch(m.getName()){
                case "getServletPath","getRequestURI"->ruta;
                case "getMethod"->metodo;
                case "getContextPath"->"/TeachMate";
                case "getSession"->session;
                case "getParameter"->parametros.get(a[0]);
                case "getRequestDispatcher"->despacho;
                default->null;
            });
            HttpServletResponse response=proxy(HttpServletResponse.class,(p,m,a)->{
                switch(m.getName()){
                    case "sendError","setStatus"->estado=(Integer)a[0];
                    case "sendRedirect"->{estado=302;redireccion=(String)a[0];}
                    case "setHeader"->cabeceras.put((String)a[0],(String)a[1]);
                }return null;
            });
            new SecurityFilter().doFilter(request,response,(q,r)->continuo=true);
        }
    }
    @Test public void anonimoDebeIniciarSesionParaVerCola()throws Exception {
        var p=new Peticion("/admin/correos.jsp","GET");p.ejecutar();
        assertEquals(302,p.estado);assertEquals("/TeachMate/admin_login.jsp",p.redireccion);assertFalse(p.continuo);
    }
    @Test public void alumnoYProfesorNoPuedenAdministrarAvisos()throws Exception {
        for(String rol:List.of("userObj","docenteObj")){
            var p=new Peticion("/admin/correos.jsp","GET");p.sesion.put(rol,rol.equals("userObj")?new User():new Docente());p.ejecutar();
            assertEquals(403,p.estado);assertFalse(p.continuo);
        }
    }
    @Test public void administradorSinTokenNoPuedeReintentar()throws Exception {
        var p=new Peticion("/gestionarCorreo","POST");p.sesion.put("adminObj",new User());p.ejecutar();
        assertEquals(403,p.estado);assertFalse(p.continuo);
    }
    @Test public void administradorConTokenPuedeLlegarAlControlador()throws Exception {
        var p=new Peticion("/gestionarCorreo","POST");p.sesion.put("adminObj",new User());p.parametros.put("csrfToken","token-de-prueba");p.ejecutar();
        assertTrue(p.continuo);assertEquals("no-store",p.cabeceras.get("Cache-Control"));
    }
    @Test public void profesorNoTieneAccesoAlComprobanteDelAlumno()throws Exception {
        var p=new Peticion("/comprobantePago","GET");p.sesion.put("docenteObj",new Docente());p.ejecutar();
        assertEquals(403,p.estado);assertFalse(p.continuo);
    }
    @Test public void metodosNoPermitidosYTokenDistintoSeBloquean()throws Exception {
        var p=new Peticion("/gestionarCorreo","DELETE");p.sesion.put("adminObj",new User());p.ejecutar();assertEquals(405,p.estado);
        var otro=new Peticion("/gestionarCorreo","POST");otro.sesion.put("adminObj",new User());otro.parametros.put("csrfToken","otro-token");otro.ejecutar();assertEquals(403,otro.estado);
    }
}
