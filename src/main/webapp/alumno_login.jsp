<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Acceso de alumnos | TeachMate</title>

<%@include file="component/allcss.jsp"%>

</head>
<body class="tm-auth">

	<%@include file="component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="component/messages.jsp" %>

	<div class="container p-5">
		<div class="row">
			<div class="col-md-4 offset-md-4">
				<div class="card my-card">
					<div class="card-header"><h1>Acceso de alumnos</h1></div>

					<div class="card-body">
<p class="tm-auth-note">Ingresa para solicitar y consultar tus asesorías.</p>

						<form action="userLogin" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-email-1" class="form-label">Correo electrónico</label> <input id="tm-email-1"
									name="email" type="email" autocomplete="username" maxlength="45" required placeholder="Ingrese correo electronico"
									class="form-control">

							</div>
							<div class="mb-3">
								<label for="tm-clave-2" class="form-label">Contraseña</label> <input id="tm-clave-2"
									name="clave" maxlength="128" autocomplete="current-password" type="password" required placeholder="Ingrese clave"
									class="form-control">
							</div>

							<button type="submit"
								class="btn btn-primary col-md-12 text-white">Iniciar sesión</button>
						</form>
						<br>¿No tienes una cuenta? <a href="reg_alumno.jsp"
							class="text-decoration-none">crea una</a>

					</div>

				</div>
			</div>
		</div>
	</div>

<nav class="tm-auth-switch" aria-label="Otros accesos"><a href="docente_login.jsp">Soy profesor</a><a href="admin_login.jsp">Administración</a></nav>
</main>
<%@ include file="component/footer.jsp" %>
</body>
</html>
