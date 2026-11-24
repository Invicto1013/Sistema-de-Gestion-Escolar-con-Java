package com.unev.sige.model;

import java.util.List;

/**
 * Plan de suscripción del software. Los precios son de referencia (RD$ por mes) y se ajustan aquí.
 * {@code maxUsuarios} = 0 significa sin límite.
 */
public record PlanServicio(String codigo, String nombre, String icono, String acento, int precioMensual,
                           int maxUsuarios, List<String> incluye, String resumen) {

    public boolean ilimitado() {
        return maxUsuarios <= 0;
    }

    /** Porcentaje de uso (0–100+). Sin límite devuelve 0. */
    public int porcentajeUso(int usuariosActivos) {
        return ilimitado() ? 0 : (int) Math.round(usuariosActivos * 100.0 / maxUsuarios);
    }

    /** Estado legible del uso: dentro del límite, cerca del tope (90 %) o excedido. */
    public String estadoUso(int usuariosActivos) {
        if (ilimitado()) return "Sin límite de usuarios";
        int p = porcentajeUso(usuariosActivos);
        if (usuariosActivos > maxUsuarios) return "Excede el límite del plan";
        if (p >= 90) return "Cerca del límite";
        return "Dentro del límite";
    }
}
