package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorActaNacimientoEscolarTest {

    @Test
    @DisplayName("Valida datos registrales numéricos de acta de nacimiento dominicana")
    void esRegistroValido_datosCorrectos() {
        assertTrue(ValidadorActaNacimientoEscolar.esRegistroValido("01", "12", "45", "00123"));
        assertTrue(ValidadorActaNacimientoEscolar.esRegistroValido("2", "5", "10", "44"));

        assertFalse(ValidadorActaNacimientoEscolar.esRegistroValido(null, "12", "45", "123"));
        assertFalse(ValidadorActaNacimientoEscolar.esRegistroValido("01", "A", "45", "123")); // Libro no numérico
        assertFalse(ValidadorActaNacimientoEscolar.esRegistroValido("01", "12", "", "123")); // Folio vacío
    }

    @Test
    @DisplayName("Genera formato oficial de inscripción para expediente estudiantil")
    void registroOficial_formato() {
        ValidadorActaNacimientoEscolar.RegistroNacimiento reg = new ValidadorActaNacimientoEscolar.RegistroNacimiento(
                "02", "14", "105", "00452", "2016"
        );
        assertEquals("Ofic. 02, Libro 14, Folio 105, Acta 00452 (2016)", reg.registroOficial());
    }
}
