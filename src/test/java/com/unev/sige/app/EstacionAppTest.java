package com.unev.sige.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EstacionAppTest {
    @Test
    void cambiaSieteDiasAntesDeCadaEstacion() {
        assertEquals("otono", EstacionApp.deFecha(LocalDate.of(2026, 11, 6)));
        assertEquals("invierno", EstacionApp.deFecha(LocalDate.of(2026, 12, 14)));
        assertEquals("primavera", EstacionApp.deFecha(LocalDate.of(2027, 3, 13)));
        assertEquals("verano", EstacionApp.deFecha(LocalDate.of(2027, 6, 14)));
        assertEquals("otono", EstacionApp.deFecha(LocalDate.of(2027, 9, 15)));
    }
}
