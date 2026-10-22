<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
	<div class="container-fluid">
		<a class="navbar-brand" href="index.jsp"><i
			class="fa-solid fa-school"></i> TeachMate</a>
		<!-- 		<a class="navbar-brand" href="index.jsp"><i
			class="fa-solid fa-school"></i> TeachMate</a> -->
		<button class="navbar-toggler" type="button" data-bs-toggle="collapse"
			data-bs-target="#navbarSupportedContent"
			aria-controls="navbarSupportedContent" aria-expanded="false"
			aria-label="Toggle navigation">
			<span class="navbar-toggler-icon"></span>
		</button>
		<div class="collapse navbar-collapse" id="navbarSupportedContent">
			<!-- original ul <ul class="navbar-nav me-auto mb-2 mb-lg-0"> -->
			<ul class="navbar-nav me-auto mb-2 mb-lg-0">
				<li class="nav-item"><a class="nav-link active"
					aria-current="page" href="index.jsp"><i class="fa fa-home"></i>
						INICIO</a></li>
				<li class="nav-item"><a class="nav-link active"
					aria-current="page" href="docente.jsp"><i
						class="fa-solid fa-chalkboard-user"></i> PROFESOR</a></li>
				<li class="nav-item"><a class="nav-link active"
					aria-current="page" href="list_docente.jsp"><i
						class="fa-solid fa-list"></i> VER PROFESOR</a></li>
				<li class="nav-item"><a class="nav-link active"
					aria-current="page" href="alumno.jsp"><i
						class="fas fa-user-circle"></i> ALUMNO</a></li>
			</ul>

			<div class="dropdown">
				<button class="btn btn-light  dropdown-toggle" type="button"
					id="dropdownMenuButton1" data-bs-toggle="dropdown"
					aria-expanded="false">
					<i class="fa fa-universal-access"></i> Admin
				</button>
				<ul class="dropdown-menu" aria-labelledby="dropdownMenuButton1">
					<li><form action="../adminLogout" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button type="submit" class="dropdown-item">Cerrar sesión</button></form></li>

				</ul>
			</div>


		</div>
	</div>
</nav>