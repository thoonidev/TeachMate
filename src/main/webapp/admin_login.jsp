<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Acceso de administración | TeachMate</title>

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
					<div class="card-header"><h1>Acceso de administración</h1></div>
					<div class="card-body">
<p class="tm-auth-note">Ingresa para gestionar profesores, cursos, asesorías y pagos.</p>

						<form action="adminLogin" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-email-1" class="form-label">Correo electrónico</label> <input id="tm-email-1" name="email"
									type="email" autocomplete="username" maxlength="45" required placeholder="Ingrese correo electronico"
									class="form-control">

							</div>
							<div class="mb-3">
								<label for="tm-clave-2" class="form-label">Contraseña</label> <input id="tm-clave-2"
									name="clave" maxlength="128" autocomplete="current-password" type="password" required placeholder="Ingrese clave"
									class="form-control">
							</div>

							<button type="submit" class="btn btn-primary text-white col-md-12">Iniciar
								sesión</button>
						</form>

					</div>
				</div>
			</div>
		</div>
	</div>

<nav class="tm-auth-switch" aria-label="Otros accesos"><a href="alumno_login.jsp">Soy alumno</a><a href="docente_login.jsp">Soy profesor</a></nav>
</main>
<%@ include file="component/footer.jsp" %>
</body>
</html>
