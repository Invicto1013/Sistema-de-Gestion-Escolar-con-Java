package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TemaWebTest {
    @Test void leeElTemaDeLaCookie() {
        assertEquals("oscuro", TemaWeb.modo("a=1; sige_tema=oscuro; b=2"));
        assertEquals(TemaWeb.PREDETERMINADO, TemaWeb.modo("sige_tema=hackeado"));
        assertEquals(TemaWeb.PREDETERMINADO, TemaWeb.modo(null));
    }
    @Test void atributoSoloSiCambiaElComportamiento() {
        assertEquals("", TemaWeb.atributo(TemaWeb.PREDETERMINADO));
        assertEquals(" data-tema='oscuro'", TemaWeb.atributo("oscuro"));
        assertEquals("", TemaWeb.atributo("<script>"));
    }
    @Test void selectorMarcaElActivoYVuelveALocal() {
        String h = TemaWeb.selector("oscuro", "/x");
        assertTrue(h.contains("modo=oscuro") && h.contains("aria-current='true'") && h.contains("volver=%2Fx"));
    }
}
