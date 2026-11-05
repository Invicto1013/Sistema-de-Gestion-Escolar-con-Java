package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class IconosWebTest {
    @Test void anadeIconoOcultoALosEnlacesConocidos() {
        String r = IconosWeb.agregar("<nav><a href='/periodos' aria-current='page'>Períodos</a></nav>");
        assertTrue(r.contains("<span class='ico' aria-hidden='true'>📅</span> Períodos</a>"));
        assertTrue(r.contains("aria-current='page'"));
    }
    @Test void noTocaEnlacesDesconocidosNiConImagen() {
        String x = "<a href='/otra'>Otra</a><a class='brand' href='/'><img src='/logo.svg' alt=''>SIGE</a>";
        assertEquals(x, IconosWeb.agregar(x));
    }
}
