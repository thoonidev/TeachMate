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
<title>Cambio | Clave</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<%@include file="component/allcss.jsp"%>
<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(173, 216, 230, 1);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}
</style>
<!-- Fin del CSS personalizado para esta página. -->


</head>
<body>
	<%@include file="component/navbar.jsp"%>
	
	
	<!-- Si no inicia sesión, inicie sesión primero -->
	<c:if test="${empty userObj }">

		<c:redirect url="/alumno_login.jsp"></c:redirect>

	</c:if>

	<div class="container p-4">
		<div class="row">
			<div class="col-md-4 offset-md-4">
				<div class="card my-card">

					<div class="card-body">
						<p class="fs-3 text-center text-info">Cambio de Clave</p>

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


						<form action="userChangePassword" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label class="form-label">Ingrese nueva clave</label> <input
									name="newPassword" maxlength="128" minlength="12" autocomplete="new-password" type="password" placeholder="Ingrese nueva clave"
									class="form-control" required="required">

							</div>
							<div class="mb-3">
								<label class="form-label">Ingrese la contraseña anterior</label> <input
									name="oldPassword" maxlength="128" autocomplete="current-password" type="password" placeholder="Ingrese la contraseña anterior"
									class="form-control" required>
							</div>
							<input type="hidden" value="${userObj.id}" name="userId">

							<button type="submit" class="btn text-white col-md-12" style="background-color: rgba(32, 190, 198);">Cambiar Clave</button>
						</form>
					</div>
				</div>

			</div>
		</div>
	</div>


</body>
</html>