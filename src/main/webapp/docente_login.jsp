<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>

<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(173, 216, 230, 1);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}
</style>
<!-- Fin del CSS personalizado para esta página. -->

<head>
<meta charset="UTF-8">
<title>Profesor Login</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>
<%@include file="component/allcss.jsp"%>
</head>
<body>
	<!-- navbar -->
	<%@include file="component/navbar.jsp"%>
	<!-- end navbar -->


	<!-- <h1>Docente Login</h1> -->
	<!-- Docente Login -->
	<div class="container p-5">
		<div class="row">
			<div class="col-md-4 offset-md-4">
				<div class="card my-card">
					<div class="card-header text-center text-white" style="background-color: rgba(32, 190, 198);">
						<!-- <p class="fs-4 text-center mt-1"><i class="fa-solid fa-users"></i> <br>User Login</p> -->
						<p class="fs-4 text-center text-white mt-2">
							<i class="fas fa-chalkboard-teacher"></i> Profesor Login
						</p>
					</div>

					<div class="card-body">
						<!-- <p class="fs-4 text-center"><i class="fa-solid fa-chalkboard-user"></i> Docente Login</p> -->

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
						<form action="docenteLogin" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
							<div class="mb-3">
								<label class="form-label">Email</label> <input
									name="email" type="email" placeholder="Ingrese correo electronico"
									class="form-control">
								<div id="emailHelp" class="form-text">Nunca compartiremos
									tu email con cualquier otra persona.</div>
							</div>
							<div class="mb-3">
								<label class="form-label">Clave</label> <input
									name="clave" maxlength="128" autocomplete="current-password" type="password" placeholder="Ingrese clave"
									class="form-control">
							</div>

							<button type="submit" class="btn text-white col-md-12" style="background-color: rgba(32, 190, 198);">Iniciar sesión</button>
						</form>
						<!-- Fin of boostrap form -->
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- Fin of Docente Login -->





</body>
</html>