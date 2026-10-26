<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.OfertaDAO,com.tme.db.DBConnection,com.tme.security.Html,java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% OfertaDAO oferta=new OfertaDAO(DBConnection.getConn()); List<OfertaDAO.Curso> cursos=oferta.cursos(false);
   int seleccionado=0;try{seleccionado=Integer.parseInt(request.getParameter("cursoId"));}catch(RuntimeException ignored){}
   OfertaDAO.Curso curso=null;for(var item:cursos)if(item.id()==seleccionado)curso=item;
%>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><title>Cursos y tarifas | TeachMate</title><%@ include file="../component/allcss.jsp" %></head>
<body><%@ include file="../component/navbar.jsp" %><main id="contenido" class="tm-main" tabindex="-1"><%@ include file="../component/messages.jsp" %>
<div class="container p-3"><div class="tm-page-heading"><div><p class="tm-eyebrow">Oferta académica</p><h1 class="tm-page-title">Cursos y tarifas</h1><p>Define qué ofrece el equipo, cuánto cuesta cada duración y qué porcentaje corresponde al profesor.</p></div><a class="btn btn-outline-secondary" href="cursos.jsp">Nuevo curso</a></div>
<div class="tm-management-grid"><section class="card"><div class="card-body"><h2 class="tm-section-title">Catálogo</h2>
<% if(cursos.isEmpty()){ %><p class="tm-empty">Crea el primer curso para comenzar.</p><% } %>
<div class="tm-course-list"><% for(var item:cursos){ %><a class="<%=item.id()==seleccionado?"active":""%>" href="cursos.jsp?cursoId=<%=item.id()%>"><strong><%=Html.escape(item.nombre())%></strong><span class="tm-status <%=item.activo()?"tm-status-activo":"tm-status-inactivo"%>"><%=item.activo()?"Activo":"Inactivo"%></span></a><% } %></div>
</div></section><div>
<section class="card mb-4"><div class="card-body"><h2 class="tm-section-title"><%=curso==null?"Crear curso":"Editar curso"%></h2>
<form method="post" action="../guardarCurso" class="row g-3"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="id" value="<%=curso==null?0:curso.id()%>">
<div class="col-12"><label class="form-label" for="nombre">Nombre del curso</label><input class="form-control" id="nombre" name="nombre" required maxlength="45" value="<%=curso==null?"":Html.escape(curso.nombre())%>"></div>
<div class="col-12"><label class="form-label" for="descripcion">Descripción pública</label><textarea class="form-control" id="descripcion" name="descripcion" maxlength="500"><%=curso==null?"":Html.escape(curso.descripcion())%></textarea></div>
<div class="col-12"><label><input type="checkbox" name="activo" <%=curso==null||curso.activo()?"checked":""%>> Curso activo para nuevas solicitudes</label><p class="form-text">Desactivar conserva profesores asignados, tarifas e historial.</p></div><div class="col-12"><button class="btn btn-primary" type="submit">Guardar curso</button></div></form>
</div></section>
<% if(curso!=null){ %><section class="card"><div class="card-body"><h2 class="tm-section-title">Tarifas de <%=Html.escape(curso.nombre())%></h2><p class="text-muted small">Una tarifa por duración. Al cambiarla se crea una versión nueva. Los precios están en soles (PEN).</p>
<% if(curso.activo()){ %><form method="post" action="../guardarTarifa" class="row g-3 mb-4"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="cursoId" value="<%=curso.id()%>">
<div class="col-md-4"><label class="form-label" for="minutos">Duración de la clase (min)</label><input class="form-control" id="minutos" name="minutos" type="number" min="15" max="180" required><p class="form-text">60 min = 1 hora · 90 min = 1 hora y media.</p></div>
<div class="col-md-4"><label class="form-label" for="precio">Precio total (S/)</label><input class="form-control" id="precio" name="precio" type="number" step="0.01" min="0.01" max="9999999.99" required></div>
<div class="col-md-4"><label class="form-label" for="porcentaje">Parte del profesor (%)</label><input class="form-control" id="porcentaje" name="porcentaje" type="number" min="0" max="100" step="0.01" required></div>
<div class="col-12"><p class="form-text">El resto corresponde a la operación del equipo. No se configura ningún reparto por defecto.</p><button type="submit" class="btn btn-primary">Guardar nueva tarifa</button></div></form><% } %>
<div class="tm-offer-items"><% List<OfertaDAO.Tarifa> tarifas=oferta.tarifas(curso.id(),false);if(tarifas.isEmpty()){ %><p class="tm-empty">Sin tarifas. Este curso todavía no permite nuevas reservas.</p><% }for(var tarifa:tarifas){ %><div class="tm-offer-item"><div><strong><%=com.tme.service.TiempoVisible.duracion(tarifa.minutos())%> · S/ <%=tarifa.precio()%></strong><span>Profesor: <%=tarifa.porcentaje()%> % · Versión #<%=tarifa.id()%> · <%=tarifa.activa()?"Activa":"Histórica"%></span></div><% if(tarifa.activa()){ %><form method="post" action="../retirarTarifa"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="cursoId" value="<%=curso.id()%>"><input type="hidden" name="tarifaId" value="<%=tarifa.id()%>"><button class="btn btn-outline-danger btn-sm" type="submit">Retirar tarifa</button></form><% } %></div><% } %></div>
</div></section><% } %></div></div></div></main><%@ include file="../component/footer.jsp" %></body></html>
