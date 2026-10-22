package com.tme.security;

import com.tme.dao.TutoriaDAO;
import com.tme.db.DBConnection;
import com.tme.modal.Docente;
import com.tme.modal.Tutoria;
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
    private static final Set<String> ADMIN = Set.of("/addDocente", "/updateDocente", "/deleteDocente", "/addEspecialidad", "/adminLogout");
    private static final Set<String> DOCENTE = Set.of("/docenteEditProfile", "/docenteChangePassword", "/updateEstado", "/docenteLogout");
    private static final Set<String> USER = Set.of("/addTutoria", "/userChangePassword", "/userLogout", "/change_password.jsp", "/list_tutoria.jsp", "/reg_tutoria.jsp");
    private static final Set<String> AUTH = Set.of("/adminLogin", "/docenteLogin", "/userLogin", "/user_register");
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
        // Reject alternate path spellings instead of allowing them to bypass route checks.
        if (request.getRequestURI().contains(";") || request.getRequestURI().contains("%")) {
            response.sendError(400); return;
        }
        String role = requiredRole(path);
        HttpSession session = request.getSession(false);
        if (role != null && (session == null || session.getAttribute(role) == null)) {
            response.sendError(403, "Debe iniciar sesión con el rol autorizado."); return;
        }
        if (!Set.of("GET", "HEAD", "POST").contains(request.getMethod())) {
            response.sendError(405); return;
        }
        if (role != null || AUTH.contains(path)) response.setHeader("Cache-Control", "no-store");
        if (request.getMethod().equals("POST")) {
            String expected = session == null ? null : (String) session.getAttribute("csrfToken");
            if (!validToken(expected, request.getParameter("csrfToken"))) {
                response.sendError(403, "Formulario vencido o inválido. Recargue la página."); return;
            }
            if (AUTH.contains(path) && !allowAttempt(request.getRemoteAddr())) {
                response.setHeader("Retry-After", "300");
                response.sendError(429, "Demasiados intentos. Espere unos minutos."); return;
            }
            try { validate(request, path); }
            catch (IllegalArgumentException exception) {
                response.sendError(400, exception.getMessage()); return;
            }
        }
        if (path.equals("/docente/comentario.jsp") || path.equals("/admin/edit_docente.jsp") || path.equals("/updateEstado")) {
            int id;
            try { id = positiveId(request, "id"); }
            catch (IllegalArgumentException exception) { response.sendError(400); return; }
            if (path.equals("/docente/comentario.jsp") || path.equals("/updateEstado")) {
                Docente docente = (Docente) session.getAttribute("docenteObj");
                Tutoria tutoria = new TutoriaDAO(DBConnection.getConn()).getTutoriaById(id);
                if (tutoria == null || tutoria.getDocenteId() != docente.getId()) {
                    response.sendError(404); return;
                }
            } else if (new com.tme.dao.DocenteDAO(DBConnection.getConn()).getDocenteById(id) == null) {
                response.sendError(404); return;
            }
        }
        if (session == null && (path.endsWith(".jsp") || path.equals("/") || path.isEmpty())) session = request.getSession(true);
        if (session != null && session.getAttribute("csrfToken") == null) session.setAttribute("csrfToken", Sessions.token());
        chain.doFilter(request, response);
    }

    static String requiredRole(String path) {
        if (path.equals("/admin") || path.startsWith("/admin/") || ADMIN.contains(path)) return "adminObj";
        if (path.equals("/docente") || path.startsWith("/docente/") || DOCENTE.contains(path)) return "docenteObj";
        return USER.contains(path) ? "userObj" : null;
    }

    static boolean validToken(String expected, String supplied) {
        return expected != null && supplied != null && supplied.length() <= 128
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    static synchronized boolean allowAttempt(String address) {
        long now = System.currentTimeMillis();
        ATTEMPTS.entrySet().removeIf(entry -> now - entry.getValue().start >= 300_000);
        if (ATTEMPTS.size() >= 10_000 && !ATTEMPTS.containsKey(address)) return false;
        AttemptWindow window = ATTEMPTS.computeIfAbsent(address, key -> new AttemptWindow(now));
        return ++window.count <= 30;
    }

    private static final class AttemptWindow {
        private final long start;
        private int count;
        AttemptWindow(long start) { this.start = start; }
    }

    static int positiveId(HttpServletRequest request, String name) {
        try {
            int id = Integer.parseInt(request.getParameter(name));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Identificador inválido.");
    }

    private static void field(HttpServletRequest request, String name, int max) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank() || value.length() > max || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Revise los campos obligatorios y sus longitudes.");
        }
    }

    private static void password(HttpServletRequest request, String name) {
        if (!Passwords.acceptable(request.getParameter(name))) {
            throw new IllegalArgumentException("La contraseña debe tener entre 12 y 128 caracteres.");
        }
    }

    private static void validate(HttpServletRequest request, String path) {
        if (AUTH.contains(path) || Set.of("/addDocente", "/updateDocente", "/docenteEditProfile", "/addTutoria").contains(path)) {
            field(request, "email", 45);
            if (!request.getParameter("email").matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Correo inválido.");
        }
        if (path.endsWith("Login")) field(request, "clave", 128);
        if (path.equals("/user_register") || path.equals("/addDocente")) password(request, "clave");
        if (path.equals("/updateDocente")) {
            positiveId(request, "id");
            String password = request.getParameter("clave");
            if (password != null && !password.isEmpty()) password(request, "clave");
        }
        if (path.equals("/user_register") || Set.of("/addDocente", "/updateDocente", "/docenteEditProfile", "/addTutoria").contains(path)) field(request, "nombre", 45);
        if (Set.of("/addDocente", "/updateDocente", "/docenteEditProfile").contains(path)) {
            for (String name : Set.of("fnacim", "calificacion", "especialidad", "telefono")) field(request, name, 45);
            try { LocalDate.parse(request.getParameter("fnacim")); }
            catch (java.time.format.DateTimeParseException exception) { throw new IllegalArgumentException("Fecha inválida."); }
        }
        if (path.endsWith("ChangePassword")) {
            field(request, "oldPassword", 128);
            password(request, "newPassword");
        }
        if (path.equals("/deleteDocente")) positiveId(request, "id");
        if (path.equals("/addEspecialidad")) field(request, "especialidadNombre", 45);
        if (path.equals("/updateEstado")) { positiveId(request, "id"); field(request, "comentario", 500); }
        if (path.equals("/addTutoria")) {
            positiveId(request, "docenteNameSelect");
            for (String name : Set.of("genero", "edad", "ftutoria", "telefono", "experiencia")) field(request, name, 45);
            field(request, "direccion", 500);
            try {
                int age = Integer.parseInt(request.getParameter("edad"));
                if (age < 1 || age > 120) throw new IllegalArgumentException("Edad inválida.");
                LocalDate.parse(request.getParameter("ftutoria"));
            } catch (java.time.format.DateTimeParseException | NumberFormatException exception) {
                throw new IllegalArgumentException("Edad o fecha inválida.");
            }
        }
    }
}
