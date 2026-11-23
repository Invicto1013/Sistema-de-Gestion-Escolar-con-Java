package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReglasAsistenciaTest {

    @Test
    @DisplayName("Cálculo de porcentaje de asistencia con límites y redondeo")
    void calcularPorcentaje_limitesYExactitud() {
        assertEquals(0.0, ReglasAsistencia.calcularPorcentaje(0, 0), 0.001);
        assertEquals(0.0, ReglasAsistencia.calcularPorcentaje(20, 0), 0.001);
        assertEquals(100.0, ReglasAsistencia.calcularPorcentaje(20, 20), 0.001);
        assertEquals(100.0, ReglasAsistencia.calcularPorcentaje(20, 25), 0.001);

        // 17 de 20 = 85.0%
        assertEquals(85.0, ReglasAsistencia.calcularPorcentaje(20, 17), 0.001);

        // 15 de 19 = 78.95%
        assertEquals(78.95, ReglasAsistencia.calcularPorcentaje(19, 15), 0.001);
    }

    @Test
    @DisplayName("Asistencia regular (>= 80%) otorga derecho a examen")
    void evaluar_asistenciaRegular_otorgaDerecho() {
        ResumenAsistencia r = new ResumenAsistencia("2026-1", "MAT101", "Matemáticas", 20, 17, 3);
        ReglasAsistencia.EvaluacionAsistencia eval = ReglasAsistencia.evaluar(r);

        assertEquals(85.0, eval.porcentajeAsistencia(), 0.001);
        assertEquals(ReglasAsistencia.EstadoAsistencia.REGULAR, eval.estado());
        assertTrue(eval.tieneDerechoExamen());
    }

    @Test
    @DisplayName("Asistencia en alerta (70% - 79%) mantiene derecho pero en observación")
    void evaluar_asistenciaAlerta_alertaPeroConDerecho() {
        ResumenAsistencia r = new ResumenAsistencia("2026-1", "MAT101", "Matemáticas", 20, 15, 5);
        ReglasAsistencia.EvaluacionAsistencia eval = ReglasAsistencia.evaluar(r);

        assertEquals(75.0, eval.porcentajeAsistencia(), 0.001);
        assertEquals(ReglasAsistencia.EstadoAsistencia.ALERTA, eval.estado());
        assertTrue(eval.tieneDerechoExamen());
    }

    @Test
    @DisplayName("Asistencia deficiente (< 70%) pierde derecho a examen por inasistencia")
    void evaluar_asistenciaDeficiente_pierdeDerecho() {
        ResumenAsistencia r = new ResumenAsistencia("2026-1", "MAT101", "Matemáticas", 20, 13, 7);
        ReglasAsistencia.EvaluacionAsistencia eval = ReglasAsistencia.evaluar(r);

        assertEquals(65.0, eval.porcentajeAsistencia(), 0.001);
        assertEquals(ReglasAsistencia.EstadoAsistencia.SIN_DERECHO, eval.estado());
        assertFalse(eval.tieneDerechoExamen());
    }

    @Test
    @DisplayName("Consolidado de asistencia para múltiples materias inscritas")
    void evaluarConsolidado_evaluaConjuntoMaterias() {
        List<ResumenAsistencia> materias = List.of(
                new ResumenAsistencia("2026-1", "MAT101", "Matemáticas", 20, 18, 2),
                new ResumenAsistencia("2026-1", "ESP101", "Español", 20, 14, 6)
        );

        // Total: 32 presentes de 40 = 80.0%
        ReglasAsistencia.EvaluacionAsistencia eval = ReglasAsistencia.evaluarConsolidado(materias);

        assertEquals(40, eval.clasesTotales());
        assertEquals(32, eval.asistencias());
        assertEquals(8, eval.inasistencias());
        assertEquals(80.0, eval.porcentajeAsistencia(), 0.001);
        assertEquals(ReglasAsistencia.EstadoAsistencia.REGULAR, eval.estado());
        assertTrue(eval.tieneDerechoExamen());
    }
}
