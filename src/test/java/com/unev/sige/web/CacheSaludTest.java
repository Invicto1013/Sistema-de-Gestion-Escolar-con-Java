package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CacheSaludTest {
    private final AtomicLong reloj = new AtomicLong(1_000);
    private final AtomicInteger consultas = new AtomicInteger();

    @Test
    void noRepiteLaConsultaDentroDelTiempoDeCache() {
        CacheSalud cache = new CacheSalud(() -> { consultas.incrementAndGet(); return true; }, 30, reloj::get);
        assertTrue(cache.estaSano());
        reloj.addAndGet(29);
        assertTrue(cache.estaSano());
        assertEquals(1, consultas.get());
    }

    @Test
    void vuelveAConsultarCuandoVenceElCache() {
        CacheSalud cache = new CacheSalud(() -> { consultas.incrementAndGet(); return true; }, 30, reloj::get);
        cache.estaSano();
        reloj.addAndGet(30);
        cache.estaSano();
        assertEquals(2, consultas.get());
    }

    @Test
    void reflejaLaRecuperacionDeLaBaseDeDatosTrasElCache() {
        AtomicInteger estado = new AtomicInteger(0);
        CacheSalud cache = new CacheSalud(() -> estado.get() == 1, 30, reloj::get);
        assertFalse(cache.estaSano());
        estado.set(1);
        assertFalse(cache.estaSano());
        reloj.addAndGet(31);
        assertTrue(cache.estaSano());
    }

    @Test
    void unaExcepcionSeTratadaComoNoDisponible() {
        CacheSalud cache = new CacheSalud(() -> { throw new IllegalStateException("sin conexión"); }, 30, reloj::get);
        assertFalse(cache.estaSano());
    }
}
