package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorTituloDocenteTest {

    @Test
    @DisplayName("Valida registro oficial de título universitario TIT-YYYY-XXXXX")
    void esNumeroRegistroValido_casos() {
        assertTrue(ValidadorTituloDocente.esNumeroRegistroValido("TIT-2020-00452"));
        assertFalse(ValidadorTituloDocente.esNumeroRegistroValido("TIT-20"));
        assertFalse(ValidadorTituloDocente.esNumeroRegistroValido(null));
    }

    @Test
    @DisplayName("Valida acreditación docente completa")
    void esAcreditacionValida_evaluacion() {
        ValidadorTituloDocente.AcreditacionDocente ac = new ValidadorTituloDocente.AcreditacionDocente(
                "TIT-2018-01234", "Licenciatura en Educación Mención Matemáticas", "UNEV", 2018
        );
        assertTrue(ValidadorTituloDocente.esAcreditacionValida(ac));
    }
}
