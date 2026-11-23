package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentificadorEscolarTest {

    @Test
    @DisplayName("Valida y genera Código Único de Estudiante (CUE) propio de SIGE")
    void cueSige_validacionYGeneracion() {
        String cue = IdentificadorEscolar.generarCue(2026, 42);
        assertEquals("SIGE-2026-00042", cue);
        assertTrue(IdentificadorEscolar.esCueSigeValido(cue));
        assertTrue(IdentificadorEscolar.esCueSigeValido("SIGE-2026-01234"));

        assertFalse(IdentificadorEscolar.esCueSigeValido(null));
        assertFalse(IdentificadorEscolar.esCueSigeValido("2026-00042")); // Falta prefijo SIGE
    }

    @Test
    @DisplayName("Valida formatos de matrícula escolar institucional")
    void esMatriculaValida_formatos() {
        assertTrue(IdentificadorEscolar.esMatriculaValida("2026-0001"));
        assertTrue(IdentificadorEscolar.esMatriculaValida("ST-2026-0042"));
        assertTrue(IdentificadorEscolar.esMatriculaValida("UNEV-2026-0100"));

        assertFalse(IdentificadorEscolar.esMatriculaValida(null));
        assertFalse(IdentificadorEscolar.esMatriculaValida(""));
        assertFalse(IdentificadorEscolar.esMatriculaValida("12345"));
    }

    @Test
    @DisplayName("Valida códigos de asignatura propios")
    void esCodigoMateriaValido_evaluacion() {
        assertTrue(IdentificadorEscolar.esCodigoMateriaValido("SIGE-MAT-101"));
        assertTrue(IdentificadorEscolar.esCodigoMateriaValido("MAT101"));
        assertTrue(IdentificadorEscolar.esCodigoMateriaValido("FIS-201"));

        assertFalse(IdentificadorEscolar.esCodigoMateriaValido(null));
        assertFalse(IdentificadorEscolar.esCodigoMateriaValido("M1"));
    }
}
