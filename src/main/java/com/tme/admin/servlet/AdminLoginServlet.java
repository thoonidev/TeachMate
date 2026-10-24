package com.tme.admin.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import com.tme.modal.User;
import com.tme.security.Passwords;
import com.tme.security.Sessions;

@WebServlet("/adminLogin")
public class AdminLoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String configuredEmail = System.getenv("ADMIN_EMAIL");
        String configuredHash = System.getenv("ADMIN_PASSWORD_HASH");
        if (configuredEmail == null || configuredEmail.isBlank() || configuredHash == null || configuredHash.isBlank()) {
            resp.sendError(503, "Acceso de administración pendiente de configuración.");
            return;
        }
        boolean passwordMatches = Passwords.verify(req.getParameter("clave"), configuredHash);
        if (configuredEmail.equals(req.getParameter("email")) && passwordMatches) {
            Sessions.login(req, "adminObj", new User());
            resp.sendRedirect("admin/index.jsp");
        } else {
            req.getSession().setAttribute("errorMsg", "Usuario o contraseña inválido.");
            resp.sendRedirect("admin_login.jsp");
        }
    }
}
