package com.tme.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tme.dao.TutoriaDAO;
import com.tme.db.DBConnection;
import com.tme.modal.Tutoria;

@WebServlet("/addTutoria")
public class TutoriaServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
	int userId = ((com.tme.modal.User) req.getSession(false).getAttribute("userObj")).getId();
	String nombre = req.getParameter("nombre");
	String genero = req.getParameter("genero");
	String edad = req.getParameter("edad");
	String ftutoria = req.getParameter("ftutoria");
	String email = req.getParameter("email");
	String telefono = req.getParameter("telefono");
	String experiencia = req.getParameter("experiencia");
	int docenteId = Integer.parseInt(req.getParameter("docenteNameSelect"));
	String direccion = req.getParameter("direccion");
	
	
	Tutoria tutoria = new Tutoria(userId, nombre, genero, edad, ftutoria, email, telefono, experiencia, docenteId, direccion, "Pendiente");
	
	TutoriaDAO tutoriaDAO = new TutoriaDAO(DBConnection.getConn());
	boolean f = tutoriaDAO.addTutoria(tutoria);
	
	//get session
	HttpSession session = req.getSession();
	
	if(f==true) {
		
		session.setAttribute("successMsg", "Tutoría registrada con éxito");
		resp.sendRedirect("reg_tutoria.jsp");
		
		
	}
	else {
		
		session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
		resp.sendRedirect("reg_tutoria.jsp");
		
	}
	
	
	
	
		
	}

	
}
