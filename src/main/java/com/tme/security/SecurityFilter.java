package com.tme.security;

import com.tme.db.DBConnection;
import com.tme.modal.Docente;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.*;
import javax.servlet.http.*;

public class SecurityFilter implements Filter {
    private static final Set<String> ADMIN = Set.of("/addDocente", "/updateDocente", "/deactivateDocente", "/reactivateDocente", "/guardarCurso", "/guardarTarifa", "/retirarTarifa", "/asignarCursos", "/adminDisponibilidad", "/adminLogout", "/guardarConfiguracionReservas", "/guardarConfiguracionPagos", "/gestionarLiquidacion");
    private static final Set<String> DOCENTE = Set.of("/docenteDisponibilidad", "/docenteEditProfile", "/docenteChangePassword", "/docenteLogout");
    private static final Set<String> USER = Set.of("/addTutoria", "/guardarPerfilAlumno", "/perfil.jsp", "/userChangePassword", "/userLogout", "/change_password.jsp", "/list_tutoria.jsp", "/mis_pagos.jsp");
    private static final Set<String> AUTH = Set.of("/adminLogin", "/docenteLogin", "/userLogin", "/user_register");
    private static final Set<String> PRIVATE_PAYMENTS = Set.of("/pago.jsp", "/gestionarPago", "/comprobantePago");
    private static final Map<String, AttemptWindow> ATTEMPTS = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) input;
        HttpServletResponse response = (HttpServletResponse) output;
        request.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        String path = request.getServletPath();
        // Alternate spellings must not bypass route checks.
        if (request.getRequestURI().contains(";") || request.getRequestURI().contains("%")) { response.sendError(400); return; }
        String role = requiredRole(path);
        HttpSession session = request.getSession(false);
        boolean payment = PRIVATE_PAYMENTS.contains(path);
        if(payment&&(session==null||(session.getAttribute("adminObj")==null&&session.getAttribute("userObj")==null))){response.setHeader("Cache-Control","no-store");response.sendError(403);return;}
        boolean liquidation=path.equals("/liquidacion.jsp")||path.equals("/gestionarAjuste");
        if(path.equals("/gestionarLiquidacion")&&(session==null||session.getAttribute("adminObj")==null)){response.setHeader("Cache-Control","no-store");response.sendError(403);return;}
        if(liquidation&&(session==null||(session.getAttribute("adminObj")==null&&session.getAttribute("docenteObj")==null))){response.setHeader("Cache-Control","no-store");response.sendError(403);return;}
        boolean shared=path.equals("/asesoria.jsp")||path.equals("/gestionarAsesoria")||payment||liquidation;
        if(shared&&(session==null||(session.getAttribute("adminObj")==null&&session.getAttribute("docenteObj")==null&&session.getAttribute("userObj")==null))) {
            response.setHeader("Cache-Control","no-store");
            if(request.getMethod().equals("GET")&&path.endsWith(".jsp"))response.sendRedirect(request.getContextPath()+"/alumno_login.jsp");
            else response.sendError(403);
            return;
        }
        if (("docenteObj".equals(role)||shared) && session != null && session.getAttribute("docenteObj") instanceof Docente docente
                && !new com.tme.dao.DocenteDAO(DBConnection.getConn()).isActiveDocente(docente.getId())) {
            session.invalidate();session=null;
            if(shared){response.sendError(403);return;}
        }
        if (role != null && (session == null || session.getAttribute(role) == null)) {
            response.setHeader("Cache-Control", "no-store");
            boolean authenticated = session != null && (session.getAttribute("userObj") != null || session.getAttribute("docenteObj") != null || session.getAttribute("adminObj") != null);
            String login = role.equals("adminObj") ? "/admin_login.jsp" : role.equals("docenteObj") ? "/docente_login.jsp" : "/alumno_login.jsp";
            boolean navigation = (request.getMethod().equals("GET") || request.getMethod().equals("HEAD")) && (path.endsWith(".jsp") || path.equals("/admin") || path.equals("/docente") || path.endsWith("/"));
            if (!authenticated && navigation) {
                request.getSession(true).setAttribute("errorMsg", "Inicia sesión para acceder a esta página.");response.sendRedirect(request.getContextPath()+login);
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                request.setAttribute("accessMessage", authenticated ? "Tu cuenta no tiene permiso para acceder a esta sección. Puedes volver al inicio o ingresar con la cuenta correspondiente." : "Para realizar esta acción, inicia sesión con la cuenta correspondiente.");
                request.setAttribute("accessLogin", login);request.getRequestDispatcher("/WEB-INF/access-denied.jsp").forward(request,response);
            }
            return;
        }
        if (!Set.of("GET", "HEAD", "POST").contains(request.getMethod())) {response.sendError(405);return;}
        if (role != null || shared || AUTH.contains(path) || path.equals("/reg_tutoria.jsp")) response.setHeader("Cache-Control", "no-store");
        if (request.getMethod().equals("POST")) {
            if(path.equals("/gestionarPago")&&request.getContentType()!=null&&request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")){
                if(request.getContentLengthLong()>6291456){response.sendError(413,"El comprobante supera el tamaño permitido de 5 MB.");return;}
                try{request.getParts();}
                catch(IllegalStateException e){response.sendError(413,"El comprobante supera el tamaño permitido de 5 MB.");return;}
                catch(ServletException e){response.sendError(400,"El archivo o formulario no es válido.");return;}
            }
            String expected=session==null?null:(String)session.getAttribute("csrfToken"),supplied;
            try{supplied=request.getParameter("csrfToken");}catch(IllegalStateException e){response.sendError(413,"El comprobante o formulario supera el tamaño permitido.");return;}
            if (!validToken(expected,supplied)) {response.sendError(403,"Formulario vencido o inválido. Recargue la página.");return;}
            if (AUTH.contains(path) && !allowAttempt(request.getRemoteAddr())) {response.setHeader("Retry-After","300");response.sendError(429,"Demasiados intentos. Espere unos minutos.");return;}
            try{validate(request,path);}catch(IllegalArgumentException e){response.sendError(400,e.getMessage());return;}
        }
        if(path.equals("/admin/edit_docente.jsp")) {
            int id;try{id=positiveId(request,"id");}catch(IllegalArgumentException e){response.sendError(400);return;}
            if(new com.tme.dao.DocenteDAO(DBConnection.getConn()).getDocenteById(id)==null){response.sendError(404);return;}
        }
        if(session==null&&(path.endsWith(".jsp")||path.equals("/")||path.isEmpty()))session=request.getSession(true);
        if(session!=null&&session.getAttribute("csrfToken")==null)session.setAttribute("csrfToken",Sessions.token());
        chain.doFilter(request,response);
    }
    static String requiredRole(String path){
        if(path.equals("/admin")||path.startsWith("/admin/")||ADMIN.contains(path))return "adminObj";
        if(path.equals("/docente")||path.startsWith("/docente/")||DOCENTE.contains(path))return "docenteObj";
        return USER.contains(path)?"userObj":null;
    }
    static boolean validToken(String expected,String supplied){return expected!=null&&supplied!=null&&supplied.length()<=128&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));}
    static synchronized boolean allowAttempt(String address){
        long now=System.currentTimeMillis();ATTEMPTS.entrySet().removeIf(e->now-e.getValue().start>=300_000);
        if(ATTEMPTS.size()>=10_000&&!ATTEMPTS.containsKey(address))return false;
        AttemptWindow window=ATTEMPTS.computeIfAbsent(address,key->new AttemptWindow(now));return ++window.count<=30;
    }
    private static final class AttemptWindow{private final long start;private int count;AttemptWindow(long start){this.start=start;}}
    static int positiveId(HttpServletRequest request,String name){try{int id=Integer.parseInt(request.getParameter(name));if(id>0)return id;}catch(NumberFormatException ignored){}throw new IllegalArgumentException("Identificador inválido.");}
    private static void field(HttpServletRequest request,String name,int max){String value=request.getParameter(name);if(value==null||value.isBlank()||value.length()>max||value.indexOf('\0')>=0)throw new IllegalArgumentException("Revise los campos obligatorios y sus longitudes.");}
    private static void password(HttpServletRequest request,String name){if(!Passwords.acceptable(request.getParameter(name)))throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");}
    private static void validate(HttpServletRequest request,String path){
        if(AUTH.contains(path)||Set.of("/addDocente","/updateDocente","/docenteEditProfile").contains(path)){
            field(request,"email",45);if(!request.getParameter("email").matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Correo inválido.");
        }
        if(path.endsWith("Login"))field(request,"clave",128);
        if(path.equals("/user_register")||path.equals("/addDocente"))password(request,"clave");
        if(path.equals("/updateDocente")){positiveId(request,"id");String value=request.getParameter("clave");if(value!=null&&!value.isEmpty())password(request,"clave");}
        if(path.equals("/user_register")||Set.of("/addDocente","/updateDocente","/docenteEditProfile").contains(path))field(request,"nombre",45);
        if(Set.of("/addDocente","/updateDocente","/docenteEditProfile").contains(path)){
            for(String name:Set.of("fnacim","calificacion","telefono"))field(request,name,45);
            try{LocalDate.parse(request.getParameter("fnacim"));}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Fecha inválida.");}
        }
        if(path.endsWith("ChangePassword")){field(request,"oldPassword",128);password(request,"newPassword");}
        if(path.equals("/deactivateDocente")||path.equals("/reactivateDocente"))positiveId(request,"id");
        if(path.equals("/addTutoria")){
            positiveId(request,"cursoId");positiveId(request,"tarifaId");positiveId(request,"docenteNameSelect");
            try{
                if(request.getParameter("horaInicio")==null||!request.getParameter("horaInicio").matches("[0-2][0-9]:[0-5][0-9]"))throw new IllegalArgumentException("Indique una hora válida.");
                java.time.LocalTime.parse(request.getParameter("horaInicio"));int duration=Integer.parseInt(request.getParameter("duracionMinutos"));if(duration<15||duration>180)throw new IllegalArgumentException("La duración debe ser de 15 a 180 minutos.");
            }catch(java.time.format.DateTimeParseException|NumberFormatException e){throw new IllegalArgumentException("Indique una hora y una duración válidas.");}
            field(request,"ftutoria",45);field(request,"experiencia",500);field(request,"nivel",150);
            try{LocalDate.parse(request.getParameter("ftutoria"));}catch(java.time.format.DateTimeParseException|NumberFormatException e){throw new IllegalArgumentException("Fecha inválida.");}
        }
    }
}
