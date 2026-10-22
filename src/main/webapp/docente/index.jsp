<%@page import="com.tme.modal.Docente"%>
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
<title>Vista Docente</title>
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


	<!-- compruebe si el docente ha iniciado sesión o no, si inicia sesión, solo él podrá acceder al panel de docentes -->
	<!-- de lo contrario redirigirlo a la página de inicio de sesión del docente para iniciar sesión -->
	<!-- si "docenteObj" está vacío significa que nadie ha iniciado sesión. -->

	<c:if test="${empty docenteObj }">

		<c:redirect url="../docente_login.jsp"></c:redirect>

	</c:if>

	<!-- Comprobar si el docente está conectado o no -->


	<div class="container p-5">
		<p class="text-center text-primary fs-3">Panel Docente</p>

		<%
		
		
		DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());
		int totalNumberOfDocente = docDAO.countTotalDocente();
		
		//obtiene el objeto docente de inicio de sesión actual de la sesión
		Docente currentLoginDocente = (Docente)session.getAttribute("docenteObj");
		
		
		%>

		<div class="row">
			<div class="col-md-4 offset-md-2">
				<div class="card my-card">
					<div class="card-body text-center text-primary">
						<i class="fa-solid fa-chalkboard-user fa-3x"></i><br>
						<p class="fs-4 text-center">
							Profesor <br><%=com.tme.security.Html.escape(totalNumberOfDocente)%>
						</p>
					</div>
				</div>

			</div>

			<div class="col-md-4">
				<div class="card my-card">
					<div class="card-body text-center text-primary">
						<i class="fa-solid fa-calendar-check fa-3x"></i><br>
						<p class="fs-4 text-center">
							Tutoria total <br> <%=com.tme.security.Html.escape(docDAO.countTotalTutoriaByDocenteId(currentLoginDocente.getId()))%>
						</p>
					</div>
				</div>

			</div>
		</div>


	</div>




</body>
</html>