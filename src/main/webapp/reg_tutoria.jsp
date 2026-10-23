<%@page import="com.tme.modal.Docente"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.DocenteDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Tutoria</title>

<%@include file="component/allcss.jsp"%>

</head>
<body class="tm-page">
	<%@include file="component/navbar.jsp"%>
<main id="contenido" class="tm-main" tabindex="-1">
<%@ include file="component/messages.jsp" %>
<c:set var="datosTutoria" value="${sessionScope.tutoriaForm}" />
<c:remove var="tutoriaForm" scope="session" />
<c:set var="docenteSeleccionado" value="${datosTutoria.docenteNameSelect}" scope="request" />

	<div class="container p-3">
		<div class="row">

			<div class="col-md-5 tm-aside-column">

                <aside class="tm-form-aside">
                    <p class="tm-eyebrow">Un espacio para tus preguntas</p>
                    <h2>Tu próximo paso,<br>a tu ritmo.</h2>
                    <p>Elige un docente y cuéntale qué necesitas reforzar. Podrás consultar su respuesta y el seguimiento de la sesión en Mis tutorías.</p>
                    <p class="text-muted">Sesiones de 15 a 180 minutos · Hora de Lima</p>
                    <img src="img/t1.jpg" alt="Alumna estudiando con sus apuntes" width="598" height="900" loading="lazy">
                </aside>
			</div>

			<div class="col-md-7">
				<div class="card my-card">
					<div class="card-body">
						<h1 class="tm-page-title">Solicitar tutoría</h1>
                        <c:choose>
                            <c:when test="${not empty sessionScope.userObj}">
                                <p class="text-muted">Elige una fecha y hora futuras (Lima). El horario queda reservado mientras el docente revisa tu solicitud.</p>
                            </c:when>
                            <c:when test="${not empty sessionScope.adminObj or not empty sessionScope.docenteObj}">
                                <div class="alert alert-info" role="status">
                                    Las solicitudes de tutoría se realizan con una cuenta de alumno.
                                    Puedes consultar el formulario e <a href="alumno_login.jsp">iniciar sesión como alumno</a>.
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="alert alert-info" role="status">
                                    Para enviar una solicitud de tutoría, inicia sesión como alumno.
                                    <a href="alumno_login.jsp">Iniciar sesión</a> o <a href="reg_alumno.jsp">crear una cuenta</a>.
                                </div>
                            </c:otherwise>
                        </c:choose>

						<form class="row g-3" action="addTutoria" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

							<div class="col-md-6">
								<label for="tm-nombre-1" class="form-label">Nombre completo</label> <input id="tm-nombre-1" required="required"
									name="nombre" type="text" placeholder="Ingrese nombre completo" maxlength="45" value="<c:out value='${empty datosTutoria ? userObj.nombre : datosTutoria.nombre}'/>"
									class="form-control">

							</div>

							<div class="col-md-6">
								<label for="tm-genero-2" class="form-label">Género</label> <select id="tm-genero-2"
									class="form-control" name="genero" required="required">
									<option value="" disabled ${empty datosTutoria.genero ? 'selected' : ''}>---Selecciona Género---</option>
									<option value="masculino" ${datosTutoria.genero eq 'masculino' ? 'selected' : ''}>Masculino</option>
									<option value="femenino" ${datosTutoria.genero eq 'femenino' ? 'selected' : ''}>Femenino</option>
								</select>
							</div>

							<div class="col-md-6">
								<label for="tm-edad-3" class="form-label">Edad</label> <input id="tm-edad-3" name="edad" min="1" max="120" value="<c:out value='${datosTutoria.edad}'/>"
								required="required"	type="number" placeholder="Ingrese edad" class="form-control">
							</div>
							<div class="col-md-6">
								<label for="tm-ftutoria-4" class="form-label">Fecha</label> <input id="tm-ftutoria-4" value="<c:out value='${datosTutoria.ftutoria}'/>"
								required="required"	name="ftutoria" type="date" min="<%=java.time.LocalDate.now(com.tme.service.ReglasTutoria.ZONA)%>" class="form-control">
							</div>

                            <div class="col-md-6">
                                <label for="horaInicio" class="form-label">Hora (Lima)</label>
                                <input id="horaInicio" name="horaInicio" type="time" required class="form-control" value="<c:out value='${datosTutoria.horaInicio}'/>">
                            </div>
                            <div class="col-md-6">
                                <label for="duracionMinutos" class="form-label">Duración en minutos</label>
                                <input id="duracionMinutos" name="duracionMinutos" type="number" min="15" max="180" value="<c:out value='${empty datosTutoria ? 60 : datosTutoria.duracionMinutos}'/>" required class="form-control">
                                <p class="form-text">De 15 a 180 minutos. La sesión debe terminar el mismo día.</p>
                            </div>
							<div class="col-md-6">
								<label for="tm-email-5" class="form-label">Correo electrónico</label> <input id="tm-email-5" name="email" maxlength="45" value="<c:out value='${empty datosTutoria ? userObj.email : datosTutoria.email}'/>"
								required="required"	type="email" placeholder="Ingrese correo electronico" class="form-control">
							</div>

							<div class="col-md-6">
								<label for="tm-telefono-6" class="form-label">Teléfono</label> <input id="tm-telefono-6" name="telefono"
									required="required"	type="tel" maxlength="45" placeholder="Ingrese telefono" value="<c:out value='${datosTutoria.telefono}'/>"
									class="form-control">
							</div>

							<div class="col-md-6">
								<label for="tm-experiencia-7" class="form-label">Tema o conocimiento previo</label> <input id="tm-experiencia-7" maxlength="45" value="<c:out value='${datosTutoria.experiencia}'/>"
								required="required"	name="experiencia" type="text" placeholder="Ingrese conocimiento previo"
									class="form-control">
							</div>

							<div class="col-md-6">
								<label for="tm-docenteNameSelect-8" class="form-label">Docente</label> <select id="tm-docenteNameSelect-8"
								 required="required" class="form-control" name="docenteNameSelect">
									<option value="" disabled ${empty datosTutoria.docenteNameSelect ? 'selected' : ''}>---Seleccionar---</option>

									<%
									DocenteDAO docenteDAO = new DocenteDAO(DBConnection.getConn());
									List<Docente> listOfDocente = docenteDAO.getActiveDocentes();
									for(Docente d : listOfDocente)

									{%>

									<option value="<%=d.getId()%>" <%=request.getAttribute("docenteSeleccionado") != null && request.getAttribute("docenteSeleccionado").equals(Integer.toString(d.getId())) ? "selected" : ""%>> <%=com.tme.security.Html.escape(d.getNombre())%> (<%=com.tme.security.Html.escape(d.getEspecialidad())%>) </option>

									<%
									}
									%>

								</select>
							</div>

							<div class="col-md-12">
								<label for="tm-direccion-9" class="form-label">Dirección completa</label>
								<textarea id="tm-direccion-9" name="direccion" maxlength="500" required="required" class="form-control" rows="3"><c:out value="${datosTutoria.direccion}" /></textarea>
							</div>

							<c:if test="${empty userObj}">
								<div class="col-md-12">
									<a href="alumno_login.jsp" class="btn btn-primary text-white col-md-12">Iniciar sesión para solicitar</a>
								</div>
							</c:if>

							<c:if test="${not empty userObj}">

								<div class="col-md-12">
									<button type="submit" class="btn btn-primary text-white col-md-12">Enviar</button>
								</div>

							</c:if>

						</form>

					</div>
				</div>

			</div>

		</div>

	</div>

</main>
<%@ include file="component/footer.jsp" %>
</body>
</html>
