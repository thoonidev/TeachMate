<%@ page pageEncoding="UTF-8" %>
<%
var disponibilidadPlazos=new com.tme.dao.ConfiguracionReservasDAO(com.tme.db.DBConnection.getConn()).obtener();
boolean disponibilidadHabilitada=disponibilidadPlazos.completa()&&new com.tme.dao.ConfiguracionPagosDAO(com.tme.db.DBConnection.getConn()).obtener().completa();
var disponibilidadAhora=com.tme.service.ReglasTutoria.ahora();
var disponibilidadMinimo=disponibilidadPlazos.completa()?disponibilidadAhora.plusMinutes(disponibilidadPlazos.antelacion()):null;
java.util.Set<Integer> disponibilidadDuraciones=new java.util.TreeSet<>();
for(var cursoDisponible:oferta.cursos(true)){if(!oferta.cursosDocente(ofertaDocenteId).contains(cursoDisponible.id()))continue;
    for(var tarifaDisponible:oferta.tarifas(cursoDisponible.id(),true))disponibilidadDuraciones.add(tarifaDisponible.minutos());
}
var disponibilidadReservas=new com.tme.dao.TutoriaDAO(com.tme.db.DBConnection.getConn()).reservasActivas(ofertaDocenteId,0);
%>
<section class="card"><div class="card-body"><h2 class="tm-section-title">Disponibilidad por fecha</h2><p class="text-muted small">Publicar una franja compromete al profesor a atender las reservas de sus cursos en ese horario, sin una aceptación adicional. Añade franjas en hora de Lima. No se repiten automáticamente cada semana. Retirar una franja impide nuevas reservas y conserva las asesorías ya registradas.</p>
<% if(disponibilidadMinimo!=null){ %><p class="alert alert-info">Las nuevas reservas requieren <strong><%=disponibilidadPlazos.antelacion()%> minutos (<%=disponibilidadPlazos.antelacion()/60%> h <%=disponibilidadPlazos.antelacion()%60%> min)</strong> de anticipación. En este momento solo se puede solicitar una clase desde el <strong><%=disponibilidadMinimo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))%></strong> (Lima). Publica preferentemente la semana siguiente. Una franja fuera de plazo no cancela las asesorías ya reservadas.</p><% }else{ %><p class="alert alert-info">Administración debe completar las condiciones para habilitar nuevas reservas.</p><% } %>
<form class="row g-3 mb-4" method="post" action="${pageContext.request.contextPath}<%=disponibilidadAction%>"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="docenteId" value="<%=ofertaDocenteId%>"><input type="hidden" name="accion" value="agregar">
<div class="col-md-4"><label class="form-label" for="fecha">Fecha</label><input class="form-control" id="fecha" name="fecha" type="date" min="<%=java.time.LocalDate.now(com.tme.service.ReglasTutoria.ZONA)%>" required></div>
<div class="col-md-4"><label class="form-label" for="desde">Desde</label><input class="form-control" id="desde" name="desde" type="time" required></div><div class="col-md-4"><label class="form-label" for="hasta">Hasta</label><input class="form-control" id="hasta" name="hasta" type="time" required></div><div class="col-12"><button class="btn btn-primary" type="submit">Añadir franja</button></div></form>
<div class="tm-offer-items"><% var franjas=oferta.franjas(ofertaDocenteId);if(franjas.isEmpty()){ %><p class="tm-empty">No hay disponibilidad publicada. Añade una fecha y un intervalo de horas.</p><% }for(var franja:franjas){
    boolean franjaEnPlazo=false;
    if(disponibilidadMinimo!=null){for(int disponibilidadDuracion:disponibilidadDuraciones){
        long bloques=java.time.Duration.between(franja.desde(),franja.hasta()).toMinutes()/disponibilidadDuracion;
        franjaEnPlazo=franjaEnPlazo||(bloques>0&&!franja.fecha().atTime(franja.desde()).plusMinutes((bloques-1)*disponibilidadDuracion).isBefore(disponibilidadMinimo));}
    }
    int reservasEnFranja=0;
    for(var reservaFranja:disponibilidadReservas){
        var inicioFranja=com.tme.service.ReglasTutoria.inicio(reservaFranja);
        if(inicioFranja!=null&&inicioFranja.toLocalDate().equals(franja.fecha())&&inicioFranja.toLocalTime().isBefore(franja.hasta())&&inicioFranja.plusMinutes(reservaFranja.getDuracionMinutos()).toLocalTime().isAfter(franja.desde()))reservasEnFranja++;
    }
%><div class="tm-offer-item"><div><strong><%=franja.fecha()%></strong><span><%=franja.desde()%> a <%=franja.hasta()%> · Lima</span><span class="small"><%=!disponibilidadHabilitada?"Condiciones de reserva o cobro pendientes":disponibilidadDuraciones.isEmpty()?"Falta una tarifa activa para ofrecer clases":franjaEnPlazo?"Dentro del plazo: las opciones dependen de la duración y de los horarios libres":"Sin nuevos turnos: la franja no permite una clase completa con la anticipación requerida"%></span><span class="small"><%=reservasEnFranja%> reservas o retenciones activas en esta franja · <a href="alumno.jsp">Consultar asesorías</a></span></div><form method="post" action="${pageContext.request.contextPath}<%=disponibilidadAction%>"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="docenteId" value="<%=ofertaDocenteId%>"><input type="hidden" name="franjaId" value="<%=franja.id()%>"><input type="hidden" name="accion" value="retirar"><button class="btn btn-outline-danger btn-sm" type="submit">Retirar franja</button></form></div><% } %></div>
</div></section>
