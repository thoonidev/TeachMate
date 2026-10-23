<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Crea tu cuenta | TeachMate</title>

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
					<div class="card-header"><h1>Crea tu cuenta</h1></div>
					<div class="card-body">
<p class="tm-auth-note">Empieza a organizar tus tutorías desde un solo lugar.</p>

						<form action="user_register" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-nombre-1" class="form-label">Nombre completo</label> <input id="tm-nombre-1"
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control">

							</div>
							<div class="mb-3">
								<label for="tm-email-2" class="form-label">Correo electrónico</label> <input id="tm-email-2"
									name="email" type="email" autocomplete="username" maxlength="45" required placeholder="Ingrese correo electronico"
									class="form-control">
								<div id="emailHelp" class="form-text">Nunca compartiremos
									tu email con cualquier otra persona.</div>
							</div>
							<div class="mb-3">
								<label for="tm-clave-3" class="form-label">Clave</label> <input id="tm-clave-3"
									name="clave" maxlength="128" minlength="12" autocomplete="new-password" type="password" required placeholder="Ingrese clave"
									class="form-control">
							</div>

							<button type="submit" class="btn btn-primary text-white col-md-12">Registrarse</button>
						</form>

					</div>
				</div>
			</div>
		</div>
	</div>

</main>
<%@ include file="component/footer.jsp" %>
</body>
</html>
