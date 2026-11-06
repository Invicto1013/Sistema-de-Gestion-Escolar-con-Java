package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VisionWebTest {
    @Test void leeModoDeLaCookie() {
        assertEquals("daltonismo", VisionWeb.modo("a=1; sige_vision=daltonismo; b=2"));
        assertEquals("normal", VisionWeb.modo("sige_vision=hackeado"));
        assertEquals("normal", VisionWeb.modo(null));
    }
    @Test void soloVuelveARutasLocales() {
        assertEquals("/materias", VisionWeb.volverSeguro("/materias"));
        assertEquals("/", VisionWeb.volverSeguro("//evil.com"));
        assertEquals("/", VisionWeb.volverSeguro("https://evil.com"));
        assertEquals("/", VisionWeb.volverSeguro("/a\r\nSet-Cookie: x=1"));
        assertEquals("/", VisionWeb.volverSeguro(null));
    }
    @Test void atributoYSelector() {
        assertEquals("", VisionWeb.atributo("normal"));
        assertEquals(" data-vision='contraste'", VisionWeb.atributo("contraste"));
        assertEquals("", VisionWeb.atributo("<script>"));
        String h = VisionWeb.selector("daltonismo", "/x");
        assertTrue(h.contains("modo=daltonismo") && h.contains("aria-current='true'") && h.contains("volver=%2Fx"));
    }
    @Test void menuAgrupaLosModosBajoUnOjo() {
        String h = VisionWeb.menu("daltonismo", "/x");
        assertTrue(h.startsWith("<details class='vision-menu'>") && h.contains("<svg") && h.contains("modo=contraste"));
        assertTrue(h.contains("aria-current='true'") && h.contains("volver=%2Fx"));
    }
}
