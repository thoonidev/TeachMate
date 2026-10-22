<!-- for jstl tag -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!-- end of jstl tag -->

<%@page isELIgnored="false"%>

<!-- <i class="fa-sharp fa-solid fa-school"></i> -->

<!-- <nav class="navbar navbar-expand-lg navbar-dark bg-danger"> -->
<nav class="navbar navbar-expand-lg navbar-dark"
	style="background-color: rgba(32, 190, 198);">
	<div class="container-fluid">
		<a class="navbar-brand" href="index.jsp"><i
			class="fa-solid fa-school"></i> TeachMate</a>
		<button class="navbar-toggler" type="button" data-bs-toggle="collapse"
			data-bs-target="#navbarSupportedContent"
			aria-controls="navbarSupportedContent" aria-expanded="false"
			aria-label="Toggle navigation">
			<span class="navbar-toggler-icon"></span>
		</button>
		<div class="collapse navbar-collapse" id="navbarSupportedContent">
			<!-- original ul <ul class="navbar-nav me-auto mb-2 mb-lg-0"> -->
			<ul class="navbar-nav ms-auto mb-2 mb-lg-0">

				<!-- Si el usuario no ha iniciado sesi�n, podr� ver los siguientes elementos -->
				<!-- para crear una barra de navegaci�n din�mica -->

				<c:if test="${empty userObj}">

					<li class="nav-item"><a class="nav-link active"
						aria-current="page" href="admin_login.jsp"><i
							class="fa-solid fa-right-to-bracket"></i> ADMIN</a></li>
					<li class="nav-item"><a class="nav-link active"
						aria-current="page" href="docente_login.jsp"> <i
							class="fas fa-sign-in-alt"></i> PROFESOR
					</a></li>
					<li class="nav-item"><a class="nav-link active"
						aria-current="page" href="reg_tutoria.jsp"><i
							class="fa fa-book fa-1x"></i> TUTORIA</a></li>
					<li class="nav-item"><a class="nav-link active"
						aria-current="page" href="alumno_login.jsp"><i
							class="fas fa-sign-in-alt"></i> ALUMNO</a></li>

				</c:if>

				<!-- finalizar si el usuario no ha iniciado sesi�n, entonces el usuario podr� ver los siguientes elementos -->



				<!-- Si el usuario ha iniciado sesi�n, podr� ver los siguientes elementos. -->
				<!-- para crear una barra de navegaci�n din�mica -->

				<c:if test="${not empty userObj }">
					<!-- Significa que el usuario est� conectado -->
					<li class="nav-item"><a class="nav-link active"
						aria-current="page" href="reg_tutoria.jsp"><i
							class="fa fa-book fa-1x"></i> TUTORIA</a></li>
					<li class="nav-item"><a class="nav-link active"
						aria-current="page" href="list_tutoria.jsp"><i
							class="fa fa-calendar-check-o"></i> VER TUTORIA</a></li>




					<div class="dropdown">
						<button class="btn btn-outline-light dropdown-toggle"
							type="button" id="dropdownMenuButton1" data-bs-toggle="dropdown"
							aria-expanded="false">
							<i class="fa-solid fa-circle-user"></i> <c:out value="${userObj.nombre}" />
						</button>
						<ul class="dropdown-menu" aria-labelledby="dropdownMenuButton1">
							<li><a class="dropdown-item" href="change_password.jsp">Cambiar Clave</a></li>
							<li><form action="userLogout" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button type="submit" class="dropdown-item">Cerrar sesión</button></form></li>

						</ul>
					</div>


					<%-- <li class="nav-item dropdown active"><a
						class="nav-link dropdown-toggle" href="#" id="navbarDropdown"
						role="button" data-bs-toggle="dropdown" aria-expanded="false">
							<i class="fa-solid fa-circle-user"></i> <c:out value="${userObj.nombre}" />
					</a>
						<ul class="dropdown-menu" aria-labelledby="navbarDropdown">
							<li><a class="dropdown-item" href="#">Change Password</a></li>
							<li><a class="dropdown-item" href="#">Logout</a></li>

						</ul></li> --%>

				</c:if>
				<!-- final de si el usuario ha iniciado sesi�n, entonces el usuario puede ver los siguientes elementos -->








			</ul>

		</div>
	</div>
</nav>