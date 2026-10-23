<%@ page pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.TutoriaDAO,com.tme.db.DBConnection,com.tme.modal.*,com.tme.service.FiltroTutorias,com.tme.service.ReglasTutoria,com.tme.service.ReglasTutoria.Rol" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    TutoriaDAO panelDao = new TutoriaDAO(DBConnection.getConn());
    FiltroTutorias filtro;
    String errorFiltro = null;
    try {
        filtro = new FiltroTutorias(request.getParameter("estado"), request.getParameter("desde"),
                request.getParameter("hasta"), request.getParameter("buscar"), FiltroTutorias.pagina(request.getParameter("pagina")));
    } catch (IllegalArgumentException exception) {
        response.setStatus(400);
        errorFiltro = exception.getMessage();
        filtro = new FiltroTutorias(null, null, null, null, 1);
    }
    TutoriaDAO.Resumen resumen = panelDao.resumen(panelRol, panelActorId);
    TutoriaDAO.Pagina resultado = panelDao.buscar(panelRol, panelActorId, filtro);
    Tutoria proxima = panelRol == Rol.ADMIN ? null : panelDao.proxima(panelRol, panelActorId);
    java.time.LocalDateTime horaPanel = ReglasTutoria.ahora();
    request.setAttribute("numeroAnterior", resultado.pagina() - 1);
    request.setAttribute("numeroSiguiente", resultado.pagina() + 1);
%>
<c:set var="panelUrl" value="${pageContext.request.contextPath}${pageContext.request.servletPath}" />
<div class="container p-3">
    <div class="tm-page-heading">
        <div>
            <p class="tm-eyebrow"><%=panelRol == Rol.ADMIN ? "Seguimiento general" : panelRol == Rol.DOCENTE ? "Tu espacio docente" : "Tu espacio de aprendizaje"%></p>
            <h1 class="tm-page-title"><%=panelRol == Rol.ADMIN ? "Tutorías" : "Mis tutorías"%></h1>
            <p class="text-muted"><%=panelRol == Rol.DOCENTE ? "Revisa las solicitudes, confirma tus sesiones y registra el seguimiento de cada alumno."
                    : panelRol == Rol.ADMIN ? "Consulta el historial y resuelve incidencias sin eliminar las tutorías."
                    : "Consulta las respuestas de tus docentes y organiza tus próximas sesiones."%></p>
        </div>
        <% if (panelRol == Rol.ALUMNO) { %><a class="btn btn-primary" href="${pageContext.request.contextPath}/reg_tutoria.jsp">Solicitar tutoría</a><% } %>
    </div>
    <div class="tm-metrics" aria-label="Resumen de tutorías">
        <div class="tm-metric"><span>Pendientes</span><strong><%=resumen.pendientes()%></strong><small>Solicitudes sin respuesta</small></div>
        <div class="tm-metric"><span>Aceptadas</span><strong><%=resumen.aceptadas()%></strong><small>Confirmadas por el docente</small></div>
        <div class="tm-metric"><span>Completadas</span><strong><%=resumen.completadas()%></strong><small>Sesiones finalizadas</small></div>
        <div class="tm-metric"><span>Total</span><strong><%=resumen.total()%></strong><small>Incluye canceladas y rechazadas</small></div>
    </div>
    <% if (panelRol != Rol.ADMIN) { %>
    <section class="tm-next-session" aria-label="Próxima sesión">
        <div>
            <h2><%=proxima != null && !ReglasTutoria.inicio(proxima).isAfter(horaPanel) ? "Sesión en curso" : "Próxima tutoría confirmada"%></h2>
            <% if (proxima == null) { %>
                <p>No tienes sesiones confirmadas próximas. Las solicitudes pendientes aparecerán aquí cuando el docente las acepte.</p>
            <% } else { %>
                <p><strong><%=com.tme.security.Html.escape(proxima.getFtutoria())%> · <%=com.tme.security.Html.escape(proxima.getHorario())%></strong> (Lima)</p>
                <p>Alumno: <%=com.tme.security.Html.escape(proxima.getNombre())%> · Docente: <%=com.tme.security.Html.escape(proxima.getNombreDocente())%></p>
            <% } %>
        </div>
        <% if (panelRol == Rol.DOCENTE) { %><a class="btn btn-outline-primary" href="${panelUrl}?estado=Pendiente">Revisar solicitudes</a><% } %>
    </section>
    <% } else { %>
        <p class="alert alert-info">Puedes cancelar tutorías pendientes o aceptadas para resolver incidencias, incluidas las de docentes inactivos. Se conserva el historial.</p>
    <% } %>
    <section class="card"><div class="card-body">
        <h2 class="tm-section-title">Consulta e historial</h2>
        <% if (errorFiltro != null) { %><p class="alert alert-danger" role="alert"><%=com.tme.security.Html.escape(errorFiltro)%></p><% } %>
        <form method="get" action="${panelUrl}" class="tm-filters">
            <div class="tm-filter-search"><label class="form-label" for="buscar">Buscar</label>
                <input class="form-control" type="search" id="buscar" name="buscar" maxlength="60" placeholder="Alumno, docente o tema" value="<%=com.tme.security.Html.escape(filtro.buscar())%>"></div>
            <div><label class="form-label" for="estado">Estado</label><select class="form-select" id="estado" name="estado">
                <option value="">Todos</option>
                <% for (EstadoTutoria estadoFiltro : EstadoTutoria.values()) { %>
                    <option value="<%=estadoFiltro.getValor()%>" <%=estadoFiltro.getValor().equals(filtro.estado()) ? "selected" : ""%>><%=estadoFiltro.getValor()%></option>
                <% } %>
            </select></div>
            <div><label class="form-label" for="desde">Desde</label><input class="form-control" type="date" id="desde" name="desde" value="<%=com.tme.security.Html.escape(filtro.desde())%>"></div>
            <div><label class="form-label" for="hasta">Hasta</label><input class="form-control" type="date" id="hasta" name="hasta" value="<%=com.tme.security.Html.escape(filtro.hasta())%>"></div>
            <div class="tm-filter-actions"><button type="submit" class="btn btn-primary">Filtrar</button><a class="btn btn-outline-secondary" href="${panelUrl}">Limpiar</a></div>
        </form>
        <p class="text-muted small" role="status">Resultados: <%=resultado.total()%> · Página <%=resultado.pagina()%> de <%=resultado.paginas()%>. Horarios en Lima.</p>
        <div class="table-responsive" role="region" aria-label="Tutorías e historial. Desliza para ver todas las columnas." tabindex="0">
            <table class="table table-striped">
                <caption>Solicitudes e historial. Los horarios pendientes todavía requieren confirmación.</caption>
                <thead><tr><th scope="col">Alumno / sesión</th><th scope="col">Fecha y horario</th><th scope="col">Docente</th><th scope="col">Estado</th><th scope="col">Información</th><th scope="col">Acciones</th></tr></thead>
                <tbody>
                <% if (resultado.tutorias().isEmpty()) { %>
                    <tr><td colspan="6"><div class="tm-empty">No hay tutorías para mostrar. Puedes cambiar los filtros o consultar de nuevo cuando haya solicitudes.</div></td></tr>
                <% }
                for (Tutoria fila : resultado.tutorias()) { %>
                    <tr>
                        <td><strong><%=com.tme.security.Html.escape(fila.getNombre())%></strong><span class="d-block text-muted small">Tutoría #<%=fila.getId()%></span></td>
                        <td><%=com.tme.security.Html.escape(fila.getFtutoria())%><span class="d-block text-muted small"><%=com.tme.security.Html.escape(fila.getHorario())%></span></td>
                        <td><%=com.tme.security.Html.escape(fila.getNombreDocente())%></td>
                        <td><span class="tm-status tm-status-<%=fila.getEstado().toLowerCase(java.util.Locale.ROOT)%>"><%=fila.getEstado()%></span></td>
                        <td><details class="tm-details"><summary>Ver información</summary><dl>
                            <dt>Tema o conocimiento previo</dt><dd><%=com.tme.security.Html.escape(fila.getExperiencia())%></dd>
                            <dt>Correo</dt><dd><%=com.tme.security.Html.escape(fila.getEmail())%></dd>
                            <dt>Teléfono</dt><dd><%=com.tme.security.Html.escape(fila.getTelefono())%></dd>
                            <dt>Edad / género</dt><dd><%=com.tme.security.Html.escape(fila.getEdad())%> · <%=com.tme.security.Html.escape(fila.getGenero())%></dd>
                            <dt>Dirección</dt><dd><%=com.tme.security.Html.escape(fila.getDireccion())%></dd>
                            <dt>Comentario del docente</dt><dd class="tm-comment"><%=fila.getComentario().isBlank() ? "Sin comentarios todavía." : com.tme.security.Html.escape(fila.getComentario())%></dd>
                        </dl></details></td>
                        <td>
                            <% boolean tieneAccion = false;
                            if (panelRol == Rol.DOCENTE) { tieneAccion = true; %>
                                <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/docente/comentario.jsp?id=<%=fila.getId()%>">Comentario</a>
                            <% }
                            for (EstadoTutoria siguiente : EstadoTutoria.values()) {
                                if (ReglasTutoria.puedeCambiar(fila, siguiente, panelRol, horaPanel)) {
                                    tieneAccion = true;
                                    String accion = panelRol == Rol.DOCENTE ? "/cambiarEstadoTutoria" : panelRol == Rol.ALUMNO ? "/cancelarTutoria" : "/adminCancelarTutoria"; %>
                                <form action="${pageContext.request.contextPath}<%=accion%>" method="post" class="d-inline">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="id" value="<%=fila.getId()%>">
                                    <input type="hidden" name="estado" value="<%=siguiente.getValor()%>">
                                    <button class="btn <%=siguiente == EstadoTutoria.CANCELADA || siguiente == EstadoTutoria.RECHAZADA ? "btn-outline-danger" : "btn-outline-primary"%> btn-sm" type="submit"><%=siguiente.getAccion()%></button>
                                </form>
                            <% } }
                            if (!tieneAccion) { %><span class="text-muted small">Sin acciones disponibles</span><% } %>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <nav class="tm-pagination" aria-label="Páginas de tutorías">
            <% if (resultado.pagina() > 1) { %>
                <c:url var="anteriorUrl" value="${pageContext.request.servletPath}"><c:param name="estado" value="<%=filtro.estado()%>"/><c:param name="desde" value="<%=filtro.desde()%>"/><c:param name="hasta" value="<%=filtro.hasta()%>"/><c:param name="buscar" value="<%=filtro.buscar()%>"/><c:param name="pagina" value="${numeroAnterior}"/></c:url>
                <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${anteriorUrl}'/>">Anterior</a>
            <% } %>
            <span>Página <%=resultado.pagina()%> de <%=resultado.paginas()%></span>
            <% if (resultado.pagina() < resultado.paginas()) { %>
                <c:url var="siguienteUrl" value="${pageContext.request.servletPath}"><c:param name="estado" value="<%=filtro.estado()%>"/><c:param name="desde" value="<%=filtro.desde()%>"/><c:param name="hasta" value="<%=filtro.hasta()%>"/><c:param name="buscar" value="<%=filtro.buscar()%>"/><c:param name="pagina" value="${numeroSiguiente}"/></c:url>
                <a class="btn btn-outline-secondary btn-sm" href="<c:out value='${siguienteUrl}'/>">Siguiente</a>
            <% } %>
        </nav>
    </div></section>
</div>
