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

@WebServlet("/userChangePassword")
public class ChangePasswordServlet extends HttpServlet{

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		int userId = ((com.tme.modal.User) req.getSession(false).getAttribute("userObj")).getId();
		String oldPassword = req.getParameter("oldPassword");
		String newPassword = req.getParameter("newPassword");
		
		UserDAO uDAO = new UserDAO(DBConnection.getConn());
		//boolean f = uDAO.checkOldPassword(userId, oldPassword);
		
		
		HttpSession session = req.getSession();
		
		if(uDAO.checkOldPassword(userId, oldPassword)) {
			
			if(uDAO.changePassword(userId, newPassword)) {
				
				session = com.tme.security.Sessions.login(req, "userObj", session.getAttribute("userObj"));
session.setAttribute("successMsg", "Cambio de contraseña exitoso");
				resp.sendRedirect("change_password.jsp");
				
			}else {
				
				session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
				resp.sendRedirect("change_password.jsp");
				
			}
			
		}else {
			session.setAttribute("errorMsg", "La contraseña anterior no coincide.");
			resp.sendRedirect("change_password.jsp");
		}
		
		
		
	}
	
	

}
