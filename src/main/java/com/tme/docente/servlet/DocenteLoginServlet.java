package com.tme.docente.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.tme.dao.DocenteDAO;
import com.tme.dao.UserDAO;
import com.tme.db.DBConnection;
import com.tme.modal.Docente;


@WebServlet("/docenteLogin")
public class DocenteLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		//obtenga el correo electrónico y la contraseña que provienen de la página docente_login.jsp
		String email = req.getParameter("email");
		String clave = req.getParameter("clave");

		//create session
		HttpSession session = req.getSession();

		//create DB connection
		DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());
		
		//llame al método loginDocente() para iniciar sesión como docente, cuyo método se declaró en DocenteDAO
		Docente docente = docDAO.loginDocente(email, clave);

		if (docente != null) {
			//Significa que el docente es válido o existe
			//luego almacena el objeto docente registrado particular en la sesión
			session = com.tme.security.Sessions.login(req, "docenteObj", docente);
			// redirigir la página de índice del docente particular que es la carpeta del docente residente.
			resp.sendRedirect("docente/index.jsp");//índice de docente significa panel docente.
		} else {
			session.setAttribute("errorMsg", "Usuario o contraseña inválido.");
			resp.sendRedirect("docente_login.jsp");
		}

	}

}
