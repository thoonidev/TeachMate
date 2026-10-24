package com.tme.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		//obtener sesión significa obtener "adminObj" y eliminarlo, ¡cerrar sesión!
		HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
