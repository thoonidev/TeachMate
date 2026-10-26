package com.tme.service;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfiguracionCorreoTest {
    private Map<String,String> valores(){return new HashMap<>(Map.of("EMAIL_ENABLED","true","BREVO_API_KEY","secreto-de-prueba",
        "EMAIL_FROM","equipo@example.test","ADMIN_EMAIL","admin@example.test","APP_BASE_URL","https://teachmate.example.test/"));}
    @Test public void porDefectoNoEnvia(){assertFalse(ConfiguracionCorreo.desde(Map.of()).habilitado());}
    @Test public void validaConfiguracionCompleta(){assertEquals("https://teachmate.example.test",ConfiguracionCorreo.desde(valores()).urlBase());}
    @Test public void noRevelaCredencialesEnTexto(){assertFalse(ConfiguracionCorreo.desde(valores()).toString().contains("secreto-de-prueba"));}
    @Test public void requiereRemitenteYClaveAlActivar(){var v=valores();v.remove("BREVO_API_KEY");assertThrows(IllegalArgumentException.class,()->ConfiguracionCorreo.desde(v));}
    @Test public void rechazaUrlConCredencialesOParametros(){
        var v=valores();v.put("APP_BASE_URL","https://usuario:clave@example.test");assertThrows(IllegalArgumentException.class,()->ConfiguracionCorreo.desde(v));
        v.put("APP_BASE_URL","https://example.test?clave=123");assertThrows(IllegalArgumentException.class,()->ConfiguracionCorreo.desde(v));
    }
    @Test public void httpSoloParaPruebasLocales(){
        var v=valores();v.put("APP_BASE_URL","http://example.test");assertThrows(IllegalArgumentException.class,()->ConfiguracionCorreo.desde(v));
        v.put("APP_BASE_URL","http://localhost:8080/TeachMate/");assertEquals("http://localhost:8080/TeachMate",ConfiguracionCorreo.desde(v).urlBase());
    }
    @Test public void rechazaActivacionAmbiguaYLimitesInvalidos(){
        var activoInvalido=valores();activoInvalido.put("EMAIL_ENABLED","yes");assertThrows(IllegalArgumentException.class,()->ConfiguracionCorreo.desde(activoInvalido));
        var limiteInvalido=valores();limiteInvalido.put("EMAIL_DAILY_LIMIT","0");assertThrows(IllegalArgumentException.class,()->ConfiguracionCorreo.desde(limiteInvalido));
    }
}
