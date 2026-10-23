package com.tme.security;

import static org.junit.Assert.*;
import com.tme.dao.*;
import com.tme.modal.*;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;
import org.apache.catalina.startup.Tomcat;
import org.junit.*;

/** Real HTTP requests to Tomcat/Jasper; JDBC points only to an isolated in-memory database. */
public class SecurityIntegrationTest {
    private static final String JDBC = "jdbc:h2:mem:security;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String PASSWORD = "Test-password-12345";
    private static Tomcat tomcat;
    private static String base;
    private static Driver bridge;

    @BeforeClass public static void start() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getName().startsWith("com.mysql.")) DriverManager.deregisterDriver(driver);
        }
        bridge = new Driver() {
            public boolean acceptsURL(String url) { return url.equals("jdbc:mysql://test.invalid/teachmate"); }
            public Connection connect(String url, Properties properties) throws SQLException {
                return acceptsURL(url) ? DriverManager.getConnection(JDBC, "sa", "") : null;
            }
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties p) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public Logger getParentLogger() { return Logger.getGlobal(); }
        };
        DriverManager.registerDriver(bridge);
        try (Connection connection = db(); Statement statement = connection.createStatement()) {
            for (String sql : Files.readString(Path.of("database/schema.sql")).replace("\uFEFF", "").split(";")) {
                if (!sql.isBlank()) statement.execute(sql);
            }
            UserDAO users = new UserDAO(connection);
            assertTrue(users.userRegister(new User("<script>alert(1)</script>", "user@example.invalid", PASSWORD)));
            assertTrue(users.userRegister(new User("Otro", "other@example.invalid", PASSWORD)));
            DocenteDAO docentes = new DocenteDAO(connection);
            assertTrue(docentes.registerDocente(new Docente("Docente Uno", "1990-01-01", "Prueba", "Algebra", "doc1@example.invalid", "123", PASSWORD)));
            assertTrue(docentes.registerDocente(new Docente("Docente Dos", "1990-01-01", "Prueba", "Algebra", "doc2@example.invalid", "123", PASSWORD)));
            statement.execute("insert into especialidad(especialidad_nombre) values('Algebra')");
            statement.execute("insert into tutoria(userId,nombre,genero,edad,ftutoria,email,telefono,experiencia,docenteId,direccion,estado) values(2,'Privado','Prueba','20','2026-10-22','other@example.invalid','123','Prueba',2,'Privado','Pendiente')");
        }
        Path webapp = Path.of("target/security-webapp");
        copyTree(Path.of("src/main/webapp"), webapp);
        copyTree(Path.of("target/classes"), webapp.resolve("WEB-INF/classes"));
        tomcat = new Tomcat();
        tomcat.setBaseDir(Path.of("target/security-tomcat").toAbsolutePath().toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        org.apache.catalina.Context context = tomcat.addWebapp("", webapp.toAbsolutePath().toString());
        context.setParentClassLoader(SecurityIntegrationTest.class.getClassLoader());
        tomcat.start();
        base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort();
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path output = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) Files.createDirectories(output);
                else Files.copy(path, output, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    private static Connection db() throws SQLException { return DriverManager.getConnection(JDBC, "sa", ""); }
    @AfterClass public static void stop() throws Exception {
        try { if (tomcat != null) { tomcat.stop(); tomcat.destroy(); } }
        finally { if (bridge != null) DriverManager.deregisterDriver(bridge); }
    }

    private static class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> post(String path, String body) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        String token(String page) throws Exception {
            HttpResponse<String> response = get(page);
            assertEquals(response.body(), 200, response.statusCode());
            Matcher matcher = Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(response.body());
            assertTrue("Missing CSRF on " + page, matcher.find());
            return matcher.group(1);
        }
        void login(String page, String action, String email, String password) throws Exception {
            String token = token(page);
            HttpResponse<String> result = post(action, "csrfToken=" + token + "&email=" + enc(email) + "&clave=" + enc(password));
            assertEquals(result.body(), 302, result.statusCode());
        }
    }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    @Test public void anonymousAndWrongRolesCannotReachPagesOrActions() throws Exception {
        Browser browser = new Browser();
        for (String route : List.of("/admin/alumno.jsp", "/admin/list_docente.jsp", "/docente/comentario.jsp?id=1", "/list_tutoria.jsp")) {
            HttpResponse<String> response = browser.get(route);
            assertEquals(route, 302, response.statusCode());
            String login = route.startsWith("/admin/") ? "/admin_login.jsp"
                    : route.startsWith("/docente/") ? "/docente_login.jsp" : "/alumno_login.jsp";
            assertTrue(response.headers().firstValue("location").orElse("").endsWith(login));
        }
        assertEquals(403, browser.get("/userChangePassword").statusCode());
        assertEquals(403, browser.post("/addDocente", "").statusCode());
        browser.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        HttpResponse<String> denied = browser.get("/admin/index.jsp");
        assertEquals(403, denied.statusCode());
        assertTrue(denied.body().contains("Tu cuenta no tiene permiso"));
        assertTrue(denied.body().contains("Volver al inicio"));
        assertFalse(denied.body().contains("HTTP Status 403"));
        assertEquals(403, browser.post("/updateComentario", "").statusCode());
    }

    @Test public void csrfSessionRotationEscapingAndLogoutWork() throws Exception {
        Browser browser = new Browser();
        String before = browser.token("/alumno_login.jsp");
        assertEquals(403, browser.post("/userLogin", "email=user@example.invalid&clave=" + PASSWORD).statusCode());
        HttpResponse<String> login = browser.post("/userLogin", "csrfToken=" + before + "&email=user@example.invalid&clave=" + PASSWORD);
        assertEquals(302, login.statusCode());
        assertTrue(login.headers().allValues("set-cookie").stream().anyMatch(cookie -> cookie.contains("HttpOnly")));
        String after = browser.token("/change_password.jsp");
        assertNotEquals(before, after);
        String home = browser.get("/index.jsp").body();
        assertTrue(home.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(home.contains("<script>alert(1)</script>"));
        assertEquals(403, browser.post("/userLogout", "csrfToken=" + before).statusCode());
        assertEquals(405, browser.get("/userLogout").statusCode());
        assertEquals(302, browser.post("/userLogout", "csrfToken=" + after).statusCode());
        assertEquals(302, browser.get("/change_password.jsp").statusCode());
    }

    @Test public void userIdTamperingCannotChangeAnotherPassword() throws Exception {
        Browser browser = new Browser();
        browser.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        String token = browser.token("/change_password.jsp");
        String changed = "Changed-password-12345";
        assertEquals(302, browser.post("/userChangePassword", "csrfToken=" + token + "&userId=2&oldPassword=" + PASSWORD + "&newPassword=" + changed).statusCode());
        try (Connection connection = db()) {
            UserDAO dao = new UserDAO(connection);
            assertNotNull(dao.loginUser("user@example.invalid", changed));
            assertNotNull(dao.loginUser("other@example.invalid", PASSWORD));
            assertTrue(dao.changePassword(1, PASSWORD));
        }
    }

    @Test public void docenteCannotReadOrCommentAnotherDocentesTutoria() throws Exception {
        Browser browser = new Browser();
        browser.login("/docente_login.jsp", "/docenteLogin", "doc1@example.invalid", PASSWORD);
        String token = browser.token("/docente/edit_profile.jsp");
        assertEquals(404, browser.get("/docente/comentario.jsp?id=1").statusCode());
        assertEquals(404, browser.post("/updateComentario", "csrfToken=" + token + "&id=1&docenteId=2&comentario=Alterado").statusCode());
        assertEquals(200, browser.get("/docente/index.jsp").statusCode());
        assertEquals(200, browser.get("/docente/alumno.jsp").statusCode());
        browser.login("/docente_login.jsp", "/docenteLogin", "doc2@example.invalid", PASSWORD);
        assertEquals(200, browser.get("/docente/comentario.jsp?id=1").statusCode());
    }

    @Test public void adminPagesCompileAndPasswordIsNeverRendered() throws Exception {
        Browser browser = new Browser();
        browser.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
        for (String page : List.of("/admin/index.jsp", "/admin/docente.jsp", "/admin/list_docente.jsp", "/admin/alumno.jsp", "/admin/edit_docente.jsp?id=1")) {
            HttpResponse<String> response = browser.get(page);
            assertEquals(page + response.body(), 200, response.statusCode());
            assertFalse(response.body().contains("pbkdf2_sha256$"));
        }
        assertEquals(405, browser.get("/deactivateDocente?id=1").statusCode());
        assertEquals(403, browser.post("/deactivateDocente", "id=1").statusCode());
    }

    @Test public void migrationIsIdempotentAndPreservesLegacyAccess() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:migration;MODE=MySQL", "sa", ""); Statement statement = connection.createStatement()) {
            statement.execute("create table user_details(id int primary key, clave varchar(255))");
            statement.execute("create table docente(id int primary key, clave varchar(255))");
            statement.execute("insert into user_details values(1,'oldpass')");
            assertEquals(1, PasswordTool.migrate(connection));
            assertEquals(0, PasswordTool.migrate(connection));
            try (ResultSet result = statement.executeQuery("select clave from user_details")) {
                assertTrue(result.next());
                assertTrue(Passwords.verify("oldpass", result.getString(1)));
            }
        }
    }

    @Test public void registrationRejectsWeakPasswordsAndStoresOnlyHashes() throws Exception {
        Browser browser = new Browser();
        String token = browser.token("/reg_alumno.jsp");
        String fields = "csrfToken=" + token + "&nombre=Nuevo&email=new@example.invalid&clave=";
        assertEquals(400, browser.post("/user_register", fields + "short").statusCode());
        assertEquals(302, browser.post("/user_register", fields + PASSWORD).statusCode());
        try (Connection connection = db(); PreparedStatement statement = connection.prepareStatement("select clave from user_details where email=?")) {
            statement.setString(1, "new@example.invalid");
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertNotEquals(PASSWORD, result.getString(1));
                assertTrue(Passwords.verify(PASSWORD, result.getString(1)));
            }
        }
    }

    @Test public void tutoriaCreationIgnoresSubmittedUserId() throws Exception {
        Browser browser = new Browser();
        browser.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        String token = browser.token("/reg_tutoria.jsp");
        String body = "csrfToken=" + token + "&userId=2&nombre=OwnershipTest&genero=Prueba&edad=20&ftutoria="
                + java.time.LocalDate.now(com.tme.service.ReglasTutoria.ZONA).plusDays(20)
                + "&email=user@example.invalid&telefono=123&experiencia=Prueba&docenteNameSelect=1&horaInicio=10:30&duracionMinutos=60&direccion=Prueba";
        assertEquals(302, browser.post("/addTutoria", body).statusCode());
        assertEquals(200, browser.get("/list_tutoria.jsp").statusCode());
        try (Connection connection = db(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select userId from tutoria where nombre='OwnershipTest'")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test public void docenteProfileAndPasswordUseSessionIdentity() throws Exception {
        Browser browser = new Browser();
        browser.login("/docente_login.jsp", "/docenteLogin", "doc1@example.invalid", PASSWORD);
        String token = browser.token("/docente/edit_profile.jsp");
        assertEquals(302, browser.post("/docenteEditProfile", "csrfToken=" + token + "&docenteId=2&nombre=OwnProfile&fnacim=1990-01-01&calificacion=Prueba&especialidad=Algebra&email=doc1@example.invalid&telefono=123").statusCode());
        token = browser.token("/docente/edit_profile.jsp");
        assertEquals(302, browser.post("/docenteChangePassword", "csrfToken=" + token + "&docenteId=2&oldPassword=" + PASSWORD + "&newPassword=Changed-password-12345").statusCode());
        try (Connection connection = db()) {
            DocenteDAO dao = new DocenteDAO(connection);
            assertEquals("OwnProfile", dao.getDocenteById(1).getNombre());
            assertEquals("Docente Dos", dao.getDocenteById(2).getNombre());
            assertNotNull(dao.loginDocente("doc1@example.invalid", "Changed-password-12345"));
            assertNotNull(dao.loginDocente("doc2@example.invalid", PASSWORD));
            assertTrue(dao.changePassword(1, PASSWORD));
        }
    }

    @Test public void adminCanEditWithoutOverwritingPassword() throws Exception {
        Browser browser = new Browser();
        browser.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
        String token = browser.token("/admin/edit_docente.jsp?id=1");
        assertEquals(302, browser.post("/updateDocente", "csrfToken=" + token + "&id=1&nombre=Docente+Uno&fnacim=1990-01-01&calificacion=Prueba&especialidad=Algebra&email=doc1@example.invalid&telefono=123&clave=").statusCode());
        try (Connection connection = db()) {
            assertNotNull(new DocenteDAO(connection).loginDocente("doc1@example.invalid", PASSWORD));
        }
    }

    @Test public void failedMigrationRollsBackPreviouslyConvertedRows() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:migration_failure;MODE=MySQL", "sa", ""); Statement statement = connection.createStatement()) {
            statement.execute("create table user_details(id int primary key, clave varchar(255))");
            statement.execute("create table docente(id int primary key, clave varchar(255))");
            statement.execute("insert into user_details values(1,'oldpass')");
            statement.execute("insert into docente values(1,'')");
            try { PasswordTool.migrate(connection); fail("Expected migration failure"); }
            catch (IllegalArgumentException expected) { }
            try (ResultSet result = statement.executeQuery("select clave from user_details")) {
                assertTrue(result.next());
                assertEquals("oldpass", result.getString(1));
            }
        }
    }

    @Test public void healthChecksDatabaseAndSchemaWithoutCreatingSessionOrDisclosingErrors() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> healthy = browser.get("/health");
        assertEquals(200, healthy.statusCode());
        assertEquals("{\"status\":\"ok\"}", healthy.body());
        assertTrue(healthy.headers().allValues("set-cookie").isEmpty());
        try (Connection connection = db(); Statement statement = connection.createStatement()) {
            statement.execute("alter table especialidad rename to temporarily_unavailable");
            try {
                HttpResponse<String> unavailable = browser.get("/health");
                assertEquals(503, unavailable.statusCode());
                assertEquals("{\"status\":\"unavailable\"}", unavailable.body());
                assertFalse(unavailable.body().contains("jdbc:"));
            } finally { statement.execute("alter table temporarily_unavailable rename to especialidad"); }
        }
        assertEquals(200, browser.get("/health").statusCode());
    }

    @Test public void publicTutoriaFormExplainsLoginAndRejectsAnonymousSubmission() throws Exception {
        Browser browser = new Browser();
        HttpResponse<String> page = browser.get("/reg_tutoria.jsp");
        assertEquals(page.body(), 200, page.statusCode());
        assertTrue(page.body().contains("Para enviar una solicitud de tutoría, inicia sesión como alumno."));
        assertTrue(page.body().contains("Iniciar sesión para solicitar"));
        assertTrue(page.body().contains("href=\"reg_alumno.jsp\""));
        assertTrue(page.body().contains("name=\"docenteNameSelect\""));
        String token = browser.token("/reg_tutoria.jsp");
        int before;
        try (Connection connection = db(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from tutoria")) {
            result.next(); before = result.getInt(1);
        }
        HttpResponse<String> denied = browser.post("/addTutoria", "csrfToken=" + token + "&userId=1");
        assertEquals(403, denied.statusCode());
        assertTrue(denied.body().contains("inicia sesión con la cuenta correspondiente"));
        try (Connection connection = db(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from tutoria")) {
            result.next(); assertEquals(before, result.getInt(1));
        }
    }

    @Test public void otherRolesSeeTutoriaGuidanceButCannotSubmit() throws Exception {
        for (String role : List.of("docente", "admin")) {
            Browser browser = new Browser();
            if (role.equals("docente")) browser.login("/docente_login.jsp", "/docenteLogin", "doc1@example.invalid", PASSWORD);
            else browser.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
            HttpResponse<String> page = browser.get("/reg_tutoria.jsp");
            assertEquals(page.body(), 200, page.statusCode());
            assertTrue(page.body().contains("Las solicitudes de tutoría se realizan con una cuenta de alumno."));
            assertTrue(page.body().contains("Iniciar sesión para solicitar"));
            assertEquals(403, browser.post("/addTutoria", "csrfToken=" + browser.token("/reg_tutoria.jsp")).statusCode());
        }
    }

    @Test public void docenteCanSaveCommentWithoutReplacingState() throws Exception {
        Browser browser = new Browser();
        browser.login("/docente_login.jsp", "/docenteLogin", "doc2@example.invalid", PASSWORD);
        String token = browser.token("/docente/comentario.jsp?id=1");
        assertEquals(302, browser.post("/updateComentario", "csrfToken=" + token + "&id=1&docenteId=1&comentario=" + enc("Repasar <ecuaciones>")).statusCode());
        try (Connection connection = db()) {
            Tutoria saved = new TutoriaDAO(connection).getTutoriaById(1);
            assertEquals("Pendiente", saved.getEstado());
            assertEquals("Repasar <ecuaciones>", saved.getComentario());
        }
        HttpResponse<String> page = browser.get("/docente/comentario.jsp?id=1");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Repasar &lt;ecuaciones&gt;"));
    }

    @Test public void workflowActionsEnforceCsrfRoleOwnershipAndTimeOverHttp() throws Exception {
        int teacherRequest, studentRequest, adminRequest;
        try (Connection connection = db()) {
            teacherRequest = createWorkflowRequest(connection, 2, 45);
            studentRequest = createWorkflowRequest(connection, 1, 46);
            adminRequest = createWorkflowRequest(connection, 2, 47);
        }
        Browser teacher = new Browser();
        teacher.login("/docente_login.jsp", "/docenteLogin", "doc2@example.invalid", PASSWORD);
        String teacherToken = teacher.token("/docente/alumno.jsp");
        assertTrue(teacher.get("/docente/alumno.jsp").body().contains(">Aceptar</button>"));
        assertEquals(405, teacher.get("/cambiarEstadoTutoria?id=" + teacherRequest + "&estado=Aceptada").statusCode());
        assertEquals(403, teacher.post("/cambiarEstadoTutoria", "id=" + teacherRequest + "&estado=Aceptada").statusCode());
        assertEquals(400, teacher.post("/cambiarEstadoTutoria", "csrfToken=" + teacherToken + "&id=" + teacherRequest + "&estado=Inventado").statusCode());
        assertEquals(302, teacher.post("/cambiarEstadoTutoria", "csrfToken=" + teacherToken + "&id=" + teacherRequest + "&docenteId=1&estado=Aceptada").statusCode());
        assertEquals(302, teacher.post("/cambiarEstadoTutoria", "csrfToken=" + teacherToken + "&id=" + teacherRequest + "&estado=Completada").statusCode());
        assertTrue(teacher.get("/docente/alumno.jsp").body().contains("después de su hora de finalización"));
        try (Connection connection = db()) {
            assertEquals("Aceptada", new TutoriaDAO(connection).getTutoriaById(teacherRequest).getEstado());
        }
        Browser student = new Browser();
        student.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        String studentToken = student.token("/list_tutoria.jsp");
        assertTrue(student.get("/list_tutoria.jsp").body().contains("/cancelarTutoria"));
        assertEquals(403, student.post("/cambiarEstadoTutoria", "csrfToken=" + studentToken + "&id=" + teacherRequest + "&estado=Cancelada").statusCode());
        assertEquals(404, student.post("/cancelarTutoria", "csrfToken=" + studentToken + "&id=" + teacherRequest + "&userId=2").statusCode());
        assertEquals(403, student.post("/cancelarTutoria", "id=" + studentRequest).statusCode());
        assertEquals(302, student.post("/cancelarTutoria", "csrfToken=" + studentToken + "&id=" + studentRequest + "&estado=Aceptada&userId=2").statusCode());
        Browser admin = new Browser();
        admin.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
        String adminToken = admin.token("/admin/alumno.jsp");
        assertEquals(403, student.post("/adminCancelarTutoria", "csrfToken=" + studentToken + "&id=" + adminRequest).statusCode());
        assertEquals(403, admin.post("/adminCancelarTutoria", "id=" + adminRequest).statusCode());
        assertEquals(302, admin.post("/adminCancelarTutoria", "csrfToken=" + adminToken + "&id=" + adminRequest).statusCode());
        try (Connection connection = db(); PreparedStatement statement = connection.prepareStatement("update tutoria set ftutoria=? where id=?")) {
            assertEquals("Cancelada", new TutoriaDAO(connection).getTutoriaById(studentRequest).getEstado());
            assertEquals("Cancelada", new TutoriaDAO(connection).getTutoriaById(adminRequest).getEstado());
            statement.setString(1, java.time.LocalDate.now(com.tme.service.ReglasTutoria.ZONA).minusDays(1).toString());
            statement.setInt(2, teacherRequest); statement.executeUpdate();
        }
        assertEquals(302, teacher.post("/cambiarEstadoTutoria", "csrfToken=" + teacherToken + "&id=" + teacherRequest + "&estado=Completada").statusCode());
        try (Connection connection = db()) {
            assertEquals("Completada", new TutoriaDAO(connection).getTutoriaById(teacherRequest).getEstado());
        }
        assertEquals(302, admin.post("/adminCancelarTutoria", "csrfToken=" + adminToken + "&id=" + teacherRequest).statusCode());
        assertTrue(admin.get("/admin/alumno.jsp").body().contains("ya no permite esa acción"));
    }

    private static int createWorkflowRequest(Connection connection, int userId, int days) {
        Tutoria tutoria = new Tutoria(userId, "Workflow", "Prueba", "20",
                java.time.LocalDate.now(com.tme.service.ReglasTutoria.ZONA).plusDays(days).toString(),
                "other@example.invalid", "0", "Prueba", 2, "Prueba", "Pendiente");
        tutoria.setHoraInicio(java.time.LocalTime.of(10, 0)); tutoria.setDuracionMinutos(60);
        TutoriaDAO dao = new TutoriaDAO(connection);
        assertTrue(dao.addTutoria(tutoria));
        return dao.getAllTutoria().get(0).getId();
    }

    @Test public void rolePanelsFilterRecordsWithoutTrustingIdentityFromTheUrl() throws Exception {
        Browser student = new Browser();
        student.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        HttpResponse<String> own = student.get("/list_tutoria.jsp?buscar=Privado&userId=2");
        assertEquals(200, own.statusCode());
        assertFalse(own.body().contains("<strong>Privado</strong>"));
        assertTrue(own.body().contains("No hay tutorías para mostrar"));
        HttpResponse<String> invalid = student.get("/list_tutoria.jsp?estado=Inventado");
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("Estado de tutoría inválido"));
        String attack = "<img src=x onerror=alert(1)>";
        HttpResponse<String> escaped = student.get("/list_tutoria.jsp?buscar=" + enc(attack));
        assertEquals(200, escaped.statusCode());
        assertFalse(escaped.body().contains(attack));
        assertTrue(escaped.body().contains("&lt;img"));
        Browser teacher = new Browser();
        teacher.login("/docente_login.jsp", "/docenteLogin", "doc1@example.invalid", PASSWORD);
        assertFalse(teacher.get("/docente/alumno.jsp?buscar=Privado&docenteId=2").body().contains("<strong>Privado</strong>"));
        assertTrue(teacher.get("/docente/index.jsp").body().contains("Por revisar"));
        Browser admin = new Browser();
        admin.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
        assertTrue(admin.get("/admin/alumno.jsp?buscar=Privado").body().contains("<strong>Privado</strong>"));
        assertTrue(admin.get("/admin/index.jsp").body().contains("Docentes activos"));
        assertEquals(404, student.get("/WEB-INF/panel-tutorias.jsp").statusCode());
    }

    @Test public void paginationLinksPreserveFiltersAndBoundDisplayedRecords() throws Exception {
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = db()) {
            try (PreparedStatement statement = connection.prepareStatement("update tutoria set nombre='PaginacionHttp' where id=?")) {
                for (int i = 0; i < 12; i++) {
                    int id = createWorkflowRequest(connection, 2, 80 + i); ids.add(id);
                    statement.setInt(1, id); statement.executeUpdate();
                }
            }
            try {
                Browser browser = new Browser();
                browser.login("/alumno_login.jsp", "/userLogin", "other@example.invalid", PASSWORD);
                HttpResponse<String> first = browser.get("/list_tutoria.jsp?buscar=PaginacionHttp&estado=Pendiente");
                assertEquals(200, first.statusCode());
                assertEquals(10, first.body().split("<strong>PaginacionHttp</strong>", -1).length - 1);
                assertTrue(first.body().contains("Página 1 de 2"));
                assertTrue(first.body().contains("buscar=PaginacionHttp"));
                assertTrue(first.body().contains("pagina=2"));
                HttpResponse<String> last = browser.get("/list_tutoria.jsp?buscar=PaginacionHttp&estado=Pendiente&pagina=2");
                assertEquals(200, last.statusCode());
                assertEquals(2, last.body().split("<strong>PaginacionHttp</strong>", -1).length - 1);
                assertTrue(last.body().contains("Página 2 de 2"));
            } finally {
                try (PreparedStatement statement = connection.prepareStatement("delete from tutoria where id=?")) {
                    for (int id : ids) { statement.setInt(1, id); statement.executeUpdate(); }
                }
            }
        }
    }

    @Test public void failedBookingKeepsEscapedFormValuesAndPrefillsAccountData() throws Exception {
        Browser browser = new Browser();
        browser.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        HttpResponse<String> form = browser.get("/reg_tutoria.jsp");
        assertEquals(200, form.statusCode());
        assertTrue(form.body().contains("value=\"user@example.invalid\""));
        String token = browser.token("/reg_tutoria.jsp");
        String fields = "csrfToken=" + token + "&nombre=" + enc("Alumno <prueba>") + "&genero=masculino&edad=20&ftutoria=2020-01-01"
                + "&email=user@example.invalid&telefono=00123&experiencia=Algebra&docenteNameSelect=2&horaInicio=10:30&duracionMinutos=60&direccion=" + enc("Lugar <prueba>");
        HttpResponse<String> rejected = browser.post("/addTutoria", fields);
        assertEquals(302, rejected.statusCode());
        assertTrue(rejected.headers().firstValue("location").orElse("").endsWith("/reg_tutoria.jsp"));
        HttpResponse<String> restored = browser.get("/reg_tutoria.jsp");
        assertTrue(restored.body().contains("fecha y hora futuras"));
        assertTrue(restored.body().contains("Alumno &lt;prueba&gt;"));
        assertTrue(restored.body().contains("Lugar &lt;prueba&gt;"));
        assertTrue(restored.body().contains("value=\"00123\""));
        assertTrue(restored.body().contains("value=\"2\" selected"));
        assertFalse(restored.body().contains("Alumno <prueba>"));
        assertFalse(browser.get("/reg_tutoria.jsp").body().contains("Alumno &lt;prueba&gt;"));
    }

    @Test public void reactivationRequiresAdminAndCsrfWithoutLosingCredentialsOrHistory() throws Exception {
        int id;
        try (Connection connection = db()) {
            DocenteDAO dao = new DocenteDAO(connection);
            assertTrue(dao.registerDocente(new Docente("Reactivar", "1990-01-01", "Prueba", "Algebra", "reactivate@example.invalid", "0", PASSWORD)));
            id = dao.loginDocente("reactivate@example.invalid", PASSWORD).getId();
            assertTrue(dao.deactivateDocenteById(id));
        }
        Browser student = new Browser();
        student.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        assertEquals(403, student.post("/reactivateDocente", "id=" + id + "&csrfToken=" + student.token("/change_password.jsp")).statusCode());
        Browser admin = new Browser();
        admin.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
        assertTrue(admin.get("/admin/list_docente.jsp").body().contains("Reactivar</button>"));
        assertEquals(405, admin.get("/reactivateDocente?id=" + id).statusCode());
        assertEquals(403, admin.post("/reactivateDocente", "id=" + id).statusCode());
        assertEquals(302, admin.post("/reactivateDocente", "csrfToken=" + admin.token("/admin/list_docente.jsp") + "&id=" + id).statusCode());
        try (Connection connection = db()) {
            assertNotNull(new DocenteDAO(connection).loginDocente("reactivate@example.invalid", PASSWORD));
        }
        assertTrue(new Browser().get("/reg_tutoria.jsp").body().contains("Reactivar (Algebra)"));
    }

    @Test public void deactivationRevokesExistingSessionAndPreservesTutoria() throws Exception {
        int id;
        int tutoriaId;
        try (Connection connection = db()) {
            DocenteDAO docentes = new DocenteDAO(connection);
            assertTrue(docentes.registerDocente(new Docente("Temporal", "1990-01-01", "Prueba", "Algebra", "inactive@example.invalid", "0", PASSWORD)));
            id = docentes.loginDocente("inactive@example.invalid", PASSWORD).getId();
            Tutoria tutoria = new Tutoria(1, "Historia", "Prueba", "20", java.time.LocalDate.now(com.tme.service.ReglasTutoria.ZONA).plusDays(30).toString(), "user@example.invalid", "0", "Prueba", id, "Prueba", "Pendiente");
            tutoria.setHoraInicio(java.time.LocalTime.of(11, 0));
            tutoria.setDuracionMinutos(60);
            assertTrue(new TutoriaDAO(connection).addTutoria(tutoria));
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("select max(id) from tutoria")) {
                result.next(); tutoriaId = result.getInt(1);
            }
        }
        Browser docente = new Browser();
        docente.login("/docente_login.jsp", "/docenteLogin", "inactive@example.invalid", PASSWORD);
        assertEquals(200, docente.get("/docente/index.jsp").statusCode());
        Browser admin = new Browser();
        admin.login("/admin_login.jsp", "/adminLogin", "admin@example.invalid", "Admin-test-12345");
        String token = admin.token("/admin/list_docente.jsp");
        assertEquals(302, admin.post("/deactivateDocente", "csrfToken=" + token + "&id=" + id).statusCode());
        assertEquals(302, docente.get("/docente/index.jsp").statusCode());
        try (Connection connection = db()) {
            assertNull(new DocenteDAO(connection).loginDocente("inactive@example.invalid", PASSWORD));
            assertNotNull(new TutoriaDAO(connection).getTutoriaById(tutoriaId));
        }
        String publicPage = new Browser().get("/reg_tutoria.jsp").body();
        assertFalse(publicPage.contains("Temporal (Algebra)"));
        assertTrue(admin.get("/admin/list_docente.jsp").body().contains("Historial conservado"));
    }
}
