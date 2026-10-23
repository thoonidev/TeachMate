package com.tme.docente.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tme.dao.TutoriaDAO;
import com.tme.db.DBConnection;

@WebServlet("/updateComentario")
public class UpdateComentario extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try {
			
		 int 	id = Integer.parseInt(req.getParameter("id"));
		 int docenteId = ((com.tme.modal.Docente) req.getSession(false).getAttribute("docenteObj")).getId();
		 String comentario = req.getParameter("comentario");
		 
		 TutoriaDAO tutDAO = new TutoriaDAO(DBConnection.getConn());
		 boolean f = tutDAO.updateComentario(id, docenteId, comentario);
		 
		 HttpSession session = req.getSession();
		 
		 
		 if(f == true) {
			 session.setAttribute("successMsg", "Comentario actualizado");
			 resp.sendRedirect("docente/alumno.jsp");
			 
		 }else {
			 
			 session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
			 resp.sendRedirect("docente/alumno.jsp");
			 
		 }
		 
			
		} catch (Exception e) {
			req.getServletContext().log("No se pudo completar la operación solicitada."); resp.sendError(500);
		}
	}

	
	
}
