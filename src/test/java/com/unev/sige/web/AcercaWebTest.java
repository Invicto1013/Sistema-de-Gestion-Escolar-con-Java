package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;

import com.unev.sige.model.AcercaDe;
import org.junit.jupiter.api.Test;

class AcercaWebTest {
    @Test void elBotonApuntaALaPaginaDeInformacion() {
        String b = AcercaWeb.boton();
        assertTrue(b.contains("href='/acerca'"));
        assertTrue(b.contains("aria-label='Acerca de SIGE'"));
        assertTrue(b.contains("<svg"));
    }
    @Test void laPaginaMuestraVersionSeccionesYRepositorio() {
        String c = AcercaWeb.cuerpo();
        assertTrue(c.contains("Versión " + AcercaDe.VERSION));
        for (AcercaDe.Seccion s : AcercaDe.secciones()) assertTrue(c.contains(s.titulo()), s.titulo());
        assertTrue(c.contains("Elian Castro"));
        assertTrue(c.contains("Universidad Nacional Evangélica (UNEV)"));
        assertTrue(c.contains("rel='noopener noreferrer'"));
        assertFalse(c.contains("style="));
        assertFalse(c.contains("<script"));
    }
    @Test void escapaElTextoPeligroso() {
        assertEquals("&lt;b&gt;&amp;&quot;&#39;", AcercaWeb.esc("<b>&\"'"));
    }
}
