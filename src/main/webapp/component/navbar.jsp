<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<c:set var="currentPage" value="${pageContext.request.servletPath}" />
<c:set var="signedIn" value="${not empty sessionScope.adminObj or not empty sessionScope.docenteObj or not empty sessionScope.userObj}" />
<c:set var="workspace" value="${signedIn and currentPage ne '/index.jsp' and currentPage ne '/' and currentPage ne '/alumno_login.jsp' and currentPage ne '/docente_login.jsp' and currentPage ne '/admin_login.jsp' and currentPage ne '/reg_alumno.jsp'}" />
<c:choose>
    <c:when test="${not empty sessionScope.adminObj}">
        <c:set var="workspaceHome" value="/admin/index.jsp" /><c:set var="accountName" value="Mi cuenta" /><c:set var="accountRole" value="Administrador" /><c:set var="logoutAction" value="/adminLogout" />
    </c:when>
    <c:when test="${not empty sessionScope.docenteObj}">
        <c:set var="workspaceHome" value="/docente/index.jsp" /><c:set var="accountName" value="${sessionScope.docenteObj.nombre}" /><c:set var="accountRole" value="Profesor" /><c:set var="logoutAction" value="/docenteLogout" />
    </c:when>
    <c:otherwise>
        <c:set var="workspaceHome" value="/list_tutoria.jsp" /><c:set var="accountName" value="${sessionScope.userObj.nombre}" /><c:set var="accountRole" value="Alumno" /><c:set var="logoutAction" value="/userLogout" />
    </c:otherwise>
</c:choose>
<a class="tm-skip-link" href="#contenido">Saltar al contenido</a>
<header class="tm-header">
<nav class="navbar navbar-expand-lg navbar-light tm-navbar" aria-label="Navegación principal">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}${signedIn ? workspaceHome : '/index.jsp'}"><span class="tm-brand-icon" aria-hidden="true"><i class="fa-solid fa-book-open"></i></span>TeachMate<span class="tm-brand-dot" aria-hidden="true">.</span></a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="${workspace ? '#workspaceNav' : '#teachmateNav'}" aria-controls="${workspace ? 'workspaceNav' : 'teachmateNav'}" aria-expanded="false" aria-label="Abrir menú de navegación"><span class="navbar-toggler-icon"></span></button>
        <div class="${workspace ? 'tm-workspace-tools' : 'collapse navbar-collapse'}" id="teachmateNav">
            <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-2">
                <c:choose>
                    <c:when test="${workspace}"><li class="nav-item tm-site-link"><a class="nav-link" href="${pageContext.request.contextPath}/index.jsp">Ver sitio <i class="fa-solid fa-arrow-up-right-from-square ms-1" aria-hidden="true"></i></a></li></c:when>
                    <c:otherwise>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/index.jsp#cursos">Cursos</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/index.jsp#profesores">Profesores</a></li>
                        <li class="nav-item"><a class="nav-link" href="${pageContext.request.contextPath}/index.jsp#como-funciona">Cómo funciona</a></li>
                        <c:if test="${not signedIn}"><li class="nav-item"><a class="nav-link ${currentPage eq '/alumno_login.jsp' ? 'active' : ''}" href="${pageContext.request.contextPath}/alumno_login.jsp">Iniciar sesión</a></li><li class="nav-item"><a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/reg_alumno.jsp">Crear cuenta <i class="fa-solid fa-arrow-right ms-1" aria-hidden="true"></i></a></li></c:if>
                    </c:otherwise>
                </c:choose>
                <c:if test="${signedIn}"><li class="nav-item">
                    <details class="tm-account">
                        <summary aria-label="Abrir menú de cuenta"><span class="tm-account-icon" aria-hidden="true"><i class="fa-regular fa-user"></i></span><span class="tm-account-name"><c:out value="${accountName}" /></span><i class="fa-solid fa-chevron-down" aria-hidden="true"></i></summary>
                        <div class="tm-account-menu"><p class="tm-account-label"><c:out value="${accountRole}" /></p><a href="${pageContext.request.contextPath}${workspaceHome}">Mi espacio</a>
                            <c:if test="${not empty sessionScope.docenteObj}"><a href="${pageContext.request.contextPath}/docente/edit_profile.jsp">Perfil y contraseña</a></c:if>
                            <c:if test="${not empty sessionScope.userObj}"><a href="${pageContext.request.contextPath}/perfil.jsp">Mi perfil</a></c:if>
                            <form action="${pageContext.request.contextPath}${logoutAction}" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button type="submit">Cerrar sesión</button></form>
                        </div>
                    </details>
                </li></c:if>
            </ul>
        </div>
    </div>
</nav>
</header>
<c:if test="${workspace}">
<aside class="tm-sidebar collapse d-lg-flex" id="workspaceNav">
    <p class="tm-sidebar-label">Mi espacio</p>
    <nav aria-label="Navegación de mi espacio">
        <c:choose>
            <c:when test="${not empty sessionScope.adminObj}">
                <a class="${currentPage eq '/admin/index.jsp' ? 'active' : ''}" ${currentPage eq '/admin/index.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/index.jsp"><i class="fa-solid fa-chart-simple" aria-hidden="true"></i>Resumen</a>
                <a class="${currentPage eq '/admin/cursos.jsp' ? 'active' : ''}" ${currentPage eq '/admin/cursos.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/cursos.jsp"><i class="fa-solid fa-book-open" aria-hidden="true"></i>Cursos y tarifas</a>
                <a class="${currentPage eq '/admin/configuracion.jsp' ? 'active' : ''}" ${currentPage eq '/admin/configuracion.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/configuracion.jsp"><i class="fa-solid fa-sliders" aria-hidden="true"></i>Condiciones de reserva</a>
                  <a class="${currentPage eq '/admin/correos.jsp' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/correos.jsp">Correos y avisos</a>
                <a class="${currentPage eq '/admin/alumno.jsp' or currentPage eq '/asesoria.jsp' ? 'active' : ''}" ${currentPage eq '/admin/alumno.jsp' or currentPage eq '/asesoria.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/alumno.jsp"><i class="fa-regular fa-calendar" aria-hidden="true"></i>Asesorías</a>
                <a class="${currentPage eq '/admin/pagos.jsp' or currentPage eq '/pago.jsp' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/pagos.jsp"><i class="fa-solid fa-receipt" aria-hidden="true"></i>Pagos de alumnos</a>
                <a class="${currentPage eq '/admin/liquidaciones.jsp' or currentPage eq '/liquidacion.jsp' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/liquidaciones.jsp"><i class="fa-solid fa-money-bill-transfer" aria-hidden="true"></i>Liquidaciones</a>
                <a class="${currentPage eq '/admin/list_docente.jsp' or currentPage eq '/admin/docente.jsp' or currentPage eq '/admin/edit_docente.jsp' or currentPage eq '/admin/oferta.jsp' ? 'active' : ''}" ${currentPage eq '/admin/list_docente.jsp' or currentPage eq '/admin/docente.jsp' or currentPage eq '/admin/edit_docente.jsp' or currentPage eq '/admin/oferta.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/list_docente.jsp"><i class="fa-solid fa-chalkboard-user" aria-hidden="true"></i>Profesores</a>
            </c:when>
            <c:when test="${not empty sessionScope.docenteObj}">
                <a class="${currentPage eq '/docente/index.jsp' ? 'active' : ''}" ${currentPage eq '/docente/index.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente/index.jsp"><i class="fa-solid fa-chart-simple" aria-hidden="true"></i>Resumen</a>
                <a class="${currentPage eq '/docente/alumno.jsp' or currentPage eq '/asesoria.jsp' ? 'active' : ''}" ${currentPage eq '/docente/alumno.jsp' or currentPage eq '/asesoria.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente/alumno.jsp"><i class="fa-regular fa-calendar" aria-hidden="true"></i>Mis asesorías</a>
                <a class="${currentPage eq '/docente/disponibilidad.jsp' ? 'active' : ''}" ${currentPage eq '/docente/disponibilidad.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente/disponibilidad.jsp"><i class="fa-regular fa-clock" aria-hidden="true"></i>Disponibilidad</a>
                <a class="${currentPage eq '/docente/ingresos.jsp' or currentPage eq '/liquidacion.jsp' ? 'active' : ''}" href="${pageContext.request.contextPath}/docente/ingresos.jsp"><i class="fa-solid fa-wallet" aria-hidden="true"></i>Mis ingresos</a>
                <a class="${currentPage eq '/docente/edit_profile.jsp' ? 'active' : ''}" ${currentPage eq '/docente/edit_profile.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente/edit_profile.jsp"><i class="fa-regular fa-user" aria-hidden="true"></i>Mi perfil</a>
            </c:when>
            <c:otherwise>
                <a class="${currentPage eq '/list_tutoria.jsp' or currentPage eq '/asesoria.jsp' ? 'active' : ''}" ${currentPage eq '/list_tutoria.jsp' or currentPage eq '/asesoria.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/list_tutoria.jsp"><i class="fa-regular fa-calendar" aria-hidden="true"></i>Mis asesorías</a>
                <a class="${currentPage eq '/reg_tutoria.jsp' or currentPage eq '/oferta.jsp' ? 'active' : ''}" ${currentPage eq '/reg_tutoria.jsp' or currentPage eq '/oferta.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/oferta.jsp"><i class="fa-solid fa-plus" aria-hidden="true"></i>Solicitar asesoría</a>
                <a class="${currentPage eq '/mis_pagos.jsp' or currentPage eq '/pago.jsp' ? 'active' : ''}" href="${pageContext.request.contextPath}/mis_pagos.jsp"><i class="fa-solid fa-receipt" aria-hidden="true"></i>Mis pagos</a>
                <a class="${currentPage eq '/change_password.jsp' or currentPage eq '/perfil.jsp' ? 'active' : ''}" ${currentPage eq '/change_password.jsp' or currentPage eq '/perfil.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/perfil.jsp"><i class="fa-regular fa-user" aria-hidden="true"></i>Mi perfil</a>
            </c:otherwise>
        </c:choose>
    </nav>
    <div class="tm-sidebar-help"><i class="fa-solid fa-laptop" aria-hidden="true"></i><strong>Asesorías online</strong><span>Todos los horarios se muestran en hora de Lima.</span><a href="${pageContext.request.contextPath}/index.jsp#preguntas">Preguntas frecuentes</a></div>
</aside>
</c:if>
