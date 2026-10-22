<%@page import="com.tme.modal.Docente"%>
<%@page import="com.tme.dao.DocenteDAO"%>
<%@page import="com.tme.modal.User"%>
<%@page import="com.tme.modal.Tutoria"%>
<%@page import="java.util.List"%>
<%@page import="com.tme.db.DBConnection"%>
<%@page import="com.tme.dao.TutoriaDAO"%>
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
<!-- for responsive -->
<!-- <meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0"> -->
<!-- for responsive -->
<title>Lista | Tutorias</title>

<!-- all css include -->
<%@include file="../component/allcss.jsp"%>

<!-- CSS personalizado para esta página. -->
<style type="text/css">
.my-card {
	box-shadow: 0px 0px 10px 1px rgba(173, 216, 230, 1);
	/*box-shadow: 0px 0px 10px 0px rgba(0,0,0,0.3);*/
}

/* backgournd image css */
.my-bg-img {
	background: linear-gradient(rgba(0, 0, 0, .4), rgba(0, 0, 0, .4)),
		url("img/lt01.jpg");
	height: 20vh;
	width: 100%;
	background-size: cover;
	background-repeat: no-repeat;
}

/* backgournd image css */
</style>
<!-- Fin del CSS personalizado para esta página. -->


</head>
<body>
	<%@include file="component/navbar.jsp"%>

	<!-- Si no inicia sesión, inicie sesión primero -->
	<c:if test="${empty userObj }">

		<c:redirect url="/alumno_login.jsp"></c:redirect>

	</c:if>

	<!-- Inicio 1st Div -->

	<div class="container-fluid my-bg-img p-5">
		<!-- css background image -->
		<p class="text-center fs-2 text-white"></p>

	</div>

	<!-- Fin of 1st Div -->

	<!-- 2nd Div -->

	<div class="container-fluid p-3">
		<p class="fs-2"></p>

		<div class="row">



			<!-- col-2 -->
			<div class="col-md-9">
				<div class="card my-card">
					<div class="card-body">
						<p class="fw-bold text-center text-info fs-4">Lista de Tutorias</p>

						<%-- <!-- message print -->
						<!-- for success msg -->
						<c:if test="${not empty successMsg }">
							<p class="text-center text-success fs-5"><c:out value="${successMsg}" /></p>
							<c:remove var="successMsg" scope="session" />
						</c:if>

						<!-- for error msg -->
						<c:if test="${not empty errorMsg }">
							<p class="text-center text-danger fs-5"><c:out value="${errorMsg}" /></p>
							<c:remove var="errorMsg" scope="session" />
						</c:if>
						<!-- End of message print --> --%>

						<table class="table table-striped">
							<thead>
								<tr class="text-white" style="background-color: rgba(32, 190, 198);">
									<!-- <th scope="col">Id</th> -->
									<th scope="col">Nombres</th>
									<th scope="col">Genero</th>
									<th scope="col">Edad</th>
									<th scope="col">Dia/Tutoria</th>
									<!-- <th scope="col">Email</th> -->
									<th scope="col">Telefono</th>
									<th scope="col">Experiencia</th>
									<th scope="col">Nombre/Docente</th>
									<!-- <th scope="col">Direccion</th> -->
									<!-- <th scope="col">User Id</th> -->
									<th scope="col">Estado</th>
								</tr>
							</thead>
							<tbody>
								<%
								User user = (User) session.getAttribute("userObj");
														DocenteDAO dDAO = new DocenteDAO(DBConnection.getConn());

														TutoriaDAO tutDAO = new TutoriaDAO(DBConnection.getConn());

														List<Tutoria> list = tutDAO.getAllTutoriaByLoginUser(user.getId());
														for (Tutoria tuttList : list) {
															Docente docente = dDAO.getDocenteById(tuttList.getDocenteId());
								%>


								<tr>
									<%-- <th scope="row"><%=com.tme.security.Html.escape(tuttList.getId())%></th> --%>
									<td><%=com.tme.security.Html.escape(tuttList.getNombre())%></td>
									<td><%=com.tme.security.Html.escape(tuttList.getGenero())%></td>
									<td><%=com.tme.security.Html.escape(tuttList.getEdad())%></td>
									<td><%=com.tme.security.Html.escape(tuttList.getFtutoria())%></td>
									<%-- <td><%=com.tme.security.Html.escape(tuttList.getEmail())%></td> --%>
									<td><%=com.tme.security.Html.escape(tuttList.getTelefono())%></td>
									<td><%=com.tme.security.Html.escape(tuttList.getExperiencia())%></td>
									<td><%=com.tme.security.Html.escape(docente.getNombre())%></td>
									<%-- <td><%=com.tme.security.Html.escape(tuttList.getDireccion())%></td> --%>
									<%-- <td><%=com.tme.security.Html.escape(tuttList.getUserId())%></td> --%>
									<td>
										<%
										if ("Pendiente".equals(tuttList.getEstado())) {
										%> <a href="" class="btn btn-sm btn-danger">Pendiente</a> <%
 } else {
 %> <%=com.tme.security.Html.escape(tuttList.getEstado())%> <%
 }
 %>
									</td>


								</tr>


								<%
								}
								%>


							</tbody>
						</table>




					</div>
				</div>

			</div>

			<!-- col-1 -->
			<div class="col-md-3 p-3">
				<!-- for Background image -->
				<!-- <img alt="" src="img/lt02.jpg" width="500px" height="400px"> -->
				<img alt="" src="img/lt02.jpg" width="250" height="">
			</div>



		</div>


	</div>

	<!-- 2nd Div -->




</body>
</html>