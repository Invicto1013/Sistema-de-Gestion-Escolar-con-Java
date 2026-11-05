package com.unev.sige.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModoVisionTest {
    @Test
    void recorreLosModosEnCiclo() {
        assertEquals(ModoVision.DALTONISMO, ModoVision.NORMAL.siguiente());
        assertEquals(ModoVision.ALTO_CONTRASTE, ModoVision.DALTONISMO.siguiente());
        assertEquals(ModoVision.NORMAL, ModoVision.ALTO_CONTRASTE.siguiente());
    }

    @Test
    void lasEtiquetasSonLegibles() {
        assertEquals("Normal", ModoVision.NORMAL.etiqueta());
        assertEquals("Daltonismo", ModoVision.DALTONISMO.etiqueta());
        assertEquals("Alto contraste", ModoVision.ALTO_CONTRASTE.etiqueta());
    }
}
