package com.tme.security;

import org.junit.Test;
import static org.junit.Assert.*;

public class CorreoSeguridadTest {
    @Test public void colaYAccionesSoloSonDeAdministracion(){
        assertEquals("adminObj",SecurityFilter.requiredRole("/admin/correos.jsp"));
        assertEquals("adminObj",SecurityFilter.requiredRole("/gestionarCorreo"));
    }
}
