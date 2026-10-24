package com.tme.security;

public final class Html {
    private Html() { }
    /** For HTML text and quoted HTML attributes, never JavaScript. */
    public static String escape(Object value) {
        if (value == null) return "";
        return value.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
