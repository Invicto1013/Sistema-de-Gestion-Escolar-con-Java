package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorEdadEstudianteTest {

    private static final LocalDate FECHA_CORTE = LocalDate.of(2026, 9, 1);

    @Test
    @DisplayName("Cálculo exacto de edad en años")
    void calcularEdad_exactitud() {
        LocalDate nac = LocalDate.of(2015, 5, 20);
        assertEquals(11, ValidadorEdadEstudiante.calcularEdad(nac, FECHA_CORTE));
    }

    @Test
    @DisplayName("Edad idónea para nivel Primaria (6 a 11 años)")
    void evaluarNivel_primaria_adecuada() {
        LocalDate nac = LocalDate.of(2016, 2, 10); // 10 años
        ValidadorEdadEstudiante.EvaluacionEdad ev = ValidadorEdadEstudiante.evaluarNivel(
                nac, ValidadorEdadEstudiante.NivelEducativo.PRIMARIA, FECHA_CORTE
        );

        assertTrue(ev.edadAdecuada());
        assertEquals(10, ev.edadAnios());
    }

    @Test
    @DisplayName("Detección de edad inferior a la requerida")
    void evaluarNivel_edadInferior() {
        LocalDate nac = LocalDate.of(2023, 1, 1); // 3 años intentando entrar a primaria (min 6)
        ValidadorEdadEstudiante.EvaluacionEdad ev = ValidadorEdadEstudiante.evaluarNivel(
                nac, ValidadorEdadEstudiante.NivelEducativo.PRIMARIA, FECHA_CORTE
        );

        assertFalse(ev.edadAdecuada());
        assertTrue(ev.observacion().contains("Menor a la edad requerida"));
    }

    @Test
    @DisplayName("Detección de sobreedad escolar")
    void evaluarNivel_sobreedad() {
        LocalDate nac = LocalDate.of(2010, 1, 1); // 16 años en primaria (max 11)
        ValidadorEdadEstudiante.EvaluacionEdad ev = ValidadorEdadEstudiante.evaluarNivel(
                nac, ValidadorEdadEstudiante.NivelEducativo.PRIMARIA, FECHA_CORTE
        );

        assertFalse(ev.edadAdecuada());
        assertTrue(ev.observacion().contains("Sobreedad"));
    }
}
