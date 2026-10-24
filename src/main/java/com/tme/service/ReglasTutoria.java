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

    public static boolean confirmada(Tutoria t) { return t.getEstado().equals("Confirmada") || t.getEstado().equals("Aceptada"); }
    public static boolean vigente(Tutoria t,LocalDateTime ahora) {
        LocalDateTime inicio=inicio(t);
        if(t.getEstado().equals("Pendiente")) return (t.getVenceRespuesta()==null||ahora.isBefore(t.getVenceRespuesta()))&&(inicio==null||ahora.isBefore(inicio));
        if(t.getEstado().equals("Pendiente de pago")) return (t.isPagoRevision()||(t.getVencePago()!=null&&ahora.isBefore(t.getVencePago())))&&(inicio==null||ahora.isBefore(inicio));
        return confirmada(t)||t.getEstado().equals("Incidencia");
    }
    public static boolean puedeEntrar(Tutoria t,LocalDateTime ahora) {
        LocalDateTime a=inicio(t);return confirmada(t)&&a!=null&&ahora.isBefore(a.plusMinutes(t.getDuracionMinutos()))&&!t.getEnlaceClase().isBlank();
    }
}
