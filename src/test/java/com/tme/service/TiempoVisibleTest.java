package com.tme.service;

import org.junit.Test;
import static org.junit.Assert.*;

public class TiempoVisibleTest {
    @Test public void plazosSeLeenEnHorasSinPerderMinutos() {
        assertEquals("24 horas",TiempoVisible.duracion(1440));
        assertEquals("1 hora",TiempoVisible.duracion(60));
        assertEquals("1 hora y 30 min",TiempoVisible.duracion(90));
        assertEquals("45 min",TiempoVisible.duracion(45));
    }
    @Test public void todosLosPlazosExistentesSeConservanAlGuardar() {
        for(int minutos=15;minutos<=43200;minutos++)
            assertEquals(minutos,TiempoVisible.minutos(TiempoVisible.cantidad(minutos),TiempoVisible.enHoras(minutos)?"horas":"minutos"));
        assertEquals(90,TiempoVisible.minutos("1.5","horas"));
        assertEquals(30,TiempoVisible.minutos("30",null)); // Existing minute-based requests.
    }
    @Test public void noSeRedondeanPlazosNiSeAceptanUnidadesDesconocidas() {
        for(String cantidad:new String[]{"0","-1","abc","0.333","2147483648"})
            assertThrows(IllegalArgumentException.class,()->TiempoVisible.minutos(cantidad,"horas"));
        assertThrows(IllegalArgumentException.class,()->TiempoVisible.minutos("2","dias"));
        assertThrows(IllegalArgumentException.class,()->TiempoVisible.minutos(null,"horas"));
    }
    @Test public void horarioResumeDiasConsecutivosSinCambiarLosAlternos() {
        assertEquals("de lunes a sábado, 09:00–18:00 (Lima)",new CalendarioAtencion(63,java.time.LocalTime.of(9,0),java.time.LocalTime.of(18,0),120).descripcion());
        assertEquals("lunes, miércoles, viernes, 09:00–18:00 (Lima)",new CalendarioAtencion(21,java.time.LocalTime.of(9,0),java.time.LocalTime.of(18,0),120).descripcion());
    }
}
