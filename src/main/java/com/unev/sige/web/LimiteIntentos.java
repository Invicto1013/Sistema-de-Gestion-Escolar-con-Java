package com.unev.sige.web;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Límite de intentos fallidos de inicio de sesión por dirección IP (ventana deslizante en memoria).
 * Complementa el bloqueo por cuenta (5 contraseñas incorrectas): frena a quien prueba muchas
 * cuentas distintas desde un mismo lugar. Los contadores se reinician si el servidor se reinicia.
 */
final class LimiteIntentos {
    private static final int MAX_CLAVES = 20_000;

    private final int maxFallos;
    private final long ventanaSegundos;
    private final LongSupplier reloj;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> fallos = new ConcurrentHashMap<>();

    LimiteIntentos(int maxFallos, long ventanaSegundos) {
        this(maxFallos, ventanaSegundos, () -> System.currentTimeMillis() / 1000);
    }

    /** Constructor con reloj inyectable (segundos) para las pruebas. */
    LimiteIntentos(int maxFallos, long ventanaSegundos, LongSupplier reloj) {
        if (maxFallos < 1 || ventanaSegundos < 1) throw new IllegalArgumentException("Límite inválido");
        this.maxFallos = maxFallos;
        this.ventanaSegundos = ventanaSegundos;
        this.reloj = reloj;
    }

    /** true si la clave ya agotó sus intentos dentro de la ventana. */
    boolean bloqueado(String clave) {
        return segundosRestantes(clave) > 0;
    }

    /** Segundos hasta que se libere el primer intento (0 si no está bloqueada). */
    long segundosRestantes(String clave) {
        ArrayDeque<Long> cola = fallos.get(clave);
        if (cola == null) return 0;
        synchronized (cola) {
            descartarVencidos(cola);
            if (cola.size() < maxFallos) return 0;
            return Math.max(1, cola.peekFirst() + ventanaSegundos - reloj.getAsLong());
        }
    }

    void registrarFallo(String clave) {
        if (!fallos.containsKey(clave) && fallos.size() >= MAX_CLAVES) {
            purgar();
            if (fallos.size() >= MAX_CLAVES) return; // protege la memoria: no se siguen creando claves
        }
        ArrayDeque<Long> cola = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            descartarVencidos(cola);
            cola.addLast(reloj.getAsLong());
        }
    }

    /** Elimina las claves sin fallos vigentes. */
    void purgar() {
        fallos.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                descartarVencidos(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }

    int clavesActivas() {
        return fallos.size();
    }

    private void descartarVencidos(ArrayDeque<Long> cola) {
        long limite = reloj.getAsLong() - ventanaSegundos;
        while (!cola.isEmpty() && cola.peekFirst() <= limite) cola.pollFirst();
    }

    /**
     * IP del cliente. Detrás de un proxy (Render) la dirección remota es la del proxy, así que solo
     * con confiarProxy=true se usa CF-Connecting-IP o el primer valor de X-Forwarded-For. Un valor
     * con formato inválido se ignora y se usa la dirección remota.
     */
    static String ipCliente(String remota, String forwardedFor, String cfConnectingIp, boolean confiarProxy) {
        if (confiarProxy) {
            String cf = limpiarIp(cfConnectingIp);
            if (cf != null) return cf;
            if (forwardedFor != null) {
                String primera = limpiarIp(forwardedFor.split(",")[0]);
                if (primera != null) return primera;
            }
        }
        return remota == null || remota.isBlank() ? "0.0.0.0" : remota;
    }

    private static String limpiarIp(String valor) {
        if (valor == null) return null;
        String v = valor.trim();
        if (v.isEmpty() || v.length() > 45 || !v.matches("[0-9a-fA-F:.]+")) return null;
        return v;
    }
}
