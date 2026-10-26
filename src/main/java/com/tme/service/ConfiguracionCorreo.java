package com.tme.service;

import java.net.URI;
import java.util.Map;

/** Credentials are environment-only; never exposed through the administration UI. */
public record ConfiguracionCorreo(boolean habilitado,String apiKey,String remitente,String nombre,
        String administrador,String urlBase,int limiteDiario) {
    public static ConfiguracionCorreo entorno() { return desde(System.getenv()); }
    public static ConfiguracionCorreo desde(Map<String,String> valores) {
        String activo=valores.getOrDefault("EMAIL_ENABLED","false");
        if(!activo.equals("true")&&!activo.equals("false"))throw new IllegalArgumentException("EMAIL_ENABLED debe ser true o false.");
        int limite;
        try { limite=Integer.parseInt(valores.getOrDefault("EMAIL_DAILY_LIMIT","250")); }
        catch(NumberFormatException e){throw new IllegalArgumentException("EMAIL_DAILY_LIMIT debe ser un entero.");}
        ConfiguracionCorreo c=new ConfiguracionCorreo(Boolean.parseBoolean(activo),valor(valores,"BREVO_API_KEY"),
            valor(valores,"EMAIL_FROM"),valores.getOrDefault("EMAIL_FROM_NAME","TeachMate"),
            valor(valores,"ADMIN_EMAIL"),valor(valores,"APP_BASE_URL").replaceAll("/+$",""),limite);
        if(c.habilitado())c.validar();
        return c;
    }
    private static String valor(Map<String,String> valores,String clave){return valores.getOrDefault(clave,"").strip();}
    public static boolean emailValido(String valor) {
        return valor!=null&&valor.length()<=254&&valor.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+");
    }
    public void validar() {
        if(apiKey==null||apiKey.isBlank()||apiKey.contains("\r")||apiKey.contains("\n")
                ||!emailValido(remitente)||!emailValido(administrador)||nombre==null||nombre.isBlank()
                ||nombre.length()>100||limiteDiario<1||limiteDiario>10000)
            throw new IllegalArgumentException("Completa la configuración de correos y el límite diario.");
        URI uri;
        try { uri=URI.create(urlBase); }
        catch(RuntimeException e){throw new IllegalArgumentException("APP_BASE_URL debe ser la URL pública de TeachMate.");}
        boolean local="http".equals(uri.getScheme())&&("localhost".equals(uri.getHost())||"127.0.0.1".equals(uri.getHost()));
        if((!"https".equals(uri.getScheme())&&!local)||uri.getHost()==null||uri.getUserInfo()!=null
                ||uri.getQuery()!=null||uri.getFragment()!=null)
            throw new IllegalArgumentException("APP_BASE_URL debe usar HTTPS, sin credenciales ni parámetros (HTTP solo en localhost).");
    }
    @Override public String toString(){return "ConfiguracionCorreo[habilitado="+habilitado+", credenciales ocultas]";}
}
