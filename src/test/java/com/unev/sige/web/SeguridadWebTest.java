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

    // --- Contrato de cabeceras y cookie (mejora: validacion automatizada de contratos) ---

    @Test
    void laPaginaLlevaTodasLasCabecerasDeSeguridad() {
        Headers h = new Headers();
        SeguridadWeb.cabecerasDePagina(h, false);
        assertEquals("no-store", h.getFirst("Cache-Control"));
        assertEquals("nosniff", h.getFirst("X-Content-Type-Options"));
        assertEquals("DENY", h.getFirst("X-Frame-Options"));
        assertEquals("same-origin", h.getFirst("Referrer-Policy"));
        assertEquals("same-origin", h.getFirst("Cross-Origin-Opener-Policy"));
        assertNotNull(h.getFirst("Permissions-Policy"));
        assertFalse(h.containsKey("Strict-Transport-Security"));
    }

    @Test
    void laPoliticaCspNoPermiteScriptsAjenosNiMarcos() {
        String csp = SeguridadWeb.CSP;
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
        assertTrue(csp.contains("form-action 'self'"));
        assertTrue(csp.contains("base-uri 'self'"));
        assertFalse(csp.contains("script-src"), "los scripts caen en default-src 'self'");
        assertFalse(csp.contains("'unsafe-eval'"));
        assertFalse(csp.contains("*"));
    }

    @Test
    void conHttpsLaPaginaAgregaHsts() {
        Headers h = new Headers();
        SeguridadWeb.cabecerasDePagina(h, true);
        assertTrue(h.getFirst("Strict-Transport-Security").contains("includeSubDomains"));
    }

    @Test
    void laCookieDeSesionEsHttpOnlyYSameSite() {
        String c = SeguridadWeb.cookieSesion("SIGE_SESSION", "abc123", 28800, false);
        assertTrue(c.startsWith("SIGE_SESSION=abc123;"));
        assertTrue(c.contains("HttpOnly"));
        assertTrue(c.contains("SameSite=Lax"));
        assertTrue(c.contains("Path=/"));
        assertTrue(c.contains("Max-Age=28800"));
        assertFalse(c.contains("Secure"));
    }

    @Test
    void laCookieSeMarcaSecureEnHttps() {
        assertTrue(SeguridadWeb.cookieSesion("S", "x", 60, true).endsWith("; Secure"));
    }
}
