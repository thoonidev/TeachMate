<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.TutoriaDAO,com.tme.db.DBConnection,com.tme.modal.Docente,com.tme.modal.Tutoria,com.tme.service.ReglasTutoria,com.tme.service.ReglasTutoria.Rol" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Mi panel profesor | TeachMate</title>
    <%@ include file="../component/allcss.jsp" %>
</head>
<body>
    <%@ include file="../component/navbar.jsp" %>
    <main id="contenido" class="tm-main" tabindex="-1">
        <%@ include file="../component/messages.jsp" %>
        <%
            Docente panelDocente = (Docente) session.getAttribute("docenteObj");
            TutoriaDAO dao = new TutoriaDAO(DBConnection.getConn());
            TutoriaDAO.Resumen resumen = dao.resumen(Rol.DOCENTE, panelDocente.getId());
            Tutoria proxima = dao.proxima(Rol.DOCENTE, panelDocente.getId());
        %>
        <div class="container p-3">
            <div class="tm-page-heading">
                <div><p class="tm-eyebrow">Tu espacio de profesor</p>
                    <h1 class="tm-page-title">Hola, <c:out value="${docenteObj.nombre}" /></h1>
                    <p class="text-muted">Estas son tus solicitudes y sesiones. Los datos corresponden únicamente a tus asesorías.</p>
                </div>
                <a class="btn btn-primary" href="alumno.jsp">Gestionar mis asesorías</a>
            </div>
            <div class="tm-metrics">
                <a class="tm-metric" href="alumno.jsp?estado=Pendiente+de+pago"><span>Pendientes de pago</span><strong><%=resumen.pendientes()%></strong><small>Reservas aún sin confirmar</small></a>
                <a class="tm-metric" href="alumno.jsp?estado=Confirmadas"><span>Confirmadas</span><strong><%=resumen.aceptadas()%></strong><small>Consulta las sesiones confirmadas</small></a>
                <a class="tm-metric" href="alumno.jsp?estado=Completada"><span>Completadas</span><strong><%=resumen.completadas()%></strong><small>Revisa tu seguimiento anterior</small></a>
                <a class="tm-metric" href="alumno.jsp"><span>Total</span><strong><%=resumen.total()%></strong><small>Todo tu historial</small></a>
            </div>
            <section class="tm-next-session">
                <div><h2>Tu próxima sesión confirmada</h2>
                    <% if (proxima == null) { %>
                        <p>No tienes sesiones confirmadas próximas. Mantén actualizada tu disponibilidad para organizar tu agenda.</p>
                    <% } else { %>
                        <p><strong><%=com.tme.security.Html.escape(proxima.getNombre())%></strong> · <%=com.tme.security.Html.escape(proxima.getFtutoria())%> · <%=com.tme.security.Html.escape(proxima.getHorario())%> (Lima)</p>
                        <p>Tema: <%=com.tme.security.Html.escape(proxima.getExperiencia())%></p>
                    <% } %>
                </div>
                <a class="btn btn-outline-primary" href="alumno.jsp?estado=Confirmadas">Ver sesiones</a>
            </section>
            <div class="tm-role-guide">
                <section class="card"><div class="card-body"><h2 class="tm-section-title">Organiza tu disponibilidad</h2><p>Los alumnos reservan directamente tus horarios publicados. No necesitas aceptar cada reserva. La clase se confirma cuando administración valida el ingreso.</p><a href="disponibilidad.jsp">Gestionar disponibilidad →</a></div></section>
                <section class="card"><div class="card-body"><h2 class="tm-section-title">Da seguimiento al aprendizaje</h2><p>Registra comentarios desde Mis asesorías. Cuando termine una sesión confirmada, podrás registrar que se realizó o reportar una incidencia.</p><a href="edit_profile.jsp">Mi perfil y contraseña →</a></div></section>
            </div>
        </div>
    </main>
    <%@ include file="../component/footer.jsp" %>
</body>
</html>
