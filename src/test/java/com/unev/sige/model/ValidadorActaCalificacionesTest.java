package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorActaCalificacionesTest {

    @Test
    @DisplayName("Valida registro institucional de acta de calificaciones con tomo y página")
    void esRegistroValido_datosCorrectos() {
        assertTrue(ValidadorActaCalificaciones.esRegistroValido("ACT-2026A", "10", "45"));
        assertTrue(ValidadorActaCalificaciones.esRegistroValido("ACT-FINAL01", "1", "12"));
        assertTrue(ValidadorActaCalificaciones.esRegistroValido("  ACT-SEC26  ", "  5  ", "  100  "));
    }

    @Test
    @DisplayName("Rechaza identificadores malformados o nulos")
    void esRegistroValido_datosIncorrectos() {
        assertFalse(ValidadorActaCalificaciones.esRegistroValido(null, "10", "45"));
        assertFalse(ValidadorActaCalificaciones.esRegistroValido("ACT-01", null, "45"));
        assertFalse(ValidadorActaCalificaciones.esRegistroValido("NO-ACTA", "10", "45")); // Falta prefijo ACT-
        assertFalse(ValidadorActaCalificaciones.esRegistroValido("ACT-2026", "A", "45")); // Tomo no numérico
    }

    @Test
    @DisplayName("Genera referencia institucional autónoma con código de verificación")
    void crear_referenciaInstitucional() {
        ValidadorActaCalificaciones.RegistroActa acta = ValidadorActaCalificaciones.crear(
                "ACT-2026X", "03", "50", "2025-2026", "SIGE-SEC-99"
        );

        assertEquals("ACT-2026X", acta.codigoActa());
        assertEquals("03", acta.tomo());
        assertEquals("50", acta.pagina());
        assertEquals("Acta ACT-2026X [Tomo 03, Pág. 50] - Ciclo 2025-2026 (Verificación: SIGE-SEC-99)",
                acta.referenciaInstitucional());
    }
}
