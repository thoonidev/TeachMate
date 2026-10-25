<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.OfertaDAO,com.tme.dao.DocenteDAO,com.tme.db.DBConnection,com.tme.security.Html,com.tme.modal.User,java.util.List,java.time.LocalDate,java.time.LocalTime,java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
var condiciones=new com.tme.dao.ConfiguracionReservasDAO(DBConnection.getConn()).obtener();
boolean reservasHabilitadas=condiciones.completa()&&new com.tme.dao.ConfiguracionPagosDAO(DBConnection.getConn()).obtener().completa();
OfertaDAO oferta=new OfertaDAO(DBConnection.getConn());
var cursos=oferta.cursos(true);
int cursoId=0,tarifaId=0,docenteFiltro=0;
LocalDate fechaConsulta=null;
try {
    if(request.getParameter("cursoId")!=null)cursoId=Integer.parseInt(request.getParameter("cursoId"));
    if(request.getParameter("tarifaId")!=null)tarifaId=Integer.parseInt(request.getParameter("tarifaId"));
    if(request.getParameter("docenteId")!=null)docenteFiltro=Integer.parseInt(request.getParameter("docenteId"));
    if(request.getParameter("fecha")!=null&&!request.getParameter("fecha").isBlank())fechaConsulta=LocalDate.parse(request.getParameter("fecha"));
} catch(RuntimeException e){response.sendError(400);return;}
OfertaDAO.Curso elegido=null;
for(var c:cursos)if(c.id()==cursoId)elegido=c;
var tarifas=elegido==null?List.<OfertaDAO.Tarifa>of():oferta.tarifas(cursoId,true);
OfertaDAO.Tarifa elegida=null;
for(var t:tarifas)if(t.id()==tarifaId)elegida=t;
if(elegida==null&&!tarifas.isEmpty())elegida=tarifas.get(0);
int alumnoId=session.getAttribute("userObj") instanceof User alumno?alumno.getId():0;
var formatoFecha=DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy",java.util.Locale.forLanguageTag("es-PE"));
var formatoHora=DateTimeFormatter.ofPattern("HH:mm");
%>
<!doctype html>
<html lang="es"><head><meta charset="UTF-8"><title>Cursos, profesores y horarios | TeachMate</title><%@ include file="component/allcss.jsp" %></head>
<body><%@ include file="component/navbar.jsp" %>
<main id="contenido" class="tm-main" tabindex="-1"><%@ include file="component/messages.jsp" %>
<div class="container p-3">
    <div class="tm-page-heading"><div><p class="tm-eyebrow">Encuentra tu asesoría</p><h1 class="tm-page-title">Curso, profesor y horario</h1><p>Elige curso y duración; después selecciona una fecha y un horario con tu profesor. Precios en soles y horas de Lima.</p></div></div>
    <% if(!reservasHabilitadas){ %>
    <p class="alert alert-info">El equipo está configurando las condiciones de reserva y cobro. Los horarios se habilitarán al completar la configuración.</p>
    <% }else{ %>
    <p class="alert alert-info">Reserva con al menos <strong><%=condiciones.antelacion()%> minutos (<%=condiciones.antelacion()/60%> h <%=condiciones.antelacion()%60%> min)</strong> de anticipación. Ahora puedes solicitar clases desde el <strong><%=com.tme.service.ReglasTutoria.ahora().plusMinutes(condiciones.antelacion()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))%></strong> (Lima), según disponibilidad. Los plazos incluyen noches y fines de semana.</p>
    <% } %>
    <% if(docenteFiltro>0){ %><p><a href="oferta.jsp">Consultar todos los profesores</a></p><% } %>
    <section class="card mb-4"><div class="card-body">
        <form method="get" class="row g-3">
            <% if(docenteFiltro>0){ %><input type="hidden" name="docenteId" value="<%=docenteFiltro%>"><% } %>
            <div class="col-md-5"><label class="form-label" for="cursoId">Curso</label><select class="form-select" id="cursoId" name="cursoId" required><option value="">Selecciona un curso</option>
            <% for(var c:cursos){if(docenteFiltro>0&&!oferta.cursosDocente(docenteFiltro).contains(c.id()))continue; %>
                <option value="<%=c.id()%>" <%=c.id()==cursoId?"selected":""%>><%=Html.escape(c.nombre())%></option>
            <% } %></select></div>
            <div class="col-md-4"><label class="form-label" for="tarifaId">Duración y precio</label><select class="form-select" id="tarifaId" name="tarifaId"><option value="0">Ver opciones del curso</option>
            <% for(var t:tarifas){ %><option value="<%=t.id()%>" <%=elegida!=null&&t.id()==elegida.id()?"selected":""%>><%=t.minutos()%> minutos · S/ <%=t.precio()%></option><% } %>
            </select></div>
            <div class="col-md-3 d-flex align-items-end"><button class="btn btn-primary" type="submit">Ver horarios</button></div>
        </form>
    </div></section>
    <% if(elegido==null){ %><p class="tm-empty">Selecciona el curso que necesitas reforzar.</p><% }else{ %>
    <h2 class="tm-section-title"><%=Html.escape(elegido.nombre())%></h2><p class="text-muted"><%=Html.escape(elegido.descripcion())%></p>
    <% if(elegida==null){ %><p class="alert alert-info">Este curso todavía no tiene tarifas habilitadas. No se pueden enviar nuevas solicitudes.</p>
    <% }else{boolean hayProfesor=false; %>
    <p class="text-muted small">Clase de <%=elegida.minutos()%> minutos · Precio total: S/ <%=elegida.precio()%>. Cada opción muestra la hora de inicio y fin.</p>
    <div class="tm-teacher-grid">
    <% for(var profesor:new DocenteDAO(DBConnection.getConn()).getActiveDocentes()){
        if(docenteFiltro>0&&profesor.getId()!=docenteFiltro)continue;
        if(!oferta.cursosDocente(profesor.getId()).contains(cursoId))continue;
        hayProfesor=true;
        var horarios=oferta.horarios(profesor.getId(),elegida.minutos(),alumnoId);
        var fechas=horarios.stream().map(OfertaDAO.Horario::fecha).distinct().toList();
        LocalDate fechaProfesor=fechaConsulta!=null&&fechas.contains(fechaConsulta)?fechaConsulta:(fechas.isEmpty()?null:fechas.get(0));
    %>
    <article class="tm-teacher-card" id="profesor-<%=profesor.getId()%>">
        <h3><%=Html.escape(profesor.getNombre())%></h3><p class="tm-teacher-subject"><%=Html.escape(profesor.getCalificacion())%></p>
        <p><%=Html.escape(profesor.getPresentacion())%></p><p class="small text-muted"><%=Html.escape(profesor.getNiveles())%></p>
        <h4 class="h6">Elige fecha y horario</h4>
        <% if(horarios.isEmpty()){ %>
        <p class="text-muted small"><%=reservasHabilitadas?"No hay horarios reservables para esta duración. Las franjas deben permitir completar la clase, cumplir la anticipación mínima y estar libres para ti y el profesor. Consulta otra duración o vuelve cuando se publiquen nuevas fechas.":"Los horarios estarán disponibles cuando el equipo complete las condiciones de reserva y cobro."%></p>
        <% }else{ %>
        <form method="get" action="oferta.jsp#profesor-<%=profesor.getId()%>" class="mb-3">
            <input type="hidden" name="cursoId" value="<%=cursoId%>"><input type="hidden" name="tarifaId" value="<%=elegida.id()%>">
            <% if(docenteFiltro>0){ %><input type="hidden" name="docenteId" value="<%=docenteFiltro%>"><% } %>
            <label class="form-label" for="fecha-<%=profesor.getId()%>">Fechas con horarios disponibles</label>
            <select class="form-select mb-2" id="fecha-<%=profesor.getId()%>" name="fecha">
                <% for(var fecha:fechas){ %><option value="<%=fecha%>" <%=fecha.equals(fechaProfesor)?"selected":""%>><%=fecha.format(formatoFecha)%></option><% } %>
            </select><button class="btn btn-outline-primary btn-sm" type="submit">Ver esta fecha</button>
        </form>
        <p><strong><%=fechaProfesor.format(formatoFecha)%></strong> · Lima</p>
        <% for(boolean tarde:new boolean[]{false,true}){boolean hayTurno=false;
            for(var horario:horarios){if(!horario.fecha().equals(fechaProfesor)||(!horario.inicio().isBefore(LocalTime.NOON))!=tarde)continue;
                if(!hayTurno){hayTurno=true; %><h5 class="h6"><%=tarde?"Tarde y noche":"Mañana"%></h5><div class="tm-slot-list"><% } %>
                <a class="tm-slot" href="reg_tutoria.jsp?cursoId=<%=cursoId%>&amp;tarifaId=<%=elegida.id()%>&amp;docenteId=<%=profesor.getId()%>&amp;fecha=<%=horario.fecha()%>&amp;hora=<%=horario.inicio()%>"><strong><%=horario.inicio().format(formatoHora)%>–<%=horario.inicio().plusMinutes(elegida.minutos()).format(formatoHora)%></strong></a>
            <% }if(hayTurno){ %></div><% }
        } %>
        <p class="form-text">Se muestran todas las opciones de esta fecha. Seleccionar una todavía no retiene el horario; se verifica al enviar la solicitud.</p>
        <% } %>
    </article>
    <% } %></div>
    <% if(!hayProfesor){ %><p class="tm-empty">No hay profesores activos asignados a este curso.</p><% } %>
    <% } } %>
</div></main><%@ include file="component/footer.jsp" %></body></html>
