package com.tme.admin.servlet;

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

@WebServlet("/updateDocente")
public class UpdateDocenteServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// obtener todos los datos que provienen de docente.jsp detalles del docente.
			String nombre = req.getParameter("nombre");
			String fnacim = req.getParameter("fnacim");
			String calificacion = req.getParameter("calificacion");
			String especialidad = null; // Course assignments are managed by the administrator.
			String email = req.getParameter("email");
			String telefono = req.getParameter("telefono");
			String clave = req.getParameter("clave");
			
			//Aquí también necesito obtener una identificación... para actualizar los detalles del docente.
			//los docentes se actualizarán según el id docente respectivo.
			int id = Integer.parseInt(req.getParameter("id"));

			Docente docente = new Docente(id, nombre, fnacim, calificacion, especialidad, email, telefono, clave);

			docente.setPresentacion(com.tme.dao.OfertaDAO.opcional(req.getParameter("presentacion"), 1000));
            docente.setNiveles(com.tme.dao.OfertaDAO.opcional(req.getParameter("niveles"), 150));
            DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());

			boolean f = docDAO.updateDocente(docente);

			HttpSession session = req.getSession();

			if (f == true) {
				session.setAttribute("successMsg", "Docente actualizado con éxito");
				resp.sendRedirect("admin/list_docente.jsp");

			} else {
				session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
				resp.sendRedirect("admin/list_docente.jsp");
			}

		} catch (IllegalArgumentException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/list_docente.jsp");
        } catch (Exception e) {
			req.getServletContext().log("No se pudo completar la operación solicitada."); resp.sendError(500);
		}

	}

}
