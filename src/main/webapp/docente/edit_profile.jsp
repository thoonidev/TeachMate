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
<title>Editar Perfil | Docente</title>

<%@include file="../component/allcss.jsp"%>

</head>
<body class="tm-page">
	<%@include file="../component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="../component/messages.jsp" %>

	<c:if test="${empty docenteObj }">

		<c:redirect url="../docente_login.jsp"></c:redirect>

	</c:if>

	<div class="container p-4">
        <h1 class="tm-page-title">Mi perfil</h1>
		<div class="row">
			<div class="col-md-4">
				<div class="card my-card">

					<div class="card-body">
						<h2 class="tm-section-title">Cambiar contraseña</h2>

						<form action="../docenteChangePassword" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-newPassword-1" class="form-label">Ingrese nueva clave</label> <input id="tm-newPassword-1"
									name="newPassword" maxlength="128" minlength="12" autocomplete="new-password" type="password"
									placeholder="Ingrese nueva clave" class="form-control"
									required="required">

							</div>
							<div class="mb-3">
								<label for="tm-oldPassword-2" class="form-label">Ingrese la contraseña anterior</label> <input id="tm-oldPassword-2"
									name="oldPassword" maxlength="128" autocomplete="current-password" type="password"
									placeholder="Ingrese la contraseña anterior" class="form-control" required>
							</div>
							<input type="hidden" value="${docenteObj.id}" name="docenteId">

							<button type="submit" class="btn btn-primary col-md-12">Cambiar
								Clave</button>
						</form>

					</div>
				</div>

			</div>

			<div class="col-md-6  offset-md-2">
				<div class="card my-card">

					<div class="card-body">
						<h2 class="tm-section-title">Datos del profesor</h2>

						<form action="../docenteEditProfile" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label for="tm-nombre-3" class="form-label">Nombre completo</label> <input id="tm-nombre-3"
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control" value="<c:out value="${docenteObj.nombre}" />">

							</div>
							<div class="mb-3">
								<label for="tm-fnacim-4" class="form-label">Fecha de nacimiento</label> <input id="tm-fnacim-4"
									name="fnacim" type="date" placeholder="Ingrese fnacim"
									class="form-control" value="<c:out value="${docenteObj.fnacim}" />">

							</div>
							<div class="mb-3">
								<label for="tm-calificacion-5" class="form-label">Formación</label> <input id="tm-calificacion-5"
									name="calificacion" type="text"
									placeholder="Ingrese calificacion" class="form-control"
									value="<c:out value="${docenteObj.calificacion}" />">
							</div>

							<div class="mb-3"><label for="presentacion" class="form-label">Presentación pública</label><textarea id="presentacion" name="presentacion" class="form-control" maxlength="1000" rows="4"><c:out value="${docenteObj.presentacion}" /></textarea></div><div class="mb-3"><label for="niveles" class="form-label">Niveles que atiende</label><input id="niveles" name="niveles" class="form-control" maxlength="150" placeholder="Por ejemplo: secundaria, preuniversitario" value="<c:out value="${docenteObj.niveles}" />"></div><p class="form-text">Los cursos se asignan desde la gestión de profesores.</p>

							<div class="mb-3">
								<label for="tm-email-7" class="form-label">Correo electrónico</label> <input id="tm-email-7"
									name="email" type="email" placeholder="Ingrese correo electronico"
									class="form-control" readonly value="<c:out value="${docenteObj.email}" />">

							</div>
							<div class="mb-3">
								<label for="tm-telefono-8" class="form-label">Teléfono</label> <input id="tm-telefono-8" name="telefono"
									type="text" placeholder="Ingrese telefono"
									class="form-control" value="<c:out value="${docenteObj.telefono}" />">

							</div>

							<input type="hidden" value="${docenteObj.id}" name="docenteId">

							<button type="submit"
								class="btn btn-primary text-white col-md-12">Actualizar</button>
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
