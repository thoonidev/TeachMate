package com.tme.service;

import com.tme.modal.EstadoTutoria;
import com.tme.modal.Tutoria;
import java.time.*;

/** Shared by the server and views. All appointments use Lima local time. */
public final class ReglasTutoria {
    public static final ZoneId ZONA = ZoneId.of("America/Lima");
    public enum Rol { ALUMNO, DOCENTE, ADMIN }

    private ReglasTutoria() { }

    public static LocalDateTime ahora() { return LocalDateTime.now(ZONA); }

    public static LocalDateTime inicio(Tutoria tutoria) {
        try {
            if (tutoria.getHoraInicio() == null || tutoria.getDuracionMinutos() == null) return null;
            return LocalDate.parse(tutoria.getFtutoria()).atTime(tutoria.getHoraInicio());
        } catch (DateTimeException | NullPointerException exception) { return null; }
    }

    public static void validarHorario(Tutoria tutoria, LocalDateTime ahora) {
        LocalDateTime inicio = inicio(tutoria);
        Integer minutos = tutoria.getDuracionMinutos();
        if (inicio == null || minutos < 15 || minutos > 180
                || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new IllegalArgumentException("Indica una fecha, hora y duración válidas (15 a 180 minutos).");
        }
        if (!inicio.isAfter(ahora)) throw new IllegalArgumentException("La tutoría debe comenzar en una fecha y hora futuras (hora de Lima).");
        if (!inicio.toLocalDate().equals(inicio.plusMinutes(minutos).toLocalDate())) {
            throw new IllegalArgumentException("La tutoría debe comenzar y terminar el mismo día.");
        }
    }

    public static boolean seCruzan(Tutoria primera, Tutoria segunda) {
        LocalDateTime a = inicio(primera), b = inicio(segunda);
        return a != null && b != null && a.isBefore(b.plusMinutes(segunda.getDuracionMinutos()))
                && b.isBefore(a.plusMinutes(primera.getDuracionMinutos()));
    }

    public static boolean puedeCambiar(Tutoria tutoria, EstadoTutoria siguiente, Rol rol, LocalDateTime ahora) {
        return errorCambio(tutoria, siguiente, rol, ahora) == null;
    }

    public static void validarCambio(Tutoria tutoria, EstadoTutoria siguiente, Rol rol, LocalDateTime ahora) {
        String error = errorCambio(tutoria, siguiente, rol, ahora);
        if (error != null) throw new IllegalArgumentException(error);
    }

    private static String errorCambio(Tutoria tutoria, EstadoTutoria siguiente, Rol rol, LocalDateTime ahora) {
        if (!EstadoTutoria.desdeValor(tutoria.getEstado()).puedeCambiarA(siguiente)) {
            return "El estado de esta tutoría ya no permite esa acción. Actualiza la página.";
        }
        if (rol == null || (rol != Rol.DOCENTE && siguiente != EstadoTutoria.CANCELADA)) {
            return "Tu rol no permite realizar esta acción.";
        }
        LocalDateTime inicio = inicio(tutoria);
        if (siguiente == EstadoTutoria.ACEPTADA) {
            try { validarHorario(tutoria, ahora); }
            catch (IllegalArgumentException exception) { return exception.getMessage(); }
        }
        if (siguiente == EstadoTutoria.COMPLETADA && (inicio == null
                || ahora.isBefore(inicio.plusMinutes(tutoria.getDuracionMinutos())))) {
            return "Solo puedes completar una tutoría aceptada después de su hora de finalización.";
        }
        if (siguiente == EstadoTutoria.CANCELADA && rol != Rol.ADMIN && inicio != null && !inicio.isAfter(ahora)) {
            return "La tutoría ya comenzó. Contacta al administrador si necesitas cancelarla.";
        }
        return null;
    }
}
