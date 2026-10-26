package com.tme.service;

import java.time.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalendarioAtencionTest {
    private final CalendarioAtencion calendario=new CalendarioAtencion(63,LocalTime.of(9,0),LocalTime.of(18,0),120);
    private LocalDateTime fecha(String texto){return LocalDateTime.parse(texto);}

    @Test public void madrugadaEsperaApertura() {
        assertEquals(fecha("2030-01-07T11:00"),calendario.sumarRevision(fecha("2030-01-07T02:00")));
    }
    @Test public void sabadoDeNocheSaltaDomingo() {
        assertEquals(fecha("2030-01-14T11:00"),calendario.sumarRevision(fecha("2030-01-12T23:00")));
    }
    @Test public void reparteMargenEntreDiasDeAtencion() {
        assertEquals(fecha("2030-01-14T10:00"),calendario.sumarRevision(fecha("2030-01-12T17:00")));
    }
    @Test public void permiteTerminarAlCierrePeroNoCuentaTiempoFueraDeAtencion() {
        assertEquals(fecha("2030-01-12T18:00"),calendario.sumarRevision(fecha("2030-01-12T16:00")));
        assertEquals(fecha("2030-01-14T11:00"),calendario.sumarRevision(fecha("2030-01-12T18:00")));
    }
    @Test public void calculaLimiteDeCorreccionHaciaAtras() {
        LocalDateTime clase=fecha("2030-01-14T10:00");
        assertEquals(fecha("2030-01-12T17:00"),calendario.limiteComprobante(clase));
        assertEquals(clase,calendario.sumarRevision(calendario.limiteComprobante(clase)));
    }
    @Test public void conservaSegundosSinAdelantarElPlazo() {
        assertEquals(fecha("2030-01-14T10:00:30"),calendario.sumarRevision(fecha("2030-01-12T17:00:30")));
    }
    @Test public void permiteClasesFueraDeLaAtencionSiLaRevisionCabeAntes() {
        assertEquals(fecha("2030-01-12T16:00"),calendario.limiteComprobante(fecha("2030-01-13T10:00")));
    }
    @Test public void rechazaHorariosSinDiasOInvertidos() {
        assertThrows(IllegalArgumentException.class,()->new CalendarioAtencion(0,LocalTime.of(9,0),LocalTime.of(18,0),120));
        assertThrows(IllegalArgumentException.class,()->new CalendarioAtencion(128,LocalTime.of(9,0),LocalTime.of(18,0),120));
        assertThrows(IllegalArgumentException.class,()->new CalendarioAtencion(63,LocalTime.of(18,0),LocalTime.of(9,0),120));
        assertThrows(IllegalArgumentException.class,()->new CalendarioAtencion(63,LocalTime.of(9,0),LocalTime.of(18,0),0));
    }
}
