<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.CorreoDAO,com.tme.db.DBConnection,com.tme.service.ConfiguracionCorreo,com.tme.service.ReglasTutoria,com.tme.security.Html,java.util.Set" %>
<%
ConfiguracionCorreo configuracionCorreo=null;
boolean configuracionInvalida=false;
try{configuracionCorreo=ConfiguracionCorreo.entorno();}catch(IllegalArgumentException e){configuracionInvalida=true;}
var colaCorreo=new CorreoDAO(DBConnection.getConn());colaCorreo.mantener(ReglasTutoria.ahora());
var avisosCorreo=colaCorreo.listar();int aceptadosCorreo=colaCorreo.aceptadosHoy(ReglasTutoria.ahora());
%>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><title>Correos y avisos | TeachMate</title><%@ include file="../component/allcss.jsp" %></head>
<body><%@ include file="../component/navbar.jsp" %><main id="contenido" class="tm-main" tabindex="-1"><%@ include file="../component/messages.jsp" %><div class="container p-3">
<div class="tm-page-heading"><div><p class="tm-eyebrow">Comunicación del equipo</p><h1 class="tm-page-title">Correos y avisos</h1><p>Consulta los avisos de reservas, pagos y recordatorios. La reserva conserva su estado aunque falle un correo.</p></div><a class="btn btn-outline-secondary" href="configuracion.jsp">Condiciones de reserva</a></div>
<% if(configuracionInvalida){ %><p class="alert alert-warning">La configuración de correo está incompleta o es inválida. Revisa la configuración del servicio.</p>
<% }else if(!configuracionCorreo.habilitado()){ %><p class="alert alert-info">El envío está desactivado. Los avisos quedan en espera y caducan cuando dejan de ser útiles.</p>
<% }else{ %><p class="alert alert-info">Envío activado mediante Brevo. Aceptados hoy: <strong><%=aceptadosCorreo%> / <%=configuracionCorreo.limiteDiario()%></strong>. Los pendientes continúan al día siguiente si alcanzas el límite, siempre que sigan vigentes.</p><% } %>
<p class="form-text">“Aceptado” significa que el proveedor recibió la solicitud; no confirma la entrega al destinatario. Los avisos dudosos requieren comprobar el envío en Brevo antes de reintentarlos. Los recordatorios requieren que el servicio esté activo.</p>
<section class="card"><div class="card-body"><h2 class="tm-section-title">Últimos 100 avisos</h2><div class="table-responsive"><table class="table"><thead><tr><th>Aviso</th><th>Destinatario</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>
<% if(avisosCorreo.isEmpty()){ %><tr><td colspan="4">Todavía no hay avisos registrados.</td></tr><% }for(var aviso:avisosCorreo){ %>
<tr><td><strong><%=Html.escape(aviso.asunto())%></strong><br><a href="../asesoria.jsp?id=<%=aviso.tutoria()%>">Ver asesoría #<%=aviso.tutoria()%></a></td><td><%=Html.escape(aviso.destino())%></td>
<td><strong><%=Html.escape(aviso.estado())%></strong><br><%=aviso.intentos()%> intento(s)<br><span class="small"><%=Html.escape(aviso.error())%></span><% if(aviso.estado().equals("Pendiente")){ %><br>Próximo intento: <%=aviso.proximo().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))%> (Lima)<% } %></td>
<td><% if(Set.of("Fallido","Dudoso").contains(aviso.estado())){ %><form action="../gestionarCorreo" method="post" class="mb-2"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="id" value="<%=aviso.id()%>"><input type="hidden" name="accion" value="reintentar">
<% if(aviso.estado().equals("Dudoso")){ %><label class="form-check mb-2"><input class="form-check-input" type="checkbox" name="confirmar" value="true" required><span class="form-check-label">Revisé Brevo y acepto que reintentar podría duplicar el correo.</span></label><% } %><button class="btn btn-outline-primary btn-sm" type="submit">Reintentar</button></form><% } %>
<% if(Set.of("Pendiente","Fallido","Dudoso").contains(aviso.estado())){ %><form action="../gestionarCorreo" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="id" value="<%=aviso.id()%>"><input type="hidden" name="accion" value="cancelar"><button class="btn btn-outline-danger btn-sm" type="submit">Cancelar aviso</button></form><% } %></td></tr><% } %>
</tbody></table></div></div></section></div></main><%@ include file="../component/footer.jsp" %></body></html>
