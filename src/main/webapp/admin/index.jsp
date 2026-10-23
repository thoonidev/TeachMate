<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.db.DBConnection,com.tme.dao.DocenteDAO,com.tme.dao.TutoriaDAO,com.tme.service.ReglasTutoria.Rol" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Panel de administración | TeachMate</title>
    <%@ include file="../component/allcss.jsp" %>
</head>
<body>
    <%@ include file="../component/navbar.jsp" %>
    <main id="contenido" class="tm-main" tabindex="-1">
        <%@ include file="../component/messages.jsp" %>
        <%
            DocenteDAO docentes = new DocenteDAO(DBConnection.getConn());
            int totalDocentes = docentes.countTotalDocente();
            int activos = docentes.countActiveDocente();
            TutoriaDAO.Resumen resumen = new TutoriaDAO(DBConnection.getConn()).resumen(Rol.ADMIN, 0);
        %>
        <div class="container p-3">
            <div class="tm-page-heading">
                <div><p class="tm-eyebrow">Administración</p><h1 class="tm-page-title">Así va TeachMate</h1>
                    <p class="text-muted">Consulta la actividad y accede a las tareas de gestión.</p></div>
                <a class="btn btn-primary" href="docente.jsp">Registrar docente</a>
            </div>
            <div class="tm-metrics">
                <a class="tm-metric" href="alumno.jsp?estado=Pendiente"><span>Solicitudes pendientes</span><strong><%=resumen.pendientes()%></strong><small>Requieren respuesta del docente</small></a>
                <a class="tm-metric" href="alumno.jsp?estado=Aceptada"><span>Sesiones aceptadas</span><strong><%=resumen.aceptadas()%></strong><small>Confirma el seguimiento</small></a>
                <a class="tm-metric" href="list_docente.jsp"><span>Docentes activos</span><strong><%=activos%></strong><small><%=totalDocentes - activos%> inactivos · <%=totalDocentes%> registrados</small></a>
                <div class="tm-metric"><span>Alumnos registrados</span><strong><%=docentes.countTotalUser()%></strong><small>Cuentas de alumnos</small></div>
            </div>
            <div class="tm-role-guide">
                <section class="card"><div class="card-body"><h2 class="tm-section-title">Seguimiento de tutorías</h2>
                    <p><strong><%=resumen.total()%></strong> tutorías registradas · <strong><%=resumen.completadas()%></strong> completadas · <strong><%=resumen.cerradas()%></strong> canceladas o rechazadas.</p>
                    <p>Usa los filtros para revisar solicitudes por fecha, estado, alumno, docente o tema. Puedes cancelar una sesión para resolver una incidencia conservando su historial.</p>
                    <a class="btn btn-outline-primary" href="alumno.jsp">Consultar tutorías</a>
                </div></section>
                <section class="card"><div class="card-body"><h2 class="tm-section-title">Docentes y especialidades</h2>
                    <p>Gestiona los accesos desde Docentes. Desactivar una cuenta conserva sus tutorías; reactivarla permite recibir nuevas solicitudes.</p>
                    <p>Especialidades disponibles en el catálogo: <strong><%=docentes.countTotalEspecialidad()%></strong>.</p>
                    <a class="btn btn-outline-primary" href="list_docente.jsp">Gestionar docentes</a>
                    <button type="button" class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#exampleModal">Agregar especialidad</button>
                </div></section>
            </div>
        </div>
	<div class="modal fade" id="exampleModal" tabindex="-1"
		aria-labelledby="exampleModalLabel" aria-hidden="true">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title text-dark" id="exampleModalLabel">Agregar
						Especialidad</h5>
					<button type="button" class="btn-close" data-bs-dismiss="modal"
						aria-label="Cerrar"></button>
				</div>
				<div class="modal-body">

					<form action="../addEspecialidad" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

						<div class="form-group">
							<label for="tm-especialidadNombre-1" class="form-label">Ingrese nombre de la especialidad</label> <input id="tm-especialidadNombre-1"
								type="text" name="especialidadNombre"
								placeholder="Nombre de la especialidad" class="form-control" />
						</div>
						<div class="text-center mt-2">
							<button type="submit" class="btn btn-outline-danger ">Añadir</button>
						</div>

					</form>

				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-primary"
						data-bs-dismiss="modal">Cerrar</button>

				</div>
			</div>
		</div>
	</div>

    </main>
    <%@ include file="../component/footer.jsp" %>
</body>
</html>
