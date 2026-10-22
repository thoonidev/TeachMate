<%@page import="com.tme.modal.Tutoria"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.modal.Docente"%>
<%@page import="com.tme.dao.TutoriaDAO"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.DocenteDAO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!-- for jstl tag -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!-- end of jstl tag -->

<%@page isELIgnored="false"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Detalles | Alumno</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

<%@include file="../component/allcss.jsp"%>


<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(137, 184, 255);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}
</style>
<!-- Fin del CSS personalizado para esta página. -->


</head>
<body>
	<%@include file="navbar.jsp"%>

	<!-- si "docenteObj" está vacío significa que nadie ha iniciado sesión. -->

	<c:if test="${empty docenteObj }">

		<c:redirect url="../docente_login.jsp"></c:redirect>

	</c:if>

	<!-- Compruebe si el docente está conectado o no -->


	<div class="container p-3">
		<div class="row">
			<div class="col-md-12">
				<div class="card my-card">
					<div class="card-body">
						<p class="text-center text-primary fs-3">Detalles del Alumno</p>

						<!-- message print -->
						<!-- for success msg -->
						<c:if test="${not empty successMsg}">
							<span hidden id="flash-successMsg"><c:out value="${successMsg}" /></span>
<script>
								Swal.fire({
									icon : 'success',
									title : 'Éxito',
									text : document.getElementById("flash-successMsg").textContent,
								});
							</script>
							<c:remove var="successMsg" scope="session" />
						</c:if>

						<!-- for error msg -->
						<c:if test="${not empty errorMsg}">
							<span hidden id="flash-errorMsg"><c:out value="${errorMsg}" /></span>
<script>
								Swal.fire({
									icon : 'error',
									title : 'Error',
									text : document.getElementById("flash-errorMsg").textContent,
								});
							</script>
							<c:remove var="errorMsg" scope="session" />
						</c:if>
						<!-- End of message print -->

						<!-- tabla para lista de alumnos -->

						<table class="table table-striped">
							<thead>
								<tr>
									<th scope="col" style="padding-right: 100px">Nombres</th>
									<th scope="col">Género</th>
									<th scope="col">Edad</th>
									<th scope="col">Dia/Tutoria</th>
									<th scope="col">Email</th>
									<th scope="col">Telefono</th>
									<th scope="col">Experiencia</th>
									<th scope="col">Estado</th>
									<th scope="col">Accion</th>
								</tr>
							</thead>
							<tbody>

								<%
								Docente docente = (Docente) session.getAttribute("docenteObj");

														//DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());
														TutoriaDAO tutDAO = new TutoriaDAO(DBConnection.getConn());
														List<Tutoria> list = tutDAO.getAllTutoriaByLoginDocente(docente.getId());
														for (Tutoria tutlist : list) {
								%>

								<tr>
									<th><%=com.tme.security.Html.escape(tutlist.getNombre())%></th>
									<td><%=com.tme.security.Html.escape(tutlist.getGenero())%></td>
									<td><%=com.tme.security.Html.escape(tutlist.getEdad())%></td>
									<td><%=com.tme.security.Html.escape(tutlist.getFtutoria())%></td>
									<td><%=com.tme.security.Html.escape(tutlist.getEmail())%></td>
									<td><%=com.tme.security.Html.escape(tutlist.getTelefono())%></td>
									<td><%=com.tme.security.Html.escape(tutlist.getExperiencia())%></td>
									<td><%=com.tme.security.Html.escape(tutlist.getEstado())%></td>

									<td>
										<%
										if ("Pendiente".equals(tutlist.getEstado())) {
										%> <a href="comentario.jsp?id=<%=com.tme.security.Html.escape(tutlist.getId())%>"
										class="btn btn-primary btn-sm">Comentario</a> 
										<%
 										} else {
 										%> 
 										 <a href="#!" class="btn btn-primary btn-sm disabled"><i
											class="fa fa-comment"></i> Comentario</a>
											 
										<%
 										}
 										%>


									</td>
									
								</tr>



								<%
								}
								%>


							</tbody>
						</table>

						<!-- Fin tabla para lista de alumnos -->



					</div>
				</div>
			</div>

		</div>

	</div>

</body>
</html>