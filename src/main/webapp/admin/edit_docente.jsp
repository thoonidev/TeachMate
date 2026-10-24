<%@page import="com.tme.modal.Docente"%>
<%@page import="com.tme.dao.DocenteDAO"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Doctor page</title>

<%@include file="../component/allcss.jsp"%>

</head>
<body class="tm-page">
	<%@include file="../component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="../component/messages.jsp" %>

	<div class="container-fluid p-3">
		<div class="row">
			<div class="col-12 col-lg-8 col-xl-6 mx-auto">
				<div class="card my-card">
					<div class="card-body">
						<h1 class="tm-page-title">Editar Profesor</h1>

						<%
						//obtenga docente id especifico con la ayuda de request.getParameter() 
						//que proviene de la URL para la edición del profesor (que paso a través de la URL cuando presiono editar docente)...
						int id = Integer.parseInt(request.getParameter("id")); //este "id" es el nombre del prefijo en la URL
						DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());
						Docente docente = docDAO.getDocenteById(id);//llame a getDocenteById(id) que regresa al profesor con una identificación específica.
						%>

						<form action="../updateDocente" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-nombre-1" class="form-label">Nombre completo</label> <input id="tm-nombre-1"
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control" value="<%=com.tme.security.Html.escape(docente.getNombre())%>">

							</div>
							<div class="mb-3">
								<label for="tm-fnacim-2" class="form-label">Fecha de nacimiento</label> <input id="tm-fnacim-2"
									name="fnacim" type="date" placeholder="Ingrese fnacim"
									class="form-control" value="<%=com.tme.security.Html.escape(docente.getFnacim())%>">

							</div>
							<div class="mb-3">
								<label for="tm-calificacion-3" class="form-label">Formación</label> <input id="tm-calificacion-3"
									name="calificacion" type="text"
									placeholder="Ingrese calificacion" class="form-control"
									value="<%=com.tme.security.Html.escape(docente.getCalificacion())%>">
							</div>

							<div class="mb-3"><label for="presentacion" class="form-label">Presentación pública</label><textarea id="presentacion" name="presentacion" class="form-control" maxlength="1000" rows="4"><%=com.tme.security.Html.escape(docente.getPresentacion())%></textarea></div><div class="mb-3"><label for="niveles" class="form-label">Niveles que atiende</label><input id="niveles" name="niveles" class="form-control" maxlength="150" placeholder="Por ejemplo: secundaria, preuniversitario" value="<%=com.tme.security.Html.escape(docente.getNiveles())%>"></div><p class="form-text">Los cursos se asignan desde la gestión de profesores.</p>

							<div class="mb-3">
								<label for="tm-email-5" class="form-label">Correo electrónico</label> <input id="tm-email-5"
									name="email" type="email" placeholder="Ingrese correo electronico"
									class="form-control" value="<%=com.tme.security.Html.escape(docente.getEmail())%>">

							</div>
							<div class="mb-3">
								<label for="tm-telefono-6" class="form-label">Teléfono</label> <input id="tm-telefono-6" name="telefono"
									type="text" placeholder="Ingrese telefono"
									class="form-control" value="<%=com.tme.security.Html.escape(docente.getTelefono())%>">

							</div>
							<div class="mb-3">
								<label for="tm-clave-7" class="form-label">Contraseña</label> <input id="tm-clave-7"
									name="clave" maxlength="128" minlength="12" autocomplete="new-password" type="password" placeholder="Dejar vacío para conservar la contraseña"
									class="form-control" value="">
							</div>

							<div class="mb-3">
								<input name="id" type="hidden" class="form-control"
									value="<%=com.tme.security.Html.escape(docente.getId())%>">
							</div>

							<button type="submit" class="btn btn-primary text-white col-md-12">Actualizar</button>
						</form>

					</div>

				</div>
			</div>

		</div>
	</div>

</main>
<%@ include file="../component/footer.jsp" %>
</body>
</html>
