<%@page import="com.tme.modal.Docente"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.DocenteDAO"%>
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
<title>Tutoria</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<!-- all css include -->
<%@include file="../component/allcss.jsp"%>

<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(173, 216, 230, 1);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}

/* backgournd image css */

/*.my-bg-img{
	background: linear-gradient(rgba(0, 0, 0, .4), rgba(0, 0, 0, .4)),url("img/img01.jpg");
	height: 20vh;
	width: 100%;
	background-size:cover;
	background-repeat: no-repeat;
	
}*/

/* backgournd image css */
</style>
<!-- Fin del CSS personalizado para esta página. -->

</head>
<body>
	<%@include file="component/navbar.jsp"%>

	<!-- Inicio 1st Div -->

	<div class="container-fluid my-bg-img p-5">
		<!-- css background image -->
		<p class="text-center fs-2 text-white"></p>

	</div>

	<!-- Fin of 1st Div -->


	<!-- 2nd Div -->

	<div class="container p-3">
		<p class="fs-2"></p>

		<div class="row">
			
			<!-- col-1 -->
			<div class="col-md-6 p-5">
				<!-- for Background image -->
				<!-- <img alt="" src="img/t1.jpg" width="500px" height="400px"> -->
				<img alt="" src="img/t1.jpg" width="370" height="">
			</div>
			
			<!-- col-2 -->
			<div class="col-md-6">
				<div class="card my-card">
					<div class="card-body">
						<p class="text-center fs-3">Tutoria</p>

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


						<!-- boostrap form -->
						<form class="row g-3" action="addTutoria" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							
							<!-- tomar ID de usuario en el campo oculto -->
							<input type="hidden" name="userId" value="${ userObj.id }">
							
							<div class="col-md-6">
								<label class="form-label">Nombre completo</label> <input required="required"
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control">

							</div>

							<div class="col-md-6">
								<label class="form-label">Genero</label> <select
									class="form-control" name="genero" required="required">
									<option selected="selected" disabled="disabled">---Selecciona
										Genero---</option>
									<option value="masculino">Masculino</option>
									<option value="femenino">Fememino</option>
								</select>
							</div>

							<div class="col-md-6">
								<label class="form-label">Edad</label> <input name="edad"
								required="required"	type="number" placeholder="Ingrese edad" class="form-control">
							</div>
							<div class="col-md-6">
								<label class="form-label">Fecha/Tutoria</label> <input
								required="required"	name="ftutoria" type="date" class="form-control">
							</div>

							<div class="col-md-6">
								<label class="form-label">Email</label> <input name="email"
								required="required"	type="email" placeholder="Ingrese correo electronico" class="form-control">
							</div>

							<div class="col-md-6">
								<label class="form-label">Telefono</label> <input name="telefono"
								required="required"	type="number" maxlength="11" placeholder="Ingrese telefono"
									class="form-control">
							</div>

							<div class="col-md-6">
								<label class="form-label">Experiencia</label> <input
								required="required"	name="experiencia" type="text" placeholder="Ingrese conocimiento previo"
									class="form-control">
							</div>

							<div class="col-md-6">
								<label class="form-label">Docente</label> <select
								 required="required" class="form-control" name="docenteNameSelect">
									<option selected="selected" disabled="disabled">---Seleccionar---</option>
									
									<%
									DocenteDAO docenteDAO = new DocenteDAO(DBConnection.getConn());
									List<Docente> listOfDocente = docenteDAO.getAllDocente();
									for(Docente d : listOfDocente)
									
									{%>
									<!-- tomamos el id docente de la tabla docentes -->
									<option value="<%=com.tme.security.Html.escape(d.getId())%>"> <%=com.tme.security.Html.escape(d.getNombre())%> (<%=com.tme.security.Html.escape(d.getEspecialidad())%>) </option>
									
									<%
									}
									%>
									
									<!-- <option>Nombre docente</option> -->
								</select>
							</div>


							<!-- a continuación son visibles en la parte derecha del formulario-->

							<div class="col-md-12">
								<label class="form-label">Dirrecion completa</label>
								<textarea name="direccion" required="required" class="form-control" rows="3" cols=""></textarea>
							</div>


							<c:if test="${empty userObj}">
								<div class="col-md-12">
									<a href="alumno_login.jsp" class="btn text-white col-md-12" style="background-color: rgba(32, 190, 198);">Enviar</a>
								</div>
							</c:if>


							<c:if test="${not empty userObj}">

								<div class="col-md-12">
									<button type="submit" class="btn text-white col-md-12" style="background-color: rgba(32, 190, 198);">Enviar</button>
								</div>

							</c:if>

						</form>

						<!-- Fin of boostrap form -->

					</div>
				</div>

			</div>



		</div>


	</div>

	<!-- 2nd Div -->















<!-- footer -->
<%@include file="component/footer.jsp" %>
<!-- Fin footer -->

</body>
</html>