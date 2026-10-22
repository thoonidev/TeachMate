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
        for (String route : List.of("/admin/alumno.jsp", "/admin/list_docente.jsp", "/docente/comentario.jsp?id=1", "/userChangePassword"))
            assertEquals(route, 403, browser.get(route).statusCode());
        assertEquals(403, browser.post("/addDocente", "").statusCode());
        browser.login("/alumno_login.jsp", "/userLogin", "user@example.invalid", PASSWORD);
        assertEquals(403, browser.get("/admin/index.jsp").statusCode());
        assertEquals(403, browser.post("/updateEstado", "").statusCode());
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
        assertEquals(403, browser.get("/change_password.jsp").statusCode());
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
        assertEquals(404, browser.post("/updateEstado", "csrfToken=" + token + "&id=1&docenteId=2&comentario=Alterado").statusCode());
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
        assertEquals(405, browser.get("/deleteDocente?id=1").statusCode());
        assertEquals(403, browser.post("/deleteDocente", "id=1").statusCode());
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
        String body = "csrfToken=" + token + "&userId=2&nombre=OwnershipTest&genero=Prueba&edad=20&ftutoria=2026-10-22&email=user@example.invalid&telefono=123&experiencia=Prueba&docenteNameSelect=1&direccion=Prueba";
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
}
