<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Acceso a tu cuenta | TeachMate</title>
    <style>
        body { margin:0; padding:24px; min-height:90vh; display:grid; place-items:center;
               font:17px/1.6 system-ui,sans-serif; color:#243347; background:#f3f7fb; }
        main { max-width:540px; padding:36px; border-radius:16px; background:white; box-shadow:0 8px 30px #24334712; }
        h1 { font-size:26px; line-height:1.3; }
        nav { display:flex; flex-wrap:wrap; gap:16px; margin-top:24px; }
        a { color:#145a9e; }
    </style>
</head>
<body><main>
    <p>TeachMate</p>
    <h1>Ingresa con la cuenta correspondiente</h1>
    <p><c:out value="${requestScope.accessMessage}" /></p>
    <nav aria-label="Opciones de acceso">
        <a href="${pageContext.request.contextPath}/index.jsp">Volver al inicio</a>
        <a href="${pageContext.request.contextPath}<c:out value='${requestScope.accessLogin}' />">Ir al inicio de sesión</a>
    </nav>
</main></body>
</html>
