package com.tme.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tme.dao.UserDAO;
import com.tme.db.DBConnection;
import com.tme.modal.User;

@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String nombre = req.getParameter("nombre");
			String email = req.getParameter("email");
			String clave = req.getParameter("clave");

			// Verificar si los campos están vacíos
			if (nombre == null || nombre.isEmpty() || email == null || email.isEmpty() || clave == null
					|| clave.isEmpty()) {
				HttpSession session = req.getSession();
				session.setAttribute("errorMsg", "Por favor, complete todos los campos.");
				resp.sendRedirect("reg_alumno.jsp");
				return; // Detener la ejecución del método doPost
			}

			// Verificar si el usuario ya está registrado
			UserDAO userDAO = new UserDAO(DBConnection.getConn());
			if (userDAO.isUserRegistered(email)) {
				HttpSession session = req.getSession();
				session.setAttribute("errorMsg", "¡El usuario ya está registrado!");
				resp.sendRedirect("reg_alumno.jsp");
				return; // Detener la ejecución del método doPost
			}

			// Si los campos no están vacíos y el usuario no está registrado, proceder con el registro del usuario
			User user = new User(nombre, email, clave);
			boolean f = userDAO.userRegister(user);

			HttpSession session = req.getSession();
			if (f) {
				session.setAttribute("successMsg", "Registro exitoso");
				resp.sendRedirect("reg_alumno.jsp");
			} else {
				session.setAttribute("errorMsg", "¡Algo salió mal en el servidor!");
				resp.sendRedirect("reg_alumno.jsp");
			}
		} catch (Exception e) {
			req.getServletContext().log("No se pudo completar la operación solicitada."); resp.sendError(500);
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
