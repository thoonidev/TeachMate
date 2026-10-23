<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.EspecialidadDAO,com.tme.db.DBConnection,com.tme.modal.Especialidad" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="description" content="Organiza tus tutorías con TeachMate. Elige un docente, solicita un horario y consulta el estado de tus sesiones.">
    <title>TeachMate | Aprende a tu ritmo</title>
    <%@ include file="component/allcss.jsp" %>
</head>
<body class="tm-home">
    <%@ include file="component/navbar.jsp" %>
    <main id="contenido" class="tm-main" tabindex="-1">
        <%@ include file="component/messages.jsp" %>
        <section class="tm-hero" aria-labelledby="hero-title">
            <div class="container tm-hero-grid">
                <div>
                    <p class="tm-eyebrow">Tu próximo paso empieza aquí</p>
                    <h1 id="hero-title">Aprender se hace<br>mejor con <em>apoyo.</em></h1>
                    <p class="tm-hero-copy">Dale espacio a tus preguntas. Encuentra un docente, organiza tu tutoría y avanza a tu ritmo.</p>
                    <div class="tm-hero-actions">
                        <c:choose>
                            <c:when test="${not empty adminObj}"><a class="btn btn-primary" href="admin/index.jsp">Ir a mi panel <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a></c:when>
                            <c:when test="${not empty docenteObj}"><a class="btn btn-primary" href="docente/index.jsp">Ir a mi panel <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a></c:when>
                            <c:otherwise><a class="btn btn-primary" href="reg_tutoria.jsp">Solicitar una tutoría <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a></c:otherwise>
                        </c:choose>
                        <a class="btn btn-outline-secondary" href="#como-funciona">Cómo funciona</a>
                    </div>
                    <p class="tm-hero-note"><i class="fa-regular fa-calendar-check me-1" aria-hidden="true"></i> Tus solicitudes, horarios y seguimiento en un solo lugar.</p>
                </div>
                <div class="tm-hero-photo">
                    <img src="img/img01.jpg" alt="Docente explicando una clase en la pizarra" width="6000" height="4000" fetchpriority="high">
                    <div class="tm-photo-note"><strong>Una pregunta. Un nuevo comienzo.</strong>Haz espacio para seguir aprendiendo.</div>
                </div>
            </div>
        </section>
        <section class="container tm-section" aria-labelledby="subjects-title">
            <div class="tm-section-head">
                <div><p class="tm-eyebrow">Encuentra tu punto de partida</p><h2 id="subjects-title">¿Qué quieres reforzar?</h2></div>
                <p>Consulta las especialidades y elige al docente que acompañará tu aprendizaje.</p>
            </div>
            <div class="tm-subjects">
                <% for (Especialidad especialidad : new EspecialidadDAO(DBConnection.getConn()).getAllEspecialidad()) { %>
                    <span class="tm-subject"><%=com.tme.security.Html.escape(especialidad.getEspecialidadNombre())%></span>
                <% } %>
            </div>
        </section>
        <section class="container tm-section" id="como-funciona" aria-labelledby="steps-title">
            <div class="tm-section-head">
                <div><p class="tm-eyebrow">Simple, de principio a fin</p><h2 id="steps-title">Tu tutoría en tres pasos</h2></div>
                <a href="reg_tutoria.jsp">Ver el formulario <i class="fa-solid fa-arrow-right ms-1" aria-hidden="true"></i></a>
            </div>
            <div class="tm-steps">
                <article class="tm-step"><span class="tm-step-number">01</span><h3>Elige a tu docente</h3><p>Revisa las especialidades disponibles. Inicia sesión como alumno o crea tu cuenta para enviar la solicitud.</p></article>
                <article class="tm-step"><span class="tm-step-number">02</span><h3>Propón un horario</h3><p>Indica la fecha, la hora de Lima y la duración. El horario queda reservado mientras el docente revisa tu solicitud.</p></article>
                <article class="tm-step"><span class="tm-step-number">03</span><h3>Sigue tu solicitud</h3><p>Consulta la respuesta del docente y los comentarios en Mis tutorías. Si tus planes cambian, cancela antes del inicio.</p></article>
            </div>
        </section>
        <section class="container tm-section" aria-labelledby="video-title">
            <div class="tm-section-head">
                <div><p class="tm-eyebrow">Conoce TeachMate</p><h2 id="video-title">Más cerca de tu aprendizaje</h2></div>
                <p>Conoce nuestra propuesta en este video de presentación.</p>
            </div>
            <video class="tm-video" controls preload="none" playsinline aria-label="Video de presentación de TeachMate">
                <source src="video/promoc.mp4" type="video/mp4">
                Tu navegador no puede reproducir el video.
            </video>
        </section>
    </main>
    <%@ include file="component/footer.jsp" %>
</body>
</html>
