package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorClubExtracurricularTest {

    @Test
    @DisplayName("Permite inscripción si no excede el límite de 3 clubes extracurriculares")
    void puedeInscribirClub_limite() {
        assertTrue(ValidadorClubExtracurricular.puedeInscribirClub(0));
        assertTrue(ValidadorClubExtracurricular.puedeInscribirClub(2));

        assertFalse(ValidadorClubExtracurricular.puedeInscribirClub(3)); // Ya tiene el máximo
        assertFalse(ValidadorClubExtracurricular.puedeInscribirClub(4));
    }

    @Test
    @DisplayName("Valida rango de horas semanales de dedicación complementaria (1 a 10 h)")
    void sonHorasValidas_rango() {
        assertTrue(ValidadorClubExtracurricular.sonHorasValidas(2));
        assertTrue(ValidadorClubExtracurricular.sonHorasValidas(10));

        assertFalse(ValidadorClubExtracurricular.sonHorasValidas(0));
        assertFalse(ValidadorClubExtracurricular.sonHorasValidas(15));
    }
}
