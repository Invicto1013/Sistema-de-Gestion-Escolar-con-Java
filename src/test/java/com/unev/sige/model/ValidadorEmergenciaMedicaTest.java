package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorEmergenciaMedicaTest {

    @Test
    @DisplayName("Valida líneas cortas de emergencia nacional (911)")
    void esLineaEmergenciaValida_lineasCortas() {
        assertTrue(ValidadorEmergenciaMedica.esLineaEmergenciaValida("911"));
        assertTrue(ValidadorEmergenciaMedica.esLineaEmergenciaValida("462")); // Línea de atención ciudadana RD
    }

    @Test
    @DisplayName("Valida números telefónicos estándar de centros médicos en RD")
    void esLineaEmergenciaValida_telefonosCompletos() {
        assertTrue(ValidadorEmergenciaMedica.esLineaEmergenciaValida("809-555-9911"));
        assertTrue(ValidadorEmergenciaMedica.esLineaEmergenciaValida("(829) 456-7890"));

        assertFalse(ValidadorEmergenciaMedica.esLineaEmergenciaValida(null));
        assertFalse(ValidadorEmergenciaMedica.esLineaEmergenciaValida("1234"));
    }

    @Test
    @DisplayName("Valida tipos y factores sanguíneos válidos")
    void esTipoSangreValido_evaluacion() {
        assertTrue(ValidadorEmergenciaMedica.esTipoSangreValido("O+"));
        assertTrue(ValidadorEmergenciaMedica.esTipoSangreValido("A-"));
        assertTrue(ValidadorEmergenciaMedica.esTipoSangreValido("AB+"));

        assertFalse(ValidadorEmergenciaMedica.esTipoSangreValido("C+"));
        assertFalse(ValidadorEmergenciaMedica.esTipoSangreValido("O")); // Falta factor Rh
        assertFalse(ValidadorEmergenciaMedica.esTipoSangreValido(null));
    }
}
