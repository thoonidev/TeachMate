<%@page import="com.tme.modal.Docente"%>
<%@page import="com.tme.dao.DocenteDAO"%>
<%@page import="com.tme.modal.Especialidad"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.EspecialidadDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Lista | Docente</title>

<%@include file="../component/allcss.jsp"%>

</head>
<body class="tm-table-page">
	<%@include file="../component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="../component/messages.jsp" %>

	<div class="container-fluid p-3">
		<div class="row">

			<div class="col-md-12">
				<div class="card my-card">
					<div class="card-body">
						<h1 class="tm-page-title">Docentes</h1>
                        <p class="text-muted">Desactivar conserva el historial. Revisa sus solicitudes abiertas antes de desactivarlo; al reactivarlo no se reabren las tutorías canceladas.</p>
                        <a class="btn btn-primary mb-4" href="docente.jsp">Registrar docente</a>

						<div class="table-responsive" role="region" aria-label="Docentes registrados. Desliza para ver todas las columnas." tabindex="0">
<table class="table table-striped">
<caption>Docentes registrados. Desliza para ver todas las columnas.</caption>
							<thead>
								<tr>

									<th scope="col">Nombres</th>
									<th scope="col">FNacim</th>
									<th scope="col">Calificacion</th>
									<th scope="col">Especialidad</th>
									<th scope="col">Correo electrónico</th>
									<th scope="col">Teléfono</th>
                                    <th scope="col">Estado</th>
									<th colspan="2" class="text-center" scope="col">Acción</th>
								</tr>
							</thead>
							<tbody>

								<%
								DocenteDAO docDAO2 = new DocenteDAO(DBConnection.getConn());
                                List<Docente> listOfDoc = docDAO2.getAllDocente();
                                if (listOfDoc.isEmpty()) { %>
                                    <tr><td colspan="9"><div class="tm-empty">Todavía no hay docentes registrados. Usa Registrar docente para añadir el primero.</div></td></tr>
                                <% }
								for (Docente docenteLst : listOfDoc) {
								%>
								<tr>

									<th><%=com.tme.security.Html.escape(docenteLst.getNombre())%></th>
									<td><%=com.tme.security.Html.escape(docenteLst.getFnacim())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getCalificacion())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getEspecialidad())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getEmail())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getTelefono())%></td>
                                    <td><span class="tm-status <%=docenteLst.isActivo() ? "tm-status-activo" : "tm-status-inactivo"%>"><%=docenteLst.isActivo() ? "Activo" : "Inactivo"%></span></td>

									<td><a class="btn btn-sm btn-primary"
										href="edit_docente.jsp?id=<%=com.tme.security.Html.escape(docenteLst.getId())%>">Editar</a></td>
                                    <td><% if (docenteLst.isActivo()) { %>
                                        <form action="../deactivateDocente" method="post">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="id" value="<%=com.tme.security.Html.escape(docenteLst.getId())%>">
                                            <button type="submit" class="btn btn-sm btn-warning">Desactivar</button>
                                        </form>
                                    <% } else { %>
                                        <span class="d-block text-muted small">Historial conservado</span>
                                        <form action="../reactivateDocente" method="post">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="id" value="<%=docenteLst.getId()%>">
                                            <button type="submit" class="btn btn-sm btn-outline-primary">Reactivar</button>
                                        </form>
                                    <% } %></td>

								</tr>
								<%
								}
								%>

							</tbody>
						</table>
</div>

					</div>

				</div>
			</div>
		</div>
	</div>

</main>
<%@ include file="../component/footer.jsp" %>
</body>
</html>
