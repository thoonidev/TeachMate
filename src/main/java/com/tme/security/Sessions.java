package com.tme.security;

import java.security.SecureRandom;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class Sessions {
    private static final SecureRandom RANDOM = new SecureRandom();
    private Sessions() { }

    public static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static HttpSession login(HttpServletRequest request, String role, Object principal) {
        HttpSession previous = request.getSession(false);
        if (previous != null) previous.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(role, principal);
        session.setAttribute("csrfToken", token());
        return session;
    }
}
