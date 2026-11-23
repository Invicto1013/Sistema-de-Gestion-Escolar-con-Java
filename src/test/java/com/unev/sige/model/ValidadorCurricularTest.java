package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCurricularTest {

    @Test
    @DisplayName("Materia sin prerrequisitos siempre es válida")
    void validarPrerrequisitos_sinPrerrequisitos_esValida() {
        ValidadorCurricular.RequisitoMateria mat = new ValidadorCurricular.RequisitoMateria(
                "MAT101", "Matemática Básica", 4, Set.of()
        );
        ValidadorCurricular.ResultadoValidacion res = ValidadorCurricular.validarPrerrequisitos(mat, Set.of());
        assertTrue(res.esValida());
        assertNull(res.mensajeError());
    }

    @Test
    @DisplayName("Rechaza inscripción si falta algún prerrequisito")
    void validarPrerrequisitos_faltaPrerrequisito_rechaza() {
        ValidadorCurricular.RequisitoMateria cal = new ValidadorCurricular.RequisitoMateria(
                "MAT201", "Cálculo I", 4, Set.of("MAT101")
        );

        // Estudiante aprobó FIS101 pero no MAT101
        ValidadorCurricular.ResultadoValidacion res = ValidadorCurricular.validarPrerrequisitos(cal, Set.of("FIS101"));
        assertFalse(res.esValida());
        assertTrue(res.mensajeError().contains("MAT101"));
    }

    @Test
    @DisplayName("Aprueba inscripción cuando todos los prerrequisitos están aprobados")
    void validarPrerrequisitos_conPrerrequisitosCumplidos_aprueba() {
        ValidadorCurricular.RequisitoMateria cal2 = new ValidadorCurricular.RequisitoMateria(
                "MAT202", "Cálculo II", 4, Set.of("MAT101", "MAT201")
        );

        ValidadorCurricular.ResultadoValidacion res = ValidadorCurricular.validarPrerrequisitos(
                cal2, Set.of("MAT101", "MAT201", "ESP101")
        );
        assertTrue(res.esValida());
        assertNull(res.mensajeError());
    }

    @Test
    @DisplayName("Controla la carga académica máxima por período")
    void validarCargaAcademica_limiteMaterias() {
        // 5 actuales + 3 nuevas = 8 -> OK
        assertTrue(ValidadorCurricular.validarCargaAcademica(5, 3).esValida());

        // 5 actuales + 4 nuevas = 9 -> Error (excede 8)
        ValidadorCurricular.ResultadoValidacion res = ValidadorCurricular.validarCargaAcademica(5, 4);
        assertFalse(res.esValida());
        assertTrue(res.mensajeError().contains("límite permitido"));
    }
}
