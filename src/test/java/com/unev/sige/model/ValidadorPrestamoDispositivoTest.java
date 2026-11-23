package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorPrestamoDispositivoTest {

    @Test
    @DisplayName("Dispositivo devuelto a tiempo o antes de la fecha esperada")
    void estaEnMora_aTiempo() {
        LocalDate esperada = LocalDate.of(2026, 11, 25);
        LocalDate actual = LocalDate.of(2026, 11, 24);

        assertFalse(ValidadorPrestamoDispositivo.estaEnMora(esperada, actual));
        assertEquals(0, ValidadorPrestamoDispositivo.diasRetraso(esperada, actual));
    }

    @Test
    @DisplayName("Calcula días de retraso en dispositivos en mora")
    void estaEnMora_conRetraso() {
        LocalDate esperada = LocalDate.of(2026, 11, 20);
        LocalDate actual = LocalDate.of(2026, 11, 25);

        assertTrue(ValidadorPrestamoDispositivo.estaEnMora(esperada, actual));
        assertEquals(5, ValidadorPrestamoDispositivo.diasRetraso(esperada, actual));
    }
}
