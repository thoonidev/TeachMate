package com.tme.docente.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tme.dao.DocenteDAO;
import com.tme.db.DBConnection;
import com.tme.modal.Docente;

@WebServlet("/docenteEditProfile")
public class DocenteEditProfileServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// obtener todos los datos que provienen de docente.jsp detalles del docente
			String nombre = req.getParameter("nombre");
			String fnacim = req.getParameter("fnacim");
			String calificacion = req.getParameter("calificacion");
			String especialidad = req.getParameter("especialidad");
			String email = req.getParameter("email");
			String telefono = req.getParameter("telefono");
			//String clave = req.getParameter("clave");

			
			int id = ((com.tme.modal.Docente) req.getSession(false).getAttribute("docenteObj")).getId();

			Docente docente = new Docente(id, nombre, fnacim, calificacion, especialidad, email, telefono, "");

			DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());

			boolean f = docDAO.editDocenteProfile(docente);

			HttpSession session = req.getSession();

			if (f == true) {
				Docente updateDocenteObj = docDAO.getDocenteById(id);
				session.setAttribute("successMsgForD", "Docente actualizado con éxito");
				session.setAttribute("docenteObj", updateDocenteObj); // anular o actualizar el valor de la sesión anterior al nuevo valor del docente actualizado.
				resp.sendRedirect("docente/edit_profile.jsp");

			} else {
				session.setAttribute("errorMsgForD", "¡Algo salió mal en el servidor!");
				resp.sendRedirect("docente/edit_profile.jsp");
			}

		} catch (Exception e) {
			req.getServletContext().log("No se pudo completar la operación solicitada."); resp.sendError(500);
		}

	}

}
