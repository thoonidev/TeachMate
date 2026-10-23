<%@page import="com.tme.modal.Tutoria"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.TutoriaDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Comentario | Alumno</title>

<%@include file="../component/allcss.jsp"%>

</head>
<body class="tm-page">

	<%@include file="../component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="../component/messages.jsp" %>

	<c:if test="${empty docenteObj }">

		<c:redirect url="../docente_login.jsp"></c:redirect>

	</c:if>

	<div class="container p-3">
		<div class="row">

			<div class="col-md-6 offset-md-3">
				<div class="card my-card">
					<div class="card-body">
						<h1 class="tm-page-title">Comentario / Recomendación</h1>

						<%
						/* obtener o recibir la identificación de la tutoria que llega a través de la URL (de la página alumno.jsp) */
						int id = Integer.parseInt(request.getParameter("id"));
						TutoriaDAO tutDAO = new TutoriaDAO(DBConnection.getConn());
						Tutoria tutoria = tutDAO.getTutoriaById(id);

						%>
                        <p class="text-muted">Tutoría #<%=tutoria.getId()%> · <%=com.tme.security.Html.escape(tutoria.getFtutoria())%> · <%=com.tme.security.Html.escape(tutoria.getHorario())%> (Lima)</p>
                        <p><span class="tm-status tm-status-<%=tutoria.getEstado().toLowerCase(java.util.Locale.ROOT)%>"><%=tutoria.getEstado()%></span> · Guardar un comentario no cambia el estado de la sesión.</p>
                        <a class="d-inline-block mb-3" href="alumno.jsp">← Volver a mis tutorías</a>

						<form class="row g-3" action="../updateComentario" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

							<div class="col-md-6">
								<label for="tm-nombre-1" class="form-label">Nombre completo</label> <input id="tm-nombre-1"
									name="nombre" type="text" placeholder="Ingrese nombre completo"
									class="form-control" readonly value="<%=com.tme.security.Html.escape(tutoria.getNombre())%>">

							</div>

							<div class="col-md-6">
								<label for="tm-edad-2" class="form-label">Edad</label> <input id="tm-edad-2" name="edad"
									type="number" placeholder="Ingrese su edad" class="form-control"
									readonly value="<%=com.tme.security.Html.escape(tutoria.getEdad())%>">
							</div>

							<div class="col-md-6">
								<label for="tm-telefono-3" class="form-label">Teléfono</label> <input id="tm-telefono-3" name="telefono"
									type="number" maxlength="11" placeholder="Ingrese telefono"
									class="form-control" readonly value="<%=com.tme.security.Html.escape(tutoria.getTelefono())%>">
							</div>

							<div class="col-md-6">
								<label for="tm-experiencia-4" class="form-label">Experiencia</label> <input id="tm-experiencia-4"
									name="experiencia" type="text" placeholder="Ingrese conocimiento previo"
									class="form-control" readonly value="<%=com.tme.security.Html.escape(tutoria.getExperiencia())%>">
							</div>

							<div class="col-md-12">
								<label for="tm-comentario-5" class="form-label">Comentario</label>
								<textarea id="tm-comentario-5" name="comentario" placeholder="Ingrese comentario"
									class="form-control" rows="4" maxlength="500" required><%=com.tme.security.Html.escape(tutoria.getComentario())%></textarea>
							</div>

							<input type="hidden" name="id" value="<%=com.tme.security.Html.escape(tutoria.getId())%>" class="form-control">

							<input type="hidden" name="docenteId" value="<%=com.tme.security.Html.escape(tutoria.getDocenteId())%>" class="form-control">

							<div class="col-md-12">
								<button type="submit" class="btn btn-primary col-md-12">Enviar</button>
							</div>

						</form>

					</div>
				</div>

			</div>

		</div>

	</div>

</main>
<%@ include file="../component/footer.jsp" %>
</body>
</html>
