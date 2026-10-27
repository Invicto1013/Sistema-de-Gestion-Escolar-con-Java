package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResumenAsistenciaTest {
    @Test
    void sinClasesRegistradasElPorcentajeEsCero() {
        assertEquals(0.0, new ResumenAsistencia("2026-1", "MAT1", "Matemática", 0, 0, 0).porcentaje(), 0.0);
    }

    @Test
    void calculaElPorcentajeDePresentes() {
        assertEquals(75.0, new ResumenAsistencia("2026-1", "MAT1", "Matemática", 8, 6, 2).porcentaje(), 0.0001);
    }

    @Test
    void asistenciaCompletaYNulaEnLosExtremos() {
        assertEquals(100.0, new ResumenAsistencia("2026-1", "LEN1", "Lengua", 10, 10, 0).porcentaje(), 0.0);
        assertEquals(0.0, new ResumenAsistencia("2026-1", "LEN1", "Lengua", 10, 0, 10).porcentaje(), 0.0);
    }
}
