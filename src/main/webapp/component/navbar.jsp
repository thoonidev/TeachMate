<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<c:set var="currentPage" value="${pageContext.request.servletPath}" />
<a class="tm-skip-link" href="#contenido">Saltar al contenido</a>
<nav class="navbar navbar-expand-lg navbar-light tm-navbar" aria-label="Navegación principal">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/index.jsp">
            <span class="tm-brand-icon" aria-hidden="true"><i class="fa-solid fa-book-open" aria-hidden="true"></i></span>TeachMate
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#teachmateNav"
                aria-controls="teachmateNav" aria-expanded="false" aria-label="Abrir menú de navegación">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="teachmateNav">
            <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-1">
                <li class="nav-item"><a class="nav-link ${currentPage eq '/index.jsp' ? 'active' : ''}"
                    ${currentPage eq '/index.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/index.jsp">Inicio</a></li>
                <c:choose>
                    <c:when test="${not empty sessionScope.adminObj}">
                        <li class="nav-item"><span class="tm-role">Administrador</span></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/admin/index.jsp' ? 'active' : ''}" ${currentPage eq '/admin/index.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/index.jsp">Panel</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/admin/docente.jsp' or currentPage eq '/admin/edit_docente.jsp' ? 'active' : ''}" ${currentPage eq '/admin/docente.jsp' or currentPage eq '/admin/edit_docente.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/docente.jsp">Registrar docente</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/admin/list_docente.jsp' ? 'active' : ''}" ${currentPage eq '/admin/list_docente.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/list_docente.jsp">Docentes</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/admin/alumno.jsp' ? 'active' : ''}" ${currentPage eq '/admin/alumno.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin/alumno.jsp">Tutorías</a></li>
                        <li class="nav-item"><form action="${pageContext.request.contextPath}/adminLogout" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <button class="btn btn-outline-secondary btn-sm" type="submit">Cerrar sesión</button>
                        </form></li>
                    </c:when>
                    <c:when test="${not empty sessionScope.docenteObj}">
                        <li class="nav-item"><span class="tm-role">Docente</span></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/docente/index.jsp' ? 'active' : ''}" ${currentPage eq '/docente/index.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente/index.jsp">Panel</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/docente/alumno.jsp' or currentPage eq '/docente/comentario.jsp' ? 'active' : ''}" ${currentPage eq '/docente/alumno.jsp' or currentPage eq '/docente/comentario.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente/alumno.jsp">Mis tutorías</a></li>
                        <li class="nav-item dropdown">
                            <button class="nav-link dropdown-toggle btn" id="cuentaDocente" data-bs-toggle="dropdown" aria-expanded="false" type="button"><c:out value="${docenteObj.nombre}" /></button>
                            <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="cuentaDocente">
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/docente/edit_profile.jsp">Mi perfil y contraseña</a></li>
                                <li><form action="${pageContext.request.contextPath}/docenteLogout" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button class="dropdown-item" type="submit">Cerrar sesión</button></form></li>
                            </ul>
                        </li>
                    </c:when>
                    <c:when test="${not empty sessionScope.userObj}">
                        <li class="nav-item"><span class="tm-role">Alumno</span></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/reg_tutoria.jsp' ? 'active' : ''}" ${currentPage eq '/reg_tutoria.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/reg_tutoria.jsp">Solicitar tutoría</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/list_tutoria.jsp' ? 'active' : ''}" ${currentPage eq '/list_tutoria.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/list_tutoria.jsp">Mis tutorías</a></li>
                        <li class="nav-item dropdown">
                            <button class="nav-link dropdown-toggle btn" id="cuentaAlumno" data-bs-toggle="dropdown" aria-expanded="false" type="button"><c:out value="${userObj.nombre}" /></button>
                            <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="cuentaAlumno">
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/change_password.jsp">Cambiar contraseña</a></li>
                                <li><form action="${pageContext.request.contextPath}/userLogout" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><button class="dropdown-item" type="submit">Cerrar sesión</button></form></li>
                            </ul>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/reg_tutoria.jsp' ? 'active' : ''}" ${currentPage eq '/reg_tutoria.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/reg_tutoria.jsp">Tutorías</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/docente_login.jsp' ? 'active' : ''}" ${currentPage eq '/docente_login.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/docente_login.jsp">Soy docente</a></li>
                        <li class="nav-item"><a class="nav-link ${currentPage eq '/admin_login.jsp' ? 'active' : ''}" ${currentPage eq '/admin_login.jsp' ? 'aria-current="page"' : ''} href="${pageContext.request.contextPath}/admin_login.jsp">Administración</a></li>
                        <li class="nav-item"><a class="btn btn-primary btn-sm" href="${pageContext.request.contextPath}/alumno_login.jsp">Ingresar como alumno <i class="fa-solid fa-arrow-right ms-1" aria-hidden="true"></i></a></li>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </div>
</nav>
