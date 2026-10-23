<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Tutorías | TeachMate</title>
    <%@ include file="../component/allcss.jsp" %>
</head>
<body class="tm-table-page">
    <%@ include file="../component/navbar.jsp" %>
    <main id="contenido" class="tm-main" tabindex="-1">
        <%@ include file="../component/messages.jsp" %>
        <% Rol panelRol = Rol.ADMIN; int panelActorId = 0; %>
        <%@ include file="../WEB-INF/panel-tutorias.jsp" %>
    </main>
    <%@ include file="../component/footer.jsp" %>
</body>
</html>
