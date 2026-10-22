<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.modal.Especialidad"%>
<%@page import="com.tme.dao.EspecialidadDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!-- for jstl tag -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!-- end of jstl tag -->

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Editar Perfil | Docente</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<%@include file="../component/allcss.jsp"%>

<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(137, 184, 255);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}
</style>
<!-- Fin del CSS personalizado para esta página. -->


</head>
<body>
	<%@include file="navbar.jsp"%>

	<!-- si "docenteObj" está vacío significa que nadie ha iniciado sesión. -->

	<c:if test="${empty docenteObj }">

		<c:redirect url="../docente_login.jsp"></c:redirect>

	</c:if>

	<!-- Comprobar si el docente está conectado o no -->

	<div class="container p-4">
		<div class="row">
			<div class="col-md-4">
				<div class="card my-card">

					<div class="card-body">
						<p class="fs-3 text-center text-primary">Cambio de Clave</p>

						<!-- for message -->
						<!-- success message -->
						<c:if test="${not empty successMsg}">
							<span hidden id="flash-successMsg"><c:out value="${successMsg}" /></span>
<script>
								Swal.fire({
									icon : 'success',
									title : 'Éxito',
									text : document.getElementById("flash-successMsg").textContent,
								});
							</script>
							<c:remove var="successMsg" scope="session" />
						</c:if>

						<!-- for error msg -->
						<c:if test="${not empty errorMsg}">
							<span hidden id="flash-errorMsg"><c:out value="${errorMsg}" /></span>
<script>
								Swal.fire({
									icon : 'error',
									title : 'Error',
									text : document.getElementById("flash-errorMsg").textContent,
								});
							</script>
							<c:remove var="errorMsg" scope="session" />
						</c:if>
						<!-- end of message -->

						<!-- formulario de cambio de contraseña del docente -->

						<form action="../docenteChangePassword" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label class="form-label">Ingrese nueva clave</label> <input
									name="newPassword" maxlength="128" minlength="12" autocomplete="new-password" type="password"
									placeholder="Ingrese nueva clave" class="form-control"
									required="required">

							</div>
							<div class="mb-3">
								<label class="form-label">Ingrese la contraseña anterior</label> <input
									name="oldPassword" maxlength="128" autocomplete="current-password" type="password"
									placeholder="Ingrese la contraseña anterior" class="form-control" required>
							</div>
							<input type="hidden" value="${docenteObj.id}" name="docenteId">

							<button type="submit" class="btn btn-primary col-md-12">Cambiar
								Clave</button>
						</form>
						<!-- formulario de cambio de contraseña del docente -->
					</div>
				</div>

			</div>

			<!-- Editar docente -->

			<div class="col-md-6  offset-md-2">
				<div class="card my-card">

					<div class="card-body">
						<p class="fs-3 text-center text-primary">Editar Perfil Docente</p>

						<!-- for message -->
						<!-- success message -->
						<c:if test="${not empty successMsgForD}">
							<span hidden id="flash-successMsgForD"><c:out value="${successMsgForD}" /></span>
<script>
							 Swal.fire({
						icon: 'success',
						title: 'Éxito',
						text: document.getElementById("flash-successMsgForD").textContent,
								 });
						</script>
							<c:remove var="successMsgForD" scope="session" />
						</c:if>
						
						<!-- for error msg -->
						<c:if test="${not empty errorMsgForD}">
							<span hidden id="flash-errorMsgForD"><c:out value="${errorMsgForD}" /></span>
<script>
						Swal.fire({
					            icon: 'error',
					            title: 'Error',
					            text: document.getElementById("flash-errorMsgForD").textContent,
							 });
							 </script>
							<c:remove var="errorMsgForD" scope="session" />
						</c:if>
						<!-- end of message -->





						<!-- boostrap form -->
						<form action="../docenteEditProfile" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label class="form-label">Nombre completo</label> <input
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control" value="<c:out value="${docenteObj.nombre}" />">

							</div>
							<div class="mb-3">
								<label class="form-label">Fecha de nacimiento</label> <input
									name="fnacim" type="date" placeholder="Ingrese fnacim"
									class="form-control" value="<c:out value="${docenteObj.fnacim}" />">

							</div>
							<div class="mb-3">
								<label class="form-label">Calificación</label> <input
									name="calificacion" type="text"
									placeholder="Ingrese calificacion" class="form-control"
									value="<c:out value="${docenteObj.calificacion}" />">
							</div>

							<div class="mb-3">
								<label class="form-label">Especialidad</label> <select
									class="form-control" name="especialidad">
									<option><c:out value="${docenteObj.especialidad}" /></option>

									<%
									EspecialidadDAO espDAO = new EspecialidadDAO(DBConnection.getConn());
										List<Especialidad> espList = espDAO.getAllEspecialidad();
										for (Especialidad esp : espList) {
										%>
									<option>
										<%=com.tme.security.Html.escape(esp.getEspecialidadNombre())%>
									</option>
									<%
										}
										%>

								</select>
							</div>

							<div class="mb-3">
								<label class="form-label">Email</label> <input
									name="email" type="email" placeholder="Ingrese correo electronico"
									class="form-control" readonly value="<c:out value="${docenteObj.email}" />">

							</div>
							<div class="mb-3">
								<label class="form-label">Telefono</label> <input name="telefono"
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

			<!-- Editar docente -->

		</div>
	</div>

</body>
</html>