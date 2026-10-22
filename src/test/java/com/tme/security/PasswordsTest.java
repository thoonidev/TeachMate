package com.tme.security;

import static org.junit.Assert.*;
import org.junit.Test;

public class PasswordsTest {
    @Test public void saltedHashesVerifyWithoutAcceptingPlaintext() {
        String password = "Una contraseña larga 123";
        String first = Passwords.hash(password);
        assertNotEquals(first, Passwords.hash(password));
        assertTrue(Passwords.verify(password, first));
        assertFalse(Passwords.verify("incorrecta", first));
        assertFalse(Passwords.verify(password, password));
        assertFalse(Passwords.verify(password, "pbkdf2_sha256$999999999$bad$bad"));
        assertFalse(Passwords.verify(null, first));
    }
    @Test public void newPasswordsHaveBoundsButLegacyMigrationPreservesAccess() {
        assertFalse(Passwords.acceptable("corta"));
        assertFalse(Passwords.acceptable("x".repeat(129)));
        assertTrue(Passwords.verify("corta", Passwords.hashForMigration("corta")));
    }
    @Test public void htmlEscapesTextAndQuotedAttributes() {
        assertEquals("&lt;script&gt;&amp;&quot;&#39;", Html.escape("<script>&\"'"));
        assertEquals("", Html.escape(null));
    }
    @Test public void rolesCoverPagesAndStandaloneActions() {
        for (String path : new String[] {"/admin/", "/admin/alumno.jsp", "/deleteDocente", "/addEspecialidad", "/updateDocente"})
            assertEquals("adminObj", SecurityFilter.requiredRole(path));
        for (String path : new String[] {"/docente/comentario.jsp", "/updateEstado", "/docenteChangePassword"})
            assertEquals("docenteObj", SecurityFilter.requiredRole(path));
        assertEquals("userObj", SecurityFilter.requiredRole("/userChangePassword"));
        assertNull(SecurityFilter.requiredRole("/admin_login.jsp"));
    }
    @Test public void csrfRequiresMatchingSessionToken() {
        String token = Sessions.token();
        assertTrue(SecurityFilter.validToken(token, token));
        assertFalse(SecurityFilter.validToken(token, Sessions.token()));
        assertFalse(SecurityFilter.validToken(null, null));
        assertFalse(SecurityFilter.validToken(token, null));
    }
    @Test public void authenticationAttemptsAreBoundedPerAddress() {
        for (int i = 0; i < 30; i++) assertTrue(SecurityFilter.allowAttempt("test-address"));
        assertFalse(SecurityFilter.allowAttempt("test-address"));
        assertTrue(SecurityFilter.allowAttempt("different-address"));
    }
}
