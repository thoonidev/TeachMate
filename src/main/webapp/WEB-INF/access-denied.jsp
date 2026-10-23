<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <title>Acceso a tu cuenta | TeachMate</title>
    <%@ include file="../component/allcss.jsp" %>
</head>
<body class="tm-error"><main id="contenido">
    <p class="tm-eyebrow">TeachMate</p>
    <h1>Ingresa con la cuenta correspondiente</h1>
    <p><c:out value="${requestScope.accessMessage}" /></p>
    <nav aria-label="Opciones de acceso">
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/index.jsp">Volver al inicio</a>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}<c:out value='${requestScope.accessLogin}' />">Ir al inicio de sesión</a>
    </nav>
</main></body>
</html>
