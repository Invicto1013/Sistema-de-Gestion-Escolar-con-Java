package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorTelefonoDominicanoTest {

    @Test
    @DisplayName("Valida teléfonos de los tres códigos de área de RD (809, 829, 849)")
    void esValido_codigosValidos() {
        assertTrue(ValidadorTelefonoDominicano.esValido("809-555-1234"));
        assertTrue(ValidadorTelefonoDominicano.esValido("(829) 123-4567"));
        assertTrue(ValidadorTelefonoDominicano.esValido("+1-849-987-6543"));
        assertTrue(ValidadorTelefonoDominicano.esValido("18095551234"));
        assertTrue(ValidadorTelefonoDominicano.esValido("8295551234"));
    }

    @Test
    @DisplayName("Rechaza números de códigos foráneos o longitudes incorrectas")
    void esValido_codigosInvalidos() {
        assertFalse(ValidadorTelefonoDominicano.esValido(null));
        assertFalse(ValidadorTelefonoDominicano.esValido(""));
        assertFalse(ValidadorTelefonoDominicano.esValido("809-123")); // Muy corto
        assertFalse(ValidadorTelefonoDominicano.esValido("305-555-1234")); // Miami (no RD)
        assertFalse(ValidadorTelefonoDominicano.esValido("800-555-1234")); // Toll-free (no RD)
    }

    @Test
    @DisplayName("Formatea teléfono estándar visual para reportes y fichas")
    void formatear_aplicaMascara() {
        assertEquals("(809) 555-1234", ValidadorTelefonoDominicano.formatear("8095551234"));
        assertEquals("(829) 999-8888", ValidadorTelefonoDominicano.formatear("18299998888"));
    }
}
