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

@WebServlet("/deleteDocente")
public class DeleteDocenteServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//obtener id (que viene como valor de cadena) y convertirlo a int	
		int id = Integer.parseInt(req.getParameter("id"));
		
		DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());
		HttpSession session = req.getSession();
		
		boolean f = docDAO.deleteDocenteById(id);
		
		if(f==true) {
			session.setAttribute("successMsg", "Docente eliminado con éxito");
			resp.sendRedirect("admin/list_docente.jsp");
		}
		else {
			session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
			resp.sendRedirect("admin/list_docente.jsp");
		}
	}
	
	

}
