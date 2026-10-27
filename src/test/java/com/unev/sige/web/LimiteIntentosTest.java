package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LimiteIntentosTest {
    private final AtomicLong reloj = new AtomicLong(1_000);

    private LimiteIntentos limite(int max, long ventana) {
        return new LimiteIntentos(max, ventana, reloj::get);
    }

    @Test
    void bloqueaAlLlegarAlMaximoDeFallos() {
        LimiteIntentos l = limite(3, 600);
        l.registrarFallo("1.1.1.1");
        l.registrarFallo("1.1.1.1");
        assertFalse(l.bloqueado("1.1.1.1"));
        l.registrarFallo("1.1.1.1");
        assertTrue(l.bloqueado("1.1.1.1"));
    }

    @Test
    void cadaIpTieneSuPropioContador() {
        LimiteIntentos l = limite(2, 600);
        l.registrarFallo("1.1.1.1");
        l.registrarFallo("1.1.1.1");
        assertTrue(l.bloqueado("1.1.1.1"));
        assertFalse(l.bloqueado("2.2.2.2"));
    }

    @Test
    void sePermiteDeNuevoCuandoPasaLaVentana() {
        LimiteIntentos l = limite(2, 600);
        l.registrarFallo("ip");
        l.registrarFallo("ip");
        assertTrue(l.bloqueado("ip"));
        reloj.addAndGet(599);
        assertTrue(l.bloqueado("ip"));
        reloj.addAndGet(1);
        assertFalse(l.bloqueado("ip"));
    }

    @Test
    void informaLosSegundosRestantes() {
        LimiteIntentos l = limite(1, 600);
        assertEquals(0L, l.segundosRestantes("ip"));
        l.registrarFallo("ip");
        assertEquals(600L, l.segundosRestantes("ip"));
        reloj.addAndGet(100);
        assertEquals(500L, l.segundosRestantes("ip"));
    }

    @Test
    void purgarLiberaLasClavesVencidas() {
        LimiteIntentos l = limite(5, 60);
        l.registrarFallo("a");
        l.registrarFallo("b");
        assertEquals(2, l.clavesActivas());
        reloj.addAndGet(61);
        l.purgar();
        assertEquals(0, l.clavesActivas());
    }

    @Test
    void rechazaConfiguracionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new LimiteIntentos(0, 60));
        assertThrows(IllegalArgumentException.class, () -> new LimiteIntentos(5, 0));
    }

    @Test
    void sinProxyUsaLaDireccionRemotaYIgnoraLasCabeceras() {
        assertEquals("10.0.0.5", LimiteIntentos.ipCliente("10.0.0.5", "9.9.9.9", "8.8.8.8", false));
    }

    @Test
    void conProxyPrefiereCloudflareLuegoElPrimerForwardedFor() {
        assertEquals("8.8.8.8", LimiteIntentos.ipCliente("10.0.0.5", "9.9.9.9, 10.0.0.1", "8.8.8.8", true));
        assertEquals("9.9.9.9", LimiteIntentos.ipCliente("10.0.0.5", "9.9.9.9, 10.0.0.1", null, true));
        assertEquals("2001:db8::1", LimiteIntentos.ipCliente("10.0.0.5", " 2001:db8::1 ", null, true));
    }

    @Test
    void ignoraCabecerasConFormatoInvalido() {
        assertEquals("10.0.0.5", LimiteIntentos.ipCliente("10.0.0.5", "<script>", "'; DROP", true));
        assertEquals("0.0.0.0", LimiteIntentos.ipCliente(null, null, null, true));
    }
}
