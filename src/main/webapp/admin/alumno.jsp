<%@page import="com.tme.modal.Docente"%>
<%@page import="com.tme.dao.DocenteDAO"%>
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
<title>Detalles | Alumno</title>

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

	<div class="col-md-12 p-5">
		<div class="card my-card">
			<div class="card-body">
				<p class="text-center text-dark fs-3">Detalles del Alumno</p>

				<table class="table table-info table-striped">
					<thead>
						<tr class="table">
							<th scope="col" style="padding-right:70px">Nombres</th>
							<th scope="col">Genero</th>
							<th scope="col">Edad</th>
							<th scope="col">Tutoria</th>
							<th scope="col">Email</th>
							<th scope="col">Telefono</th>
							<th scope="col" style="padding-right:50px">Experiencia</th>
							<th scope="col" style="padding-right:70px">Nombre/Docente</th>
							<th scope="col">Direccion</th>
							<th scope="col" style="padding-right:100px">Estado</th>

						</tr>
					</thead>
					<tbody>

						<%
						TutoriaDAO tutDAO = new TutoriaDAO(DBConnection.getConn());
										DocenteDAO docDAO = new DocenteDAO(DBConnection.getConn());
										List<Tutoria> list = tutDAO.getAllTutoria();
										for(Tutoria tutList : list)
										{
											Docente docente =	docDAO.getDocenteById(tutList.getDocenteId());
						%>
						
						<tr>
							<th><%=com.tme.security.Html.escape(tutList.getNombre())%></th>
							<td><%=com.tme.security.Html.escape(tutList.getGenero())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getEdad())%></td>
							<%-- <td><%=com.tme.security.Html.escape(tutList.getNombre())%></td> --%>
							<td><%=com.tme.security.Html.escape(tutList.getFtutoria())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getEmail())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getTelefono())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getExperiencia())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getNombre())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getDireccion())%></td>
							<td><%=com.tme.security.Html.escape(tutList.getEstado())%></td>

						</tr>
						
						
						<%
						}
						%>

						
					</tbody>

				</table>

			</div>


		</div>

	</div>




</body>
</html>