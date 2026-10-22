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

@WebServlet("/docenteChangePassword")
public class DocenteChangePassword extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int docenteId = ((com.tme.modal.Docente) req.getSession(false).getAttribute("docenteObj")).getId();
		String newPassword = req.getParameter("newPassword");
		String oldPassword = req.getParameter("oldPassword");

		DocenteDAO docenteDAO = new DocenteDAO(DBConnection.getConn());

		HttpSession session = req.getSession();

		if (docenteDAO.checkOldPassword(docenteId, oldPassword)) {

			if (docenteDAO.changePassword(docenteId, newPassword)) {
				
				session = com.tme.security.Sessions.login(req, "docenteObj", session.getAttribute("docenteObj"));
session.setAttribute("successMsg", "Cambio de contraseña exitoso");
				resp.sendRedirect("docente/edit_profile.jsp");

			} else {
				
				session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
				resp.sendRedirect("docente/edit_profile.jsp");

			}

		} else {
			session.setAttribute("errorMsg", "La contraseña anterior no coincide.");
			resp.sendRedirect("docente/edit_profile.jsp");

		}
	}

}
