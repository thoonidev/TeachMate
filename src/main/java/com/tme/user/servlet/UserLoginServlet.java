package com.tme.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tme.dao.UserDAO;
import com.tme.db.DBConnection;
import com.tme.modal.User;

@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String email = req.getParameter("email");
		String clave = req.getParameter("clave");
		
		HttpSession session = req.getSession();
		
		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, clave);
		
		if (user!=null) {
			session = com.tme.security.Sessions.login(req, "userObj", user);
			resp.sendRedirect("list_tutoria.jsp");
		}
		else {
			session.setAttribute("errorMsg","Usuario o contraseña inválido.");
			resp.sendRedirect("alumno_login.jsp"); 
		}
	}
	
	
}
