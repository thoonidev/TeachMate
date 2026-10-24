<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>No se pudo completar la operación | TeachMate</title>
    <%@ include file="../component/allcss.jsp" %>
</head>
<body class="tm-error">
    <main id="contenido">
        <p class="tm-eyebrow">TeachMate</p>
        <h1>No se pudo completar la operación</h1>
        <p>Inténtalo nuevamente más tarde.</p>
        <a class="btn btn-primary" href="${pageContext.request.contextPath}/index.jsp">Volver al inicio</a>
    </main>
</body>
</html>
