<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<footer class="tm-footer ${workspace ? 'tm-footer-compact' : ''}">
    <div class="container">
        <c:choose>
            <c:when test="${workspace}"><div class="tm-footer-bottom"><span>TeachMate · Asesorías individuales online</span><a href="${pageContext.request.contextPath}/index.jsp#preguntas">Ayuda y preguntas frecuentes</a><span>Hora de Lima · Perú</span></div></c:when>
            <c:otherwise>
                <div class="tm-footer-grid">
                    <div class="tm-footer-intro"><a class="tm-footer-brand" href="${pageContext.request.contextPath}/index.jsp">TeachMate<span aria-hidden="true">.</span></a><p>Un profesor, tus preguntas y un espacio para avanzar. Asesorías individuales, estés donde estés.</p><span class="tm-footer-mode"><i class="fa-solid fa-laptop" aria-hidden="true"></i> Atención en línea</span></div>
                    <nav aria-label="Explorar TeachMate"><h2>Encuentra tu asesoría</h2><a href="${pageContext.request.contextPath}/oferta.jsp">Cursos disponibles</a><a href="${pageContext.request.contextPath}/index.jsp#profesores">Nuestro equipo</a><a href="${pageContext.request.contextPath}/index.jsp#como-funciona">Cómo funciona</a></nav>
                    <nav aria-label="Ayuda y acceso"><h2>Tu cuenta</h2><c:choose><c:when test="${signedIn}"><a href="${pageContext.request.contextPath}${workspaceHome}">Ir a mi espacio</a></c:when><c:otherwise><a href="${pageContext.request.contextPath}/alumno_login.jsp">Acceso de alumnos</a><a href="${pageContext.request.contextPath}/reg_alumno.jsp">Crear una cuenta</a><a href="${pageContext.request.contextPath}/docente_login.jsp">Acceso de profesores</a></c:otherwise></c:choose><a href="${pageContext.request.contextPath}/index.jsp#preguntas">Preguntas frecuentes</a></nav>
                </div>
                <div class="tm-footer-bottom"><span>© <%=java.time.Year.now(com.tme.service.ReglasTutoria.ZONA)%> TeachMate</span><span>Asesorías individuales · Hora de Lima, Perú</span><c:if test="${not signedIn}"><a href="${pageContext.request.contextPath}/admin_login.jsp">Administración</a></c:if></div>
            </c:otherwise>
        </c:choose>
    </div>
</footer>