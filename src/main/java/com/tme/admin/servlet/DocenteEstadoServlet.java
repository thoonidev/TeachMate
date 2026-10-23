package com.tme.admin.servlet;

import com.tme.dao.DocenteDAO;
import com.tme.db.DBConnection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet({"/deactivateDocente", "/reactivateDocente"})
public class DocenteEstadoServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DocenteDAO dao = new DocenteDAO(DBConnection.getConn());
        int id = Integer.parseInt(request.getParameter("id"));
        boolean activar = request.getServletPath().equals("/reactivateDocente");
        boolean cambiado = activar ? dao.reactivateDocenteById(id) : dao.deactivateDocenteById(id);
        request.getSession(false).setAttribute(cambiado ? "successMsg" : "errorMsg", cambiado
                ? (activar ? "Docente reactivado; ya puede recibir nuevas solicitudes."
                           : "Docente desactivado; se conserva su historial. Revisa sus tutorías pendientes o aceptadas.")
                : "El docente no existe o su estado ya fue actualizado.");
        response.sendRedirect(request.getContextPath() + "/admin/list_docente.jsp");
    }
}
