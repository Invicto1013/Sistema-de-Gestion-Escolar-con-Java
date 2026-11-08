package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MovimientoWebTest {
    @Test void porDefectoSigueAlSistema() {
        assertEquals("sistema", MovimientoWeb.modo(null));
        assertEquals("sistema", MovimientoWeb.modo("sige_mov=hack"));
        assertEquals("", MovimientoWeb.atributo("sistema"));
    }
    @Test void siempreActivaLasAnimaciones() {
        assertEquals("siempre", MovimientoWeb.modo("a=1; sige_mov=siempre"));
        assertEquals(" data-mov='siempre'", MovimientoWeb.atributo("siempre"));
        assertTrue(MovimientoWeb.seccionMenu("siempre", "/x").contains("volver=%2Fx"));
    }
}
