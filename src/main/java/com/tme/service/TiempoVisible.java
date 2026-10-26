package com.tme.service;

import java.math.BigDecimal;

/** Human durations at the UI boundary; persistence and scheduling remain in minutes. */
public final class TiempoVisible {
    private TiempoVisible() { }
    public static String duracion(Integer minutos) {
        if (minutos == null) return "Por definir";
        int horas = minutos / 60, resto = minutos % 60;
        if (horas == 0) return resto + " min";
        return horas + (horas == 1 ? " hora" : " horas") + (resto == 0 ? "" : " y " + resto + " min");
    }
    // Multiples of three have an exact decimal-hour representation; keep other legacy values in minutes.
    public static boolean enHoras(Integer minutos) { return minutos == null || minutos % 3 == 0; }
    public static String cantidad(Integer minutos) {
        if (minutos == null) return "";
        return enHoras(minutos) ? BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60)).stripTrailingZeros().toPlainString() : minutos.toString();
    }
    public static int minutos(String cantidad, String unidad) {
        try {
            if (unidad != null && !unidad.equals("horas") && !unidad.equals("minutos")) throw new IllegalArgumentException();
            BigDecimal valor = new BigDecimal(cantidad);
            if ("horas".equals(unidad)) valor = valor.multiply(BigDecimal.valueOf(60));
            int minutos = valor.intValueExact();
            if (minutos <= 0) throw new IllegalArgumentException();
            return minutos;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Indica un plazo válido. Por ejemplo: 2 horas o 30 minutos.");
        }
    }
}
