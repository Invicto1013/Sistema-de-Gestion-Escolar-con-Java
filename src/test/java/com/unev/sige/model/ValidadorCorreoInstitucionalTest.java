package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCorreoInstitucionalTest {

    @Test
    @DisplayName("Valida sintaxis general de correo")
    void esFormatoValido_sintaxis() {
        assertTrue(ValidadorCorreoInstitucional.esFormatoValido("juan.perez@sige.edu.do"));
        assertTrue(ValidadorCorreoInstitucional.esFormatoValido("maria@gmail.com"));

        assertFalse(ValidadorCorreoInstitucional.esFormatoValido(null));
        assertFalse(ValidadorCorreoInstitucional.esFormatoValido(""));
        assertFalse(ValidadorCorreoInstitucional.esFormatoValido("sin-arroba.com"));
        assertFalse(ValidadorCorreoInstitucional.esFormatoValido("@dominio.com"));
    }

    @Test
    @DisplayName("Identifica correos pertenecientes al dominio institucional")
    void esCorreoInstitucional_dominio() {
        assertTrue(ValidadorCorreoInstitucional.esCorreoInstitucional("juan@sige.edu.do", null));
        assertTrue(ValidadorCorreoInstitucional.esCorreoInstitucional("profesor.garcia@sige.edu.do", "sige.edu.do"));
        assertTrue(ValidadorCorreoInstitucional.esCorreoInstitucional("director@colegio.edu.do", "colegio.edu.do"));

        assertFalse(ValidadorCorreoInstitucional.esCorreoInstitucional("padre@gmail.com", "sige.edu.do"));
        assertFalse(ValidadorCorreoInstitucional.esCorreoInstitucional("tutor@hotmail.com", null));
    }

    @Test
    @DisplayName("Genera sugerencia estandarizada de cuenta institucional")
    void sugerirCorreo_generacion() {
        String sugerido = ValidadorCorreoInstitucional.sugerirCorreo("Carlos", "Gómez", "2026", "sige.edu.do");
        assertEquals("carlos.gomez26@sige.edu.do", sugerido);
    }
}
