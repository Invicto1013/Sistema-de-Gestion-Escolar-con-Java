package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EstacionWebTest {
    @Test void laEstacionCambiaSieteDiasAntesDelInicio() {
        assertEquals("otono", EstacionWeb.deFecha(LocalDate.of(2026, 11, 6)));
        assertEquals("otono", EstacionWeb.deFecha(LocalDate.of(2026, 9, 15)));
        assertEquals("verano", EstacionWeb.deFecha(LocalDate.of(2026, 9, 14)));
        assertEquals("invierno", EstacionWeb.deFecha(LocalDate.of(2026, 12, 14)));
        assertEquals("otono", EstacionWeb.deFecha(LocalDate.of(2026, 12, 13)));
        assertEquals("invierno", EstacionWeb.deFecha(LocalDate.of(2027, 1, 20)));
        assertEquals("invierno", EstacionWeb.deFecha(LocalDate.of(2027, 3, 12)));
        assertEquals("primavera", EstacionWeb.deFecha(LocalDate.of(2027, 3, 13)));
        assertEquals("verano", EstacionWeb.deFecha(LocalDate.of(2027, 6, 14)));
        assertEquals("primavera", EstacionWeb.deFecha(LocalDate.of(2027, 6, 13)));
    }
    @Test void laPreferenciaManda() {
        LocalDate hoy = LocalDate.of(2026, 11, 6);
        assertEquals("otono", EstacionWeb.efectiva("auto", hoy));
        assertEquals("invierno", EstacionWeb.efectiva("invierno", hoy));
        assertEquals("", EstacionWeb.efectiva("ninguna", hoy));
        assertEquals("otono", EstacionWeb.efectiva("<x>", hoy));
    }
    @Test void leeLaCookieYGeneraHtmlSeguro() {
        assertEquals("verano", EstacionWeb.modo("a=1; sige_estacion=verano"));
        assertEquals("auto", EstacionWeb.modo("sige_estacion=hack"));
        assertEquals("", EstacionWeb.atributo(""));
        assertEquals(" data-estacion='otono'", EstacionWeb.atributo("otono"));
        assertEquals("", EstacionWeb.capa(""));
        assertTrue(EstacionWeb.capa("otono").contains("estacion-fx"));
        assertTrue(EstacionWeb.seccionMenu("invierno", "/x").contains("volver=%2Fx"));
    }
}
