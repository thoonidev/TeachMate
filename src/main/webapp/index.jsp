<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.OfertaDAO,com.tme.dao.DocenteDAO,com.tme.db.DBConnection,com.tme.modal.Docente,com.tme.security.Html,java.util.List,com.tme.service.TiempoVisible" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
List<OfertaDAO.Curso> homeSubjects = new OfertaDAO(DBConnection.getConn()).cursos(true);
List<Docente> homeTeachers = new DocenteDAO(DBConnection.getConn()).getActiveDocentes();
var homePlazos = new com.tme.dao.ConfiguracionReservasDAO(DBConnection.getConn()).obtener();
%>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="description" content="Asesorías individuales online. Elige tu curso, reserva un horario y aprende con un profesor dedicado a tus dudas.">
    <title>TeachMate | Asesorías individuales online</title>
    <%@ include file="component/allcss.jsp" %>
</head>
<body class="tm-home">
    <%@ include file="component/navbar.jsp" %>
    <main id="contenido" class="tm-main" tabindex="-1">
        <%@ include file="component/messages.jsp" %>
        <section class="tm-hero" aria-labelledby="hero-title">
            <div class="container tm-hero-grid">
                <div>
                    <p class="tm-eyebrow"><span class="tm-live-dot" aria-hidden="true"></span> Un profesor para ti</p>
                    <h1 id="hero-title">Entiende el tema.<br><em>Avanza con confianza.</em></h1>
                    <p class="tm-hero-copy">Tu tema, tu ritmo y un profesor dedicado a ti. Reserva una asesoría online y trabaja lo que necesitas entender.</p>
                    <div class="tm-hero-actions">
                        <a class="btn btn-primary" href="oferta.jsp">Explorar asesorías <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a>
                        <c:choose><c:when test="${signedIn}"><a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}${workspaceHome}">Ir a mi espacio</a></c:when><c:otherwise><a class="btn btn-outline-secondary" href="#como-funciona">Cómo funciona</a></c:otherwise></c:choose>
                    </div>
                    <ul class="tm-hero-benefits"><li><i class="fa-solid fa-check" aria-hidden="true"></i> Atención uno a uno</li><li><i class="fa-solid fa-check" aria-hidden="true"></i> Desde donde estés</li></ul>
                </div>
                <div class="tm-learning-board" aria-label="Una asesoría centrada en tu aprendizaje">
                    <div class="tm-board-top"><span>EL ESPACIO ES TUYO</span><i class="fa-solid fa-arrow-up-right-from-square" aria-hidden="true"></i></div>
                    <p class="tm-board-title">Menos dudas.<br>Más <span>claridad.</span></p>
                    <div class="tm-board-line"><span class="tm-board-number">01</span><div><strong>Tu tema</strong><span>Lo que necesitas reforzar</span></div><i class="fa-solid fa-book-open" aria-hidden="true"></i></div>
                    <div class="tm-board-line"><span class="tm-board-number">02</span><div><strong>Tu profesor</strong><span>Atención individual y cercana</span></div><i class="fa-regular fa-user" aria-hidden="true"></i></div>
                    <div class="tm-board-line"><span class="tm-board-number">03</span><div><strong>Tu siguiente paso</strong><span>Práctica y recomendaciones</span></div><i class="fa-solid fa-arrow-trend-up" aria-hidden="true"></i></div>
                    <div class="tm-board-bottom"><span class="tm-live-dot" aria-hidden="true"></span> Individual · Online · A tu ritmo</div>
                </div>
            </div>
        </section>
        <section class="tm-service-strip" aria-label="Tu asesoría en TeachMate"><div class="container"><span><i class="fa-regular fa-user" aria-hidden="true"></i> Un profesor para ti</span><span><i class="fa-solid fa-laptop" aria-hidden="true"></i> Clases online</span><span><i class="fa-regular fa-clock" aria-hidden="true"></i> Horarios de Lima</span></div></section>
        <% if (!homeSubjects.isEmpty()) { %>
        <section class="container tm-section" id="cursos" aria-labelledby="subjects-title">
            <div class="tm-section-head tm-section-head-centered"><p class="tm-eyebrow">Elige por dónde empezar</p><h2 id="subjects-title">Un espacio para cada tema</h2><p>Encuentra tu curso y consulta sus horarios y precios.</p></div>
            <div class="tm-subjects">
                <% for (OfertaDAO.Curso especialidad : homeSubjects) { %>
                    <a class="tm-subject" href="oferta.jsp?cursoId=<%=especialidad.id()%>"><i class="fa-solid fa-book-open" aria-hidden="true"></i><span><%=Html.escape(especialidad.nombre())%></span><i class="fa-solid fa-arrow-right" aria-hidden="true"></i></a>
                <% } %>
            </div>
        </section>
        <% } %>
        <section class="container tm-section" id="profesores" aria-labelledby="teachers-title">
            <div class="tm-section-head tm-section-head-centered"><p class="tm-eyebrow">Aprende con el equipo</p><h2 id="teachers-title">Tu próximo profesor, aquí</h2><p>Conoce su experiencia y elige con quién aprender.</p></div>
            <div class="tm-teacher-grid">
                <% if (homeTeachers.isEmpty()) { %><div class="tm-catalog-empty"><span class="tm-empty-icon" aria-hidden="true"><i class="fa-solid fa-chalkboard-user"></i></span><h3>Estamos preparando el equipo</h3><p>Aquí encontrarás a los profesores cuando estén disponibles.</p><a class="tm-text-link" href="#como-funciona">Conoce cómo reservar <i class="fa-solid fa-arrow-right ms-1" aria-hidden="true"></i></a></div><% }
                   for (Docente teacher : homeTeachers) {
                       String teacherName = teacher.getNombre() == null ? "" : teacher.getNombre().strip();
                       String teacherInitial = teacherName.isEmpty() ? "P" : teacherName.substring(0, teacherName.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
                %>
                <article class="tm-teacher-card">
                    <div class="tm-teacher-top"><span class="tm-avatar" aria-hidden="true"><%=Html.escape(teacherInitial)%></span><span class="tm-teacher-mode"><i class="fa-solid fa-laptop" aria-hidden="true"></i> Online</span></div>
                    <h3><%=Html.escape(teacher.getNombre())%></h3><p class="tm-teacher-subject"><%=Html.escape(teacher.getEspecialidad())%></p>
                    <p class="tm-teacher-qualification"><%=Html.escape(teacher.getCalificacion())%></p><p class="tm-teacher-qualification"><%=Html.escape(teacher.getPresentacion())%></p><p class="small text-muted"><%=Html.escape(teacher.getNiveles())%></p>
                    <div class="tm-teacher-action"><span>Asesoría individual</span><a class="btn btn-outline-primary btn-sm" href="oferta.jsp?docenteId=<%=teacher.getId()%>" aria-label="Ver cursos y horarios de <%=Html.escape(teacher.getNombre())%>">Ver horarios <i class="fa-solid fa-arrow-right ms-1" aria-hidden="true"></i></a></div>
                </article>
                <% } %>
            </div>
        </section>
        <section class="tm-how-section" id="como-funciona" aria-labelledby="steps-title">
            <div class="container"><div class="tm-section-head tm-section-head-centered"><p class="tm-eyebrow">Tres pasos para empezar</p><h2 id="steps-title">Reserva. Prepárate. Aprende.</h2><p>Consulta las opciones y reserva con tu cuenta de alumno.</p></div>
                <div class="tm-steps">
                    <article class="tm-step"><span class="tm-step-number">01</span><h3>Encuentra a tu profesor</h3><p>Explora los cursos, horarios y precios. Crea tu cuenta para reservar.</p></article>
                    <article class="tm-step"><span class="tm-step-number">02</span><h3>Reserva tu espacio</h3><p>Elige un horario disponible y escribe el tema que quieres trabajar.</p></article>
                    <article class="tm-step"><span class="tm-step-number">03</span><h3>Paga y consulta la confirmación</h3><p>Paga y sube tu comprobante en Mis pagos. Validamos el pago para confirmar la clase. Consulta el enlace en Mis asesorías.</p></article>
                </div>
                <% if(homePlazos.completa()) { %><p class="tm-booking-note"><i class="fa-regular fa-clock" aria-hidden="true"></i> Reserva con al menos <strong><%=TiempoVisible.duracion(homePlazos.antelacion())%></strong> de anticipación. Te mostramos solo los horarios que dejan tiempo para revisar el pago.</p><% } %>
            </div>
        </section>
        <section class="container tm-section tm-faq-section" id="preguntas" aria-labelledby="faq-title">
            <div><p class="tm-eyebrow">Antes de empezar</p><h2 id="faq-title">Preguntas frecuentes</h2><p class="text-muted">Lo esencial para organizar tu primera asesoría.</p></div>
            <div class="tm-faq-list">
                <details><summary>¿Las asesorías son individuales?</summary><p>Sí. Cada sesión reúne a un alumno y un profesor para trabajar el tema que necesitas reforzar.</p></details>
                <details><summary>¿Necesito una cuenta?</summary><p>Puedes conocer al equipo sin registrarte. Para reservar y consultar el estado de tu asesoría necesitas una cuenta de alumno.</p></details>
                <details><summary>¿Cuándo se confirma mi clase?</summary><p>Cuando administración valida tu pago. Al reservar, guardamos el horario hasta el plazo indicado para subir tu comprobante. Consulta la confirmación en Mis asesorías.</p></details>
                <% if(homePlazos.completa()) { %><details><summary>¿Puedo reservar de noche?</summary><p>Sí. Puedes reservar en la web a cualquier hora, con al menos <%=TiempoVisible.duracion(homePlazos.antelacion())%> de anticipación. Revisamos los pagos <%=Html.escape(homePlazos.calendario().descripcion())%>. El plazo de pago también cuenta fuera de ese horario.</p></details><% } %>
                <details><summary>¿En qué zona horaria se muestran las sesiones?</summary><p>Todos los horarios son de Lima, Perú. Si estás en otro país, revisa la diferencia horaria.</p></details>
                <details><summary>¿Cómo accedo a la clase online?</summary><p>El profesor publicará el enlace en Mis asesorías. Necesitas internet y un dispositivo con micrófono.</p></details>
            </div>
        </section>
        <section class="container tm-section"><div class="tm-home-cta"><div><p class="tm-eyebrow">Empieza con una pregunta</p><h2>Haz espacio para entenderlo.</h2><p>Encuentra al profesor que te acompañará en el siguiente paso.</p></div><a class="btn btn-primary" href="#profesores">Ver profesores <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a></div></section>
    </main>
    <%@ include file="component/footer.jsp" %>
</body>
</html>