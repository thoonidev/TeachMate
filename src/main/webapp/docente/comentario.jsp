<%@page import="com.tme.modal.Tutoria"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.TutoriaDAO"%>
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
<title>Comentario | Alumno</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<%@include file="../component/allcss.jsp"%>


<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(137, 184, 255);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}

/* backgournd image css */
.my-bg-img {
	background: linear-gradient(rgba(0, 0, 0, .4), rgba(0, 0, 0, .4)),
		url("../img/hospital1.jpg");
	height: 20vh;
	width: 100%;
	background-size: cover;
	background-repeat: no-repeat;
}
</style>
<!-- Fin del CSS personalizado para esta página. -->

</head>
<body>

	<%@include file="navbar.jsp"%>

	<c:if test="${empty docenteObj }">

		<c:redirect url="../docente_login.jsp"></c:redirect>

	</c:if>

	<!-- Compruebe si el docente está conectado o no -->


	<!-- start 1st Div -->

	<div class="container-fluid my-bg-img p-5">
		<!-- css background image -->
		<p class="text-center fs-2 text-white"></p>

	</div>

	<!-- end of 1st Div -->


	<!-- 2nd Div -->

	<div class="container p-3">
		<p class="fs-2"></p>

		<div class="row">



			<!-- col-2 -->
			<div class="col-md-6 offset-md-3">
				<div class="card my-card">
					<div class="card-body">
						<p class="text-center fs-3">Comentario / Recomendación</p>

						

						<!-- message print -->
						<!-- for success msg -->
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
						<!-- End of message print -->


						<%
						/* obtener o recibir la identificación de la tutoria que llega a través de la URL (de la página alumno.jsp) */
						int id = Integer.parseInt(request.getParameter("id"));
						TutoriaDAO tutDAO = new TutoriaDAO(DBConnection.getConn());
						Tutoria tutoria = tutDAO.getTutoriaById(id);
						
						%>
						
						
						
						<!-- boostrap form -->
						<form class="row g-3" action="../updateEstado" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

							<!-- tomar ID de usuario en el campo oculto -->
							<%-- <input type="hidden" name="userId" value="${ userObj.id }"> --%>

							<div class="col-md-6">
								<label class="form-label">Nombre completo</label> <input
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control" readonly value="<%=com.tme.security.Html.escape(tutoria.getNombre())%>">

							</div>

							<div class="col-md-6">
								<label class="form-label">Edad</label> <input name="edad"
									type="number" placeholder="Ingrese su edad" class="form-control"
									readonly value="<%=com.tme.security.Html.escape(tutoria.getEdad())%>">
							</div>

							<div class="col-md-6">
								<label class="form-label">Telefono</label> <input name="telefono"
									type="number" maxlength="11" placeholder="Ingrese telefono"
									class="form-control" readonly value="<%=com.tme.security.Html.escape(tutoria.getTelefono())%>">
							</div>

							<div class="col-md-6">
								<label class="form-label">Experiencia</label> <input
									name="experiencia" type="text" placeholder="Ingrese conocimiento previo"
									class="form-control" readonly value="<%=com.tme.security.Html.escape(tutoria.getExperiencia())%>">
							</div>



							<div class="col-md-12">
								<label class="form-label">Comentario</label>
								<textarea name="comentario" placeholder="Ingrese comentario"
									class="form-control" rows="" cols=""></textarea>
							</div>

							<!-- comentario de la tutoria es / ID del comentario -->
							<input type="hidden" name="id" value="<%=com.tme.security.Html.escape(tutoria.getId())%>" class="form-control">

							<!-- docente da un comentario / ID del docente -->
							<input type="hidden" name="docenteId" value="<%=com.tme.security.Html.escape(tutoria.getDocenteId())%>" class="form-control">



							<div class="col-md-12">
								<button type="submit" class="btn btn-primary col-md-12">Enviar</button>
							</div>


						</form>

						<!-- end of boostrap form -->

					</div>
				</div>

			</div>

		</div>


	</div>

	<!-- 2nd Div -->

</body>
</html>