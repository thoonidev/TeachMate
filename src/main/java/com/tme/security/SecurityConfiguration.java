package com.tme.security;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebListener;

@WebListener
public class SecurityConfiguration implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        String adminHash = System.getenv("ADMIN_PASSWORD_HASH");
        if (adminHash != null && !adminHash.isBlank() && !Passwords.isEncoded(adminHash)) {
            throw new IllegalStateException("ADMIN_PASSWORD_HASH no tiene un formato válido.");
        }
        SessionCookieConfig cookie = event.getServletContext().getSessionCookieConfig();
        cookie.setHttpOnly(true);
        cookie.setSecure(Boolean.parseBoolean(System.getenv("SESSION_COOKIE_SECURE")));
    }
}
