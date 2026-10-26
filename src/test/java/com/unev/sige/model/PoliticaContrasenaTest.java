package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PoliticaContrasenaTest {

    @Test
    @DisplayName("Acepta una contraseña con letras, números y 8 caracteres")
    void validar_contrasenaValida_devuelveNull() {
        // Arrange
        String contrasena = "Clave1234";
        // Act
        String resultado = PoliticaContrasena.validar(contrasena);
        // Assert
        assertNull(resultado);
    }

    @Test
    @DisplayName("Acepta exactamente 8 caracteres (límite inferior)")
    void validar_ochoCaracteres_devuelveNull() {
        assertNull(PoliticaContrasena.validar("abcdef12"));
    }

    @Test
    @DisplayName("Rechaza contraseñas de menos de 8 caracteres")
    void validar_contrasenaCorta_devuelveMensajeDeLongitud() {
        String resultado = PoliticaContrasena.validar("abc123");
        assertEquals("La contraseña debe tener al menos 8 caracteres.", resultado);
    }

    @Test
    @DisplayName("Rechaza contraseñas sin números")
    void validar_sinNumero_devuelveMensajeDeComposicion() {
        assertNotNull(PoliticaContrasena.validar("abcdefgh"));
    }

    @Test
    @DisplayName("Rechaza contraseñas sin letras")
    void validar_sinLetra_devuelveMensajeDeComposicion() {
        assertNotNull(PoliticaContrasena.validar("12345678"));
    }

    @Test
    @DisplayName("Rechaza contraseñas nulas")
    void validar_nula_devuelveMensaje() {
        assertNotNull(PoliticaContrasena.validar(null));
    }

    @Test
    @DisplayName("Rechaza contraseñas que superan los 72 bytes de BCrypt")
    void validar_masDe72Bytes_devuelveMensajeDeLongitudMaxima() {
        String larga = "a1".repeat(37);
        assertNotNull(PoliticaContrasena.validar(larga));
    }
}
