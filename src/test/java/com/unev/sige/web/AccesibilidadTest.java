package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccesibilidadTest {
    private static final String NAV = "<nav><a href='/'>Inicio</a><a href='/usuarios'>Usuarios</a></nav>";

    @Test
    void marcaSoloElEnlaceDeLaPaginaActual() {
        String r = Accesibilidad.marcarPaginaActual(NAV, "/usuarios");
        assertTrue(r.contains("<a href='/usuarios' aria-current='page'>Usuarios</a>"));
        assertFalse(r.contains("<a href='/' aria-current"));
    }

    @Test
    void sinCoincidenciaONuloDejaElMenuIgual() {
        assertEquals(NAV, Accesibilidad.marcarPaginaActual(NAV, "/otra"));
        assertEquals(NAV, Accesibilidad.marcarPaginaActual(NAV, null));
        assertEquals(NAV, Accesibilidad.marcarPaginaActual(NAV, ""));
    }

    @Test
    void lasCabecerasDeTablaLlevanScopeCol() {
        String r = Accesibilidad.mejorar("<table><thead><tr><th>ID</th><th>Nombre</th></tr></thead></table>");
        assertEquals(2, r.split("<th scope='col'>", -1).length - 1);
        assertFalse(r.contains("<th>"));
    }

    @Test
    void elContenedorDeTablaSePuedeEnfocarConTeclado() {
        String r = Accesibilidad.mejorar("<div class='table-wrap'><table></table></div>");
        assertTrue(r.contains("tabindex='0'"));
        assertTrue(r.contains("role='region'"));
        assertTrue(r.contains("aria-label="));
    }

    @Test
    void textoSinTablasNoCambia() {
        assertEquals("<p>Hola</p>", Accesibilidad.mejorar("<p>Hola</p>"));
        assertEquals(null, Accesibilidad.mejorar(null));
    }
}
