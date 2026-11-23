package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorBibliotecaEscolarTest {

    @Test
    @DisplayName("Valida ISBN-10 clásico con dígito numérico o 'X'")
    void esIsbn10Valido_evaluacion() {
        // ISBN-10 válido de ejemplo (0-306-40615-2)
        assertTrue(ValidadorBibliotecaEscolar.esIsbn10Valido("0-306-40615-2"));
        assertTrue(ValidadorBibliotecaEscolar.esIsbn10Valido("0306406152"));

        assertFalse(ValidadorBibliotecaEscolar.esIsbn10Valido("0-306-40615-3")); // Checksum malo
        assertFalse(ValidadorBibliotecaEscolar.esIsbn10Valido(null));
    }

    @Test
    @DisplayName("Valida ISBN-13 moderno (EAN-13)")
    void esIsbn13Valido_evaluacion() {
        // ISBN-13 válido oficial (978-0-306-40615-7)
        assertTrue(ValidadorBibliotecaEscolar.esIsbn13Valido("978-0-306-40615-7"));
        assertTrue(ValidadorBibliotecaEscolar.esIsbn13Valido("9780306406157"));

        assertFalse(ValidadorBibliotecaEscolar.esIsbn13Valido("978-0-306-40615-8")); // Checksum malo
        assertFalse(ValidadorBibliotecaEscolar.esIsbn13Valido("970-0-306-40615-7")); // Prefijo no es 978 ni 979
    }

    @Test
    @DisplayName("Validador polimórfico detecta tanto ISBN-10 como ISBN-13")
    void esIsbnValido_casosGenerales() {
        assertTrue(ValidadorBibliotecaEscolar.esIsbnValido("0-306-40615-2"));
        assertTrue(ValidadorBibliotecaEscolar.esIsbnValido("978-0-306-40615-7"));
        assertFalse(ValidadorBibliotecaEscolar.esIsbnValido("12345"));
    }
}
