package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorCedulaDominicanaTest {

    @Test
    @DisplayName("Limpia caracteres especiales y guiones")
    void limpiar_normaliza() {
        assertEquals("40212345678", ValidadorCedulaDominicana.limpiar("402-1234567-8"));
        assertEquals("00100000000", ValidadorCedulaDominicana.limpiar(" 001-0000000-0 "));
    }

    @Test
    @DisplayName("Formatea cédula de 11 dígitos con guiones estándar")
    void formatear_aplicaGuiones() {
        assertEquals("402-1234567-8", ValidadorCedulaDominicana.formatear("40212345678"));
        assertEquals("001-1234567-9", ValidadorCedulaDominicana.formatear("001-1234567-9"));
    }

    @Test
    @DisplayName("Rechaza secuencias inválidas, vacías o nulas")
    void esValida_secuenciasInvalidas() {
        assertFalse(ValidadorCedulaDominicana.esValida(null));
        assertFalse(ValidadorCedulaDominicana.esValida(""));
        assertFalse(ValidadorCedulaDominicana.esValida("12345"));
        assertFalse(ValidadorCedulaDominicana.esValida("00000000000"));
        assertFalse(ValidadorCedulaDominicana.esValida("11111111111"));
        assertFalse(ValidadorCedulaDominicana.esValida("402-1234567-9")); // Dígito verificador malo
    }

    @Test
    @DisplayName("Valida cédula con dígito verificador correcto según Módulo 10")
    void esValida_cedulaCorrecta() {
        // 0010000002 con pesos 1,2,1,2...: productos 0,0,1,0,0,0,0,0,0,4 -> suma 5 -> verificador (10-5)%10 = 5
        assertTrue(ValidadorCedulaDominicana.esValida("00100000025"));
        assertTrue(ValidadorCedulaDominicana.esValida("001-0000002-5"));
        assertFalse(ValidadorCedulaDominicana.esValida("001-0000002-6"));
    }
}
