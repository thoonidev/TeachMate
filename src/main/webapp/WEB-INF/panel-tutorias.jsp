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
            <p class="tm-eyebrow"><%=panelRol == Rol.ADMIN ? "Seguimiento general" : panelRol == Rol.DOCENTE ? "Tu espacio de profesor" : "Tu espacio de aprendizaje"%></p>
            <h1 class="tm-page-title"><%=panelRol == Rol.ADMIN ? "Asesorías" : "Mis asesorías"%></h1>
            <p class="text-muted"><%=panelRol == Rol.DOCENTE ? "Consulta las reservas de tus horarios publicados y registra el seguimiento de cada alumno."
                    : panelRol == Rol.ADMIN ? "Consulta el historial y resuelve incidencias sin eliminar las asesorías."
                    : "Consulta tus reservas, completa el pago y organiza tus próximas sesiones."%></p>
        </div>
        <% if (panelRol == Rol.ALUMNO) { %><a class="btn btn-primary" href="${pageContext.request.contextPath}/oferta.jsp">Solicitar asesoría</a><% } %>
    </div>
    <div class="tm-metrics" aria-label="Resumen de asesorías">
        <div class="tm-metric"><span>Pendientes de pago</span><strong><%=resumen.pendientes()%></strong><small>Reservas aún sin confirmar</small></div>
        <div class="tm-metric"><span>Confirmadas / anteriores</span><strong><%=resumen.aceptadas()%></strong><small>Confirmadas y aceptadas anteriores</small></div>
        <div class="tm-metric"><span>Completadas</span><strong><%=resumen.completadas()%></strong><small>Sesiones finalizadas</small></div>
        <div class="tm-metric"><span>Total</span><strong><%=resumen.total()%></strong><small>Incluye canceladas y rechazadas</small></div>
    </div>
    <% if (panelRol != Rol.ADMIN) { %>
    <section class="tm-next-session" aria-label="Próxima sesión">
        <div>
            <h2><%=proxima != null && !ReglasTutoria.inicio(proxima).isAfter(horaPanel) ? "Sesión en curso" : "Próxima asesoría confirmada"%></h2>
            <% if (proxima == null) { %>
                <p>No tienes sesiones confirmadas próximas. Las solicitudes aparecerán aquí cuando la sesión esté confirmada.</p>
            <% } else { %>
                <p><strong><%=com.tme.security.Html.escape(proxima.getFtutoria())%> · <%=com.tme.security.Html.escape(proxima.getHorario())%></strong> (Lima)</p>
                <p>Alumno: <%=com.tme.security.Html.escape(proxima.getNombre())%> · Profesor: <%=com.tme.security.Html.escape(proxima.getNombreDocente())%></p>
            <% } %>
        </div>
        <% if (panelRol == Rol.DOCENTE) { %><a class="btn btn-outline-primary" href="${panelUrl}?estado=Pendiente+de+pago">Ver reservas pendientes</a><% } %>
    </section>
    <% } else { %>
        <p class="alert alert-info">Abre una asesoría para revisar cancelaciones, cambios de horario e incidencias. Cada decisión conserva su motivo e historial.</p>
    <% } %>
    <section class="card mb-3"><div class="card-body"><h2 class="tm-section-title">Pendientes por atender</h2>
<% if(panelRol==Rol.DOCENTE){ %><p>Las reservas de tus horarios publicados no requieren aceptación. Mantén tu disponibilidad actualizada y prepara las sesiones cuando el pago esté validado.</p><a class="btn btn-outline-primary btn-sm" href="${panelUrl}?estado=Pendiente+de+pago">Reservas pendientes de pago</a>
<% }else if(panelRol==Rol.ALUMNO){ %><p>Consulta los pagos o correcciones pendientes. Un comprobante enviado espera validación administrativa.</p><a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/mis_pagos.jsp?estado=Por+atender">Comprobantes por corregir</a> <a class="btn btn-outline-primary btn-sm" href="${panelUrl}?estado=Pendiente+de+pago">Reservas pendientes de pago</a>
<% }else{ %><p>Verifica los comprobantes en Pagos y resuelve las incidencias antes del inicio de la clase. Los pagos en revisión se muestran por proximidad de la sesión.</p><a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/admin/pagos.jsp?estado=Por+atender">Revisar pagos pendientes</a> <a class="btn btn-outline-primary btn-sm" href="${panelUrl}?estado=Incidencia">Resolver incidencias</a><% } %>
<p class="form-text mb-0">El plazo para enviar o corregir el comprobante corre también de noche y en fines de semana. La revisión administrativa respeta el horario de atención acordado. Las reservas sin comprobante vencen automáticamente. Todavía no se envían avisos por correo; consulta el estado en este panel.</p></div></section>
    <section class="card"><div class="card-body">
        <h2 class="tm-section-title">Consulta e historial</h2>
        <% if (errorFiltro != null) { %><p class="alert alert-danger" role="alert"><%=com.tme.security.Html.escape(errorFiltro)%></p><% } %>
        <form method="get" action="${panelUrl}" class="tm-filters">
            <div class="tm-filter-search"><label class="form-label" for="buscar">Buscar</label>
                <input class="form-control" type="search" id="buscar" name="buscar" maxlength="60" placeholder="Alumno, profesor o tema" value="<%=com.tme.security.Html.escape(filtro.buscar())%>"></div>
            <div><label class="form-label" for="estado">Estado</label><select class="form-select" id="estado" name="estado">
                <option value="">Todos</option><option value="Confirmadas" <%=filtro.estado().equals("Confirmadas")?"selected":""%>>Confirmadas y aceptadas anteriores</option>
                <% for (EstadoTutoria estadoFiltro : EstadoTutoria.values()) { %>
                    <option value="<%=estadoFiltro.getValor()%>" <%=estadoFiltro.getValor().equals(filtro.estado()) ? "selected" : ""%>><%=estadoFiltro.getEtiqueta()%></option>
                <% } %>
            </select></div>
            <div><label class="form-label" for="desde">Desde</label><input class="form-control" type="date" id="desde" name="desde" value="<%=com.tme.security.Html.escape(filtro.desde())%>"></div>
            <div><label class="form-label" for="hasta">Hasta</label><input class="form-control" type="date" id="hasta" name="hasta" value="<%=com.tme.security.Html.escape(filtro.hasta())%>"></div>
            <div class="tm-filter-actions"><button type="submit" class="btn btn-primary">Filtrar</button><a class="btn btn-outline-secondary" href="${panelUrl}">Limpiar</a></div>
        </form>
        <p class="text-muted small" role="status">Resultados: <%=resultado.total()%> · Página <%=resultado.pagina()%> de <%=resultado.paginas()%>. Horarios en Lima.</p>
        <div class="table-responsive" role="region" aria-label="Asesorías e historial" tabindex="0">
            <table class="table table-striped tm-session-table">
                <caption>Solicitudes e historial. Las solicitudes y los pagos pendientes todavía requieren confirmación.</caption>
                <thead><tr><th scope="col">Tema / alumno</th><th scope="col">Fecha y horario</th><th scope="col">Profesor</th><th scope="col">Estado</th><th scope="col">Información</th><th scope="col">Acciones</th></tr></thead>
                <tbody>
                <% if (resultado.tutorias().isEmpty()) { %>
                    <tr><td colspan="6"><div class="tm-empty">No hay asesorías para mostrar. Puedes cambiar los filtros o consultar de nuevo cuando haya solicitudes.</div></td></tr>
                <% }
                for (Tutoria fila : resultado.tutorias()) { %>
                    <tr>
                        <td data-label="Tema / alumno"><strong><%=com.tme.security.Html.escape(fila.getExperiencia())%></strong><span class="d-block text-muted small"><%=com.tme.security.Html.escape(fila.getNombre())%> · #<%=fila.getId()%></span></td>
                        <td data-label="Fecha y horario"><%=com.tme.security.Html.escape(fila.getFtutoria())%><span class="d-block text-muted small"><%=com.tme.security.Html.escape(fila.getHorario())%></span></td>
                        <td data-label="Profesor"><%=com.tme.security.Html.escape(fila.getNombreDocente())%></td>
                        <td data-label="Estado"><span class="tm-status tm-status-<%=EstadoTutoria.desdeValor(fila.getEstado()).getClase()%>"><%=EstadoTutoria.desdeValor(fila.getEstado()).getEtiqueta()%></span>
<% if("Pendiente".equals(fila.getEstado())&&fila.getVenceRespuesta()!=null){ %><span class="d-block small">Registro anterior: respuesta hasta <%=fila.getVenceRespuesta().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))%> · Lima</span>
<% }else if("Pendiente de pago".equals(fila.getEstado())){if(fila.isPagoRevision()){ %><span class="d-block small">Comprobante en revisión o corrección. Consulta el pago para conocer el siguiente paso.</span><% }else if(fila.getVencePago()!=null){ %><span class="d-block small">Pago hasta <%=fila.getVencePago().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))%> · Lima</span><% }} %></td>
                        <td data-label="Información"><details class="tm-details"><summary>Ver información</summary><dl>
                            <dt>Curso y precio de la solicitud</dt><dd><%=fila.getCursoNombre()==null ? "Registro anterior sin tarifa" : com.tme.security.Html.escape(fila.getCursoNombre())+" · S/ "+fila.getPrecio()%></dd>
                            <dt>Tema o conocimiento previo</dt><dd><%=com.tme.security.Html.escape(fila.getExperiencia())%></dd>
                            <dt>Correo</dt><dd><%=com.tme.security.Html.escape(fila.getEmail())%></dd>
                            <dt>Teléfono</dt><dd><%=com.tme.security.Html.escape(fila.getTelefono())%></dd>
                            <% if(!fila.getEdad().isBlank() || !fila.getGenero().isBlank()){ %><dt>Edad / género</dt><dd><%=com.tme.security.Html.escape(fila.getEdad())%> · <%=com.tme.security.Html.escape(fila.getGenero())%></dd>
                            <% } if(!fila.getDireccion().isBlank()){ %><dt>Dirección</dt><dd><%=com.tme.security.Html.escape(fila.getDireccion())%></dd>
                            <% } %><dt>Comentario del profesor</dt><dd class="tm-comment"><%=fila.getComentario().isBlank() ? "Sin comentarios todavía." : com.tme.security.Html.escape(fila.getComentario())%></dd>
                        </dl></details></td>
                        <td data-label="Acciones">
                            <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/asesoria.jsp?id=<%=fila.getId()%>">Ver asesoría</a>
                            <% if(fila.isCancelacionSolicitada()){ %><span class="d-block text-muted small">Cancelación en revisión</span><% } %>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <nav class="tm-pagination" aria-label="Páginas de asesorías">
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
