package com.unev.sige.web;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * Guarda unos segundos el resultado de comprobar la base de datos, para que el endpoint /health
 * no consulte MySQL en cada visita aunque alguien lo llame sin parar.
 */
final class CacheSalud {
    private final BooleanSupplier comprobacion;
    private final long ttlSegundos;
    private final LongSupplier reloj;
    private boolean ultimo;
    private long vigenteHasta = Long.MIN_VALUE;

    CacheSalud(BooleanSupplier comprobacion, long ttlSegundos) {
        this(comprobacion, ttlSegundos, () -> System.currentTimeMillis() / 1000);
    }

    CacheSalud(BooleanSupplier comprobacion, long ttlSegundos, LongSupplier reloj) {
        this.comprobacion = comprobacion;
        this.ttlSegundos = ttlSegundos;
        this.reloj = reloj;
    }

    synchronized boolean estaSano() {
        long ahora = reloj.getAsLong();
        if (ahora >= vigenteHasta) {
            try {
                ultimo = comprobacion.getAsBoolean();
            } catch (RuntimeException e) {
                ultimo = false;
            }
            vigenteHasta = ahora + ttlSegundos;
        }
        return ultimo;
    }
}
