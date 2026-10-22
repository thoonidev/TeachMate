<%@page import="com.tme.modal.Docente"%>
<%@page import="com.tme.dao.DocenteDAO"%>
<%@page import="com.tme.modal.Especialidad"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.EspecialidadDAO"%>
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
<title>Lista | Docente</title>
<script src="https://cdn.jsdelivr.net/npm/sweetalert2@11"></script>

<%@include file="../component/allcss.jsp"%>

<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgb(175, 175, 175);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}
</style>
<!-- Fin del CSS personalizado para esta página. -->

</head>
<body>
	<%@include file="navbar.jsp"%>


	<div class="container-fluid p-3">
		<div class="row">

			<div class="col-md-12">
				<div class="card my-card">
					<div class="card-body">
						<p class="fs-3 text-center text-dark">Lista de Profesores</p>

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

						<!-- tabla para la lista de docentes -->

						<table class="table table-striped">
							<thead>
								<tr class="table-info">
									<!-- <th scope="col">ID</th> -->
									<th scope="col">Nombres</th>
									<th scope="col">FNacim</th>
									<th scope="col">Calificacion</th>
									<th scope="col">Especialidad</th>
									<th scope="col">Email</th>
									<th scope="col">Telefono</th>
									<th colspan="2" class="text-center" scope="col">Accion</th>
								</tr>
							</thead>
							<tbody>

								<%
								DocenteDAO docDAO2 = new DocenteDAO(DBConnection.getConn());
								List<Docente> listOfDoc = docDAO2.getAllDocente();
								for (Docente docenteLst : listOfDoc) {
								%>
								<tr>
									<%-- <th scope="row"><%=com.tme.security.Html.escape(docenteLst.getId())%></th> --%>
									<th><%=com.tme.security.Html.escape(docenteLst.getNombre())%></th>
									<td><%=com.tme.security.Html.escape(docenteLst.getFnacim())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getCalificacion())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getEspecialidad())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getEmail())%></td>
									<td><%=com.tme.security.Html.escape(docenteLst.getTelefono())%></td>


									<td><a class="btn btn-sm btn-primary"
										href="edit_docente.jsp?id=<%=com.tme.security.Html.escape(docenteLst.getId())%>">Editar</a></td>
									<td><form action="../deleteDocente" method="post">
<input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="id" value="<%=com.tme.security.Html.escape(docenteLst.getId())%>"><button type="submit" class="btn btn-sm btn-danger">Eliminar</button></form></td>



								</tr>
								<%
								}
								%>


							</tbody>
						</table>

						<!-- fin tabla para la lista de docentes -->


					</div>

				</div>
			</div>
		</div>
	</div>









</body>
</html>