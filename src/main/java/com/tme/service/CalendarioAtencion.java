package com.tme.service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/** Weekly review window in Lima; payment deadlines themselves use elapsed time. */
public record CalendarioAtencion(int dias, LocalTime desde, LocalTime hasta, int revisionMinutos) {
    public CalendarioAtencion {
        if (dias < 1 || dias > 127 || desde == null || hasta == null || !desde.isBefore(hasta)
                || desde.getSecond() != 0 || hasta.getSecond() != 0 || desde.getNano() != 0 || hasta.getNano() != 0
                || Duration.between(desde,hasta).toMinutes() < 15 || revisionMinutos < 15 || revisionMinutos > 10080)
            throw new IllegalArgumentException("Selecciona días de atención, una franja de al menos 15 minutos y un margen de revisión de 15 minutos a 7 días.");
    }
    public boolean atiende(LocalDate fecha) { return (dias & (1 << (fecha.getDayOfWeek().getValue()-1))) != 0; }
    public LocalDateTime sumarRevision(LocalDateTime inicio) { return desplazar(inicio, false); }
    public LocalDateTime limiteComprobante(LocalDateTime clase) { return desplazar(clase, true); }

    private LocalDateTime desplazar(LocalDateTime instante, boolean atras) {
        Duration falta = Duration.ofMinutes(revisionMinutos);
        LocalDate fecha = instante.toLocalDate();
        while (true) {
            LocalDateTime apertura=fecha.atTime(desde), cierre=fecha.atTime(hasta);
            if (atiende(fecha)) {
                LocalDateTime cursor=atras ? (instante.isBefore(cierre)?instante:cierre)
                    : (instante.isAfter(apertura)?instante:apertura);
                if (atras ? cursor.isAfter(apertura) : cursor.isBefore(cierre)) {
                    Duration disponible=atras?Duration.between(apertura,cursor):Duration.between(cursor,cierre);
                    if (falta.compareTo(disponible)<=0) return atras?cursor.minus(falta):cursor.plus(falta);
                    falta=falta.minus(disponible);
                }
            }
            fecha=atras?fecha.minusDays(1):fecha.plusDays(1);
            instante=atras?fecha.atTime(hasta):fecha.atTime(desde);
        }
    }
    public String descripcion() {
        String[] nombres={"lunes","martes","miércoles","jueves","viernes","sábado","domingo"};
        List<String> seleccion=new ArrayList<>();
        for(int i=0;i<7;i++)if((dias&(1<<i))!=0)seleccion.add(nombres[i]);
        return String.join(", ",seleccion)+", "+desde+"–"+hasta+" (Lima)";
    }
}
