<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.tme.dao.OfertaDAO,com.tme.dao.DocenteDAO,com.tme.db.DBConnection,com.tme.modal.Docente,com.tme.security.Html,java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="description" content="Conoce al equipo de TeachMate y solicita una asesoría individual online para reforzar los temas que necesitas.">
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
                    <p class="tm-hero-copy">Asesorías individuales online con un equipo de profesores. Trabaja tus dudas, practica a tu ritmo y recibe atención dedicada a tu aprendizaje.</p>
                    <div class="tm-hero-actions">
                        <a class="btn btn-primary" href="#profesores">Conocer profesores <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a>
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
        <section class="container tm-section" id="cursos" aria-labelledby="subjects-title">
            <div class="tm-section-head"><div><p class="tm-eyebrow">Tu punto de partida</p><h2 id="subjects-title">¿Qué necesitas reforzar?</h2></div><p>Explora las especialidades del equipo y encuentra a un profesor para trabajar tus dudas.</p></div>
            <div class="tm-subjects">
                <% List<OfertaDAO.Curso> homeSubjects = new OfertaDAO(DBConnection.getConn()).cursos(true);
                   if (homeSubjects.isEmpty()) { %><p class="tm-empty">Pronto podrás consultar las especialidades de nuestro equipo.</p><% }
                   for (OfertaDAO.Curso especialidad : homeSubjects) { %>
                    <a class="tm-subject" href="oferta.jsp?cursoId=<%=especialidad.id()%>"><i class="fa-solid fa-book-open" aria-hidden="true"></i><%=Html.escape(especialidad.nombre())%></a>
                <% } %>
            </div>
        </section>
        <section class="container tm-section" id="profesores" aria-labelledby="teachers-title">
            <div class="tm-section-head"><div><p class="tm-eyebrow">Personas que te acompañan</p><h2 id="teachers-title">Conoce a nuestros profesores</h2></div><p>Elige según la especialidad y cuéntale qué tema quieres trabajar en tu asesoría.</p></div>
            <div class="tm-teacher-grid">
                <% List<Docente> homeTeachers = new DocenteDAO(DBConnection.getConn()).getActiveDocentes();
                   if (homeTeachers.isEmpty()) { %><div class="tm-empty">Todavía no hay profesores disponibles para nuevas solicitudes. Vuelve a consultar más adelante.</div><% }
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
            <div class="container"><div class="tm-section-head"><div><p class="tm-eyebrow">Así de sencillo</p><h2 id="steps-title">De tu duda a tu próxima asesoría</h2></div><a class="tm-text-link" href="oferta.jsp">Solicitar asesoría <i class="fa-solid fa-arrow-right ms-1" aria-hidden="true"></i></a></div>
                <div class="tm-steps">
                    <article class="tm-step"><span class="tm-step-number">01</span><h3>Encuentra a tu profesor</h3><p>Consulta los cursos del equipo. Crea tu cuenta o inicia sesión para enviar tu solicitud.</p></article>
                    <article class="tm-step"><span class="tm-step-number">02</span><h3>Cuéntale qué necesitas</h3><p>Elige un curso, una duración y un horario disponible. Indica el tema. El profesor revisará tu solicitud.</p></article>
                    <article class="tm-step"><span class="tm-step-number">03</span><h3>Consulta la respuesta</h3><p>Cuando el profesor acepte, sigue las instrucciones de Mis pagos y adjunta tu comprobante. Administración verificará el ingreso para confirmar la clase. El enlace aparecerá en Mis asesorías.</p></article>
                </div>
            </div>
        </section>
        <section class="container tm-section tm-faq-section" id="preguntas" aria-labelledby="faq-title">
            <div><p class="tm-eyebrow">Antes de empezar</p><h2 id="faq-title">Preguntas frecuentes</h2><p class="text-muted">Lo esencial para organizar tu primera asesoría.</p></div>
            <div class="tm-faq-list">
                <details><summary>¿Las asesorías son individuales?</summary><p>Sí. Cada sesión reúne a un alumno y un profesor para trabajar el tema indicado en la solicitud.</p></details>
                <details><summary>¿Necesito una cuenta para solicitar?</summary><p>Puedes conocer al equipo sin registrarte. Para enviar una solicitud y consultar las respuestas necesitas una cuenta de alumno.</p></details>
                <details><summary>¿Enviar una solicitud confirma el horario?</summary><p>No. El profesor debe aceptar y después se debe validar el pago. Consulta el estado en Mis asesorías antes de dar por confirmada la sesión.</p></details>
                <details><summary>¿En qué zona horaria se muestran las sesiones?</summary><p>Las fechas y horas corresponden a Lima, Perú. Si estás en otra zona horaria, considera la diferencia al proponer tu horario.</p></details>
                <details><summary>¿Cómo accedo a la clase online?</summary><p>El enlace estará en el detalle de la asesoría confirmada cuando lo publique el profesor. Necesitarás conexión a internet, micrófono y un dispositivo para la videollamada.</p></details>
            </div>
        </section>
        <section class="container tm-section"><div class="tm-home-cta"><div><p class="tm-eyebrow">Empieza con una pregunta</p><h2>Haz espacio para entenderlo.</h2><p>Encuentra al profesor que te acompañará en el siguiente paso.</p></div><a class="btn btn-primary" href="#profesores">Ver profesores <i class="fa-solid fa-arrow-right ms-2" aria-hidden="true"></i></a></div></section>
    </main>
    <%@ include file="component/footer.jsp" %>
</body>
</html>