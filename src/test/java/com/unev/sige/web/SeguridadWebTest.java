package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.Headers;
import org.junit.jupiter.api.Test;

class SeguridadWebTest {

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1'};
    private static final byte[] ZIP = {'P', 'K', 3, 4};

    @Test
    void aceptaDocumentosEscolaresValidos() {
        assertNull(SeguridadWeb.validarArchivo("tarea.pdf", PDF));
        assertNull(SeguridadWeb.validarArchivo("Informe Final.DOCX", ZIP));
        assertNull(SeguridadWeb.validarArchivo("notas.txt", new byte[] {'h', 'i'}));
    }

    @Test
    void rechazaEjecutablesYPaginas() {
        for (String malo : new String[] {"virus.exe", "a.bat", "x.js", "p.html", "p.svg", "app.jar", "s.php", "sin_extension", "oculto.", ".htaccess"}) {
            assertNotNull(SeguridadWeb.validarArchivo(malo, PDF), malo);
        }
    }

    @Test
    void rechazaContenidoQueNoCorrespondeALaExtension() {
        assertNotNull(SeguridadWeb.validarArchivo("falso.pdf", new byte[] {'M', 'Z', 0, 0}));
        assertNotNull(SeguridadWeb.validarArchivo("falso.png", PDF));
        assertNotNull(SeguridadWeb.validarArchivo("vacio.pdf", new byte[0]));
    }

    @Test
    void extraeLaExtension() {
        assertEquals("pdf", SeguridadWeb.extension("A.b.PDF"));
        assertEquals("", SeguridadWeb.extension("sinpunto"));
        assertEquals("", SeguridadWeb.extension(null));
    }

    @Test
    void hstsSoloConHttps() {
        Headers sin = new Headers();
        SeguridadWeb.aplicarCabeceras(sin, false);
        assertFalse(sin.containsKey("Strict-Transport-Security"));
        assertTrue(sin.containsKey("Permissions-Policy"));
        Headers con = new Headers();
        SeguridadWeb.aplicarCabeceras(con, true);
        assertTrue(con.getFirst("Strict-Transport-Security").startsWith("max-age="));
    }
}
