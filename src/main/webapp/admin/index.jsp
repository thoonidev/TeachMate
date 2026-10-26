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
                <div><p class="tm-eyebrow">Administración</p><h1 class="tm-page-title">Resumen del equipo</h1>
                    <p class="text-muted">Consulta la actividad y accede a las tareas de gestión.</p></div>
                <a class="btn btn-primary" href="docente.jsp">Registrar profesor</a>
            </div>
            <div class="tm-metrics">
                <a class="tm-metric" href="alumno.jsp?estado=Pendiente+de+pago"><span>Reservas pendientes de pago</span><strong><%=resumen.pendientes()%></strong><small>Aún sin pago validado</small></a>
                <a class="tm-metric" href="alumno.jsp?estado=Confirmadas"><span>Sesiones confirmadas</span><strong><%=resumen.aceptadas()%></strong><small>Clases con pago validado</small></a>
                <a class="tm-metric" href="list_docente.jsp"><span>Profesores activos</span><strong><%=activos%></strong><small><%=totalDocentes - activos%> inactivos · <%=totalDocentes%> registrados</small></a>
                <div class="tm-metric"><span>Alumnos registrados</span><strong><%=docentes.countTotalUser()%></strong><small>Cuentas de alumnos</small></div>
            </div>
            <p class="alert alert-info">Revisa los pagos pendientes durante el horario de atención. <a href="pagos.jsp?estado=Por+atender">Abrir pagos por atender</a>. Confirma la clase solo después de verificar el ingreso.</p><div class="tm-role-guide">
                <section class="card"><div class="card-body"><h2 class="tm-section-title">Seguimiento de asesorías</h2>
                    <p><strong><%=resumen.total()%></strong> asesorías registradas · <strong><%=resumen.completadas()%></strong> completadas · <strong><%=resumen.cerradas()%></strong> canceladas o rechazadas.</p>
                    <p>Filtra por fecha, estado o participante. Abre una asesoría para gestionar cambios o incidencias.</p>
                    <a class="btn btn-outline-primary" href="alumno.jsp">Consultar asesorías</a>
                </div></section>
                <section class="card"><div class="card-body"><h2 class="tm-section-title">Profesores y cursos</h2>
                    <p>Registra profesores, asigna sus cursos y define las tarifas. Desactivar una cuenta conserva su historial.</p>
                    <p>Cursos registrados en el catálogo: <strong><%=docentes.countTotalEspecialidad()%></strong>.</p>
                    <a class="btn btn-outline-primary" href="list_docente.jsp">Gestionar profesores</a>
                    <a class="btn btn-outline-secondary" href="cursos.jsp">Cursos y tarifas</a>
                </div></section>
            </div>
        </div>
    </main>
    <%@ include file="../component/footer.jsp" %>
</body>
</html>
