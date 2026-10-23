<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.OfertaDAO,com.tme.dao.DocenteDAO,com.tme.db.DBConnection,com.tme.security.Html,java.util.Map,java.util.HashMap" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
Map<String,String> reservaDatos=new HashMap<>();Object guardado=session.getAttribute("tutoriaForm");session.removeAttribute("tutoriaForm");
if(guardado instanceof Map<?,?> datos){for(var entry:datos.entrySet())if(entry.getKey() instanceof String clave && entry.getValue() instanceof String valor)reservaDatos.put(clave,valor);}
if(reservaDatos.isEmpty()){
 for(String key:new String[]{"cursoId","tarifaId"})reservaDatos.put(key,request.getParameter(key));
 reservaDatos.put("docenteNameSelect",request.getParameter("docenteId"));reservaDatos.put("ftutoria",request.getParameter("fecha"));reservaDatos.put("horaInicio",request.getParameter("hora"));
}
int reservaCurso=0,reservaTarifa=0,reservaProfesor=0;
try{reservaCurso=Integer.parseInt(reservaDatos.get("cursoId"));reservaTarifa=Integer.parseInt(reservaDatos.get("tarifaId"));reservaProfesor=Integer.parseInt(reservaDatos.get("docenteNameSelect"));}catch(RuntimeException ignored){}
OfertaDAO reservaOferta=new OfertaDAO(DBConnection.getConn());OfertaDAO.Curso reservaCursoObj=null;OfertaDAO.Tarifa reservaTarifaObj=null;
for(var item:reservaOferta.cursos(true))if(item.id()==reservaCurso)reservaCursoObj=item;
if(reservaCursoObj!=null)for(var item:reservaOferta.tarifas(reservaCurso,true))if(item.id()==reservaTarifa)reservaTarifaObj=item;
var reservaDocente=new DocenteDAO(DBConnection.getConn()).getDocenteById(reservaProfesor);
var reservaPlazos=new com.tme.dao.ConfiguracionReservasDAO(DBConnection.getConn()).obtener();
boolean reservaValida=reservaPlazos.completa()&&new com.tme.dao.ConfiguracionPagosDAO(DBConnection.getConn()).obtener().completa()&&reservaCursoObj!=null&&reservaTarifaObj!=null&&reservaDocente!=null&&reservaDocente.isActivo()&&reservaOferta.cursosDocente(reservaProfesor).contains(reservaCurso);
request.setAttribute("datosTutoria",reservaDatos);
if(reservaValida&&session.getAttribute("userObj")==null){
 try {var fecha=java.time.LocalDate.parse(reservaDatos.get("ftutoria"));var hora=java.time.LocalTime.parse(reservaDatos.get("horaInicio"));session.setAttribute("reservaRetorno","reg_tutoria.jsp?cursoId="+reservaCurso+"&tarifaId="+reservaTarifa+"&docenteId="+reservaProfesor+"&fecha="+fecha+"&hora="+hora);}catch(RuntimeException ignored){}
}
%>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><title>Solicitar asesoría | TeachMate</title><%@ include file="component/allcss.jsp" %></head><body><%@ include file="component/navbar.jsp" %><main id="contenido" class="tm-main" tabindex="-1"><%@ include file="component/messages.jsp" %><div class="container p-3"><div class="tm-page-heading"><div><p class="tm-eyebrow">Tu próxima asesoría</p><h1 class="tm-page-title">Revisa tu solicitud</h1><p>El horario y la tarifa se verifican de nuevo al enviar.</p></div><a class="btn btn-outline-secondary" href="oferta.jsp">Cambiar curso u horario</a></div>
<% if(!reservaValida){ %><div class="alert alert-info">Selecciona un curso, una tarifa y un horario disponible para continuar.</div><a class="btn btn-primary" href="oferta.jsp">Ver cursos y horarios</a><% }else{ %>
<div class="tm-management-grid"><aside class="tm-form-aside"><p class="tm-eyebrow">Asesoría individual online</p><h2><%=Html.escape(reservaCursoObj.nombre())%></h2><p><strong><%=Html.escape(reservaDocente.getNombre())%></strong></p><p><c:out value="${datosTutoria.ftutoria}"/> · <c:out value="${datosTutoria.horaInicio}"/> (Lima)</p><p><%=reservaTarifaObj.minutos()%> minutos</p><p class="tm-offer-price">S/ <%=reservaTarifaObj.precio()%></p><p>Precio total de la asesoría. Enviar la solicitud no registra un pago.</p></aside>
<section class="card"><div class="card-body"><c:choose><c:when test="${empty sessionScope.userObj}"><p>Para enviar la solicitud necesitas una cuenta de alumno.</p><a class="btn btn-primary" href="alumno_login.jsp">Iniciar sesión como alumno</a><a class="btn btn-outline-secondary" href="reg_alumno.jsp">Crear cuenta</a></c:when><c:otherwise>
<p class="form-text">El profesor tendrá <%=reservaPlazos.respuesta()%> minutos para responder. Si acepta, el plazo de pago será de <%=reservaPlazos.pago()%> minutos. La confirmación requiere verificar el ingreso. El pago se realiza fuera del sistema y se adjunta un comprobante privado. Si administración lo observa, tendrás el plazo de corrección acordado, sin superar el inicio de la clase.</p><p class="form-text">Los cambios de horario requieren aceptación de ambos. Las cancelaciones de sesiones confirmadas pasan por administración; no hay penalizaciones automáticas.</p><form action="addTutoria" method="post" class="row g-3"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="cursoId" value="<%=reservaCurso%>"><input type="hidden" name="tarifaId" value="<%=reservaTarifa%>"><input type="hidden" name="docenteNameSelect" value="<%=reservaProfesor%>"><input type="hidden" name="duracionMinutos" value="<%=reservaTarifaObj.minutos()%>">
<div class="col-md-6"><label class="form-label" for="nombre">Nombre completo</label><input class="form-control" id="nombre" name="nombre" maxlength="45" required value="<c:out value='${sessionScope.userObj.nombre}'/>" readonly></div>
<div class="col-md-6"><label class="form-label" for="email">Correo electrónico</label><input class="form-control" id="email" name="email" type="email" required value="<c:out value='${sessionScope.userObj.email}'/>" readonly></div>
<div class="col-md-6"><label class="form-label" for="fecha">Fecha</label><input class="form-control" id="fecha" name="ftutoria" type="date" required readonly value="<c:out value='${datosTutoria.ftutoria}'/>"></div>
<div class="col-md-6"><label class="form-label" for="hora">Hora de Lima</label><input class="form-control" id="hora" name="horaInicio" type="time" required readonly value="<c:out value='${datosTutoria.horaInicio}'/>"></div>
<div class="col-12"><label class="form-label" for="nivel">Nivel de estudio</label><input class="form-control" id="nivel" name="nivel" required maxlength="150" placeholder="Por ejemplo: secundaria, preuniversitario, universidad" value="<c:out value='${datosTutoria.nivel}'/>"></div>
<div class="col-12"><label class="form-label" for="tema">Tema que necesitas reforzar</label><input class="form-control" id="tema" name="experiencia" required maxlength="500" value="<c:out value='${datosTutoria.experiencia}'/>"></div>
<div class="col-12"><p class="form-text">Usaremos el teléfono de tu perfil: <c:out value="${sessionScope.userObj.telefono}"/>. <a href="perfil.jsp">Actualizar mis datos</a>.</p></div>
<div class="col-12"><c:if test="${empty sessionScope.userObj.telefono}"><p class="alert alert-info">Completa tu teléfono en <a href="perfil.jsp">Mi perfil</a> antes de enviar.</p></c:if><button class="btn btn-primary" type="submit" ${empty sessionScope.userObj.telefono ? 'disabled' : ''}>Enviar solicitud</button><p class="form-text">Podrás consultar la respuesta del profesor en Mis asesorías.</p></div>
</form></c:otherwise></c:choose></div></section></div><% } %></div></main><%@ include file="component/footer.jsp" %></body></html>
