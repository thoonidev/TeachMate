<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Cambio | Clave</title>

<%@include file="component/allcss.jsp"%>

</head>
<body class="tm-auth">
	<%@include file="component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="component/messages.jsp" %>

	<c:if test="${empty userObj }">

		<c:redirect url="/alumno_login.jsp"></c:redirect>

	</c:if>

	<div class="container p-4">
		<div class="row">
			<div class="col-md-4 offset-md-4">
				<div class="card my-card">

					<div class="card-body">
						<h1 class="tm-page-title">Cambio de Clave</h1>

						<form action="userChangePassword" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-newPassword-1" class="form-label">Ingrese nueva clave</label> <input id="tm-newPassword-1"
									name="newPassword" maxlength="128" minlength="12" autocomplete="new-password" type="password" placeholder="Ingrese nueva clave"
									class="form-control" required="required">

							</div>
							<div class="mb-3">
								<label for="tm-oldPassword-2" class="form-label">Ingrese la contraseña anterior</label> <input id="tm-oldPassword-2"
									name="oldPassword" maxlength="128" autocomplete="current-password" type="password" placeholder="Ingrese la contraseña anterior"
									class="form-control" required>
							</div>
							<input type="hidden" value="${userObj.id}" name="userId">

							<button type="submit" class="btn btn-primary text-white col-md-12">Cambiar Clave</button>
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
