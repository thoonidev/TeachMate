package com.tme.service;

import com.tme.modal.EstadoTutoria;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Only display filters: identity and role always come from the authenticated session. */
public record FiltroTutorias(String estado, String desde, String hasta, String buscar, int pagina) {
    public static final int TAMANO = 10;
    public FiltroTutorias {
        estado = limpio(estado); desde = limpio(desde); hasta = limpio(hasta); buscar = limpio(buscar);
        if (!estado.isEmpty()) EstadoTutoria.desdeValor(estado);
        try {
            if (!desde.isEmpty()) fecha(desde);
            if (!hasta.isEmpty()) fecha(hasta);
        } catch (DateTimeParseException exception) { throw new IllegalArgumentException("Revisa las fechas del filtro."); }
        if (!desde.isEmpty() && !hasta.isEmpty() && desde.compareTo(hasta) > 0)
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final.");
        if (buscar.length() > 60 || buscar.indexOf('\0') >= 0)
            throw new IllegalArgumentException("La búsqueda debe tener como máximo 60 caracteres.");
        if (pagina < 1) throw new IllegalArgumentException("Página inválida.");
    }
    private static String limpio(String value) { return value == null ? "" : value.strip(); }
    private static void fecha(String value) {
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
            throw new IllegalArgumentException("Revisa las fechas del filtro.");
        LocalDate.parse(value);
    }
    public static int pagina(String value) {
        try { return value == null || value.isBlank() ? 1 : Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Página inválida."); }
    }
}
