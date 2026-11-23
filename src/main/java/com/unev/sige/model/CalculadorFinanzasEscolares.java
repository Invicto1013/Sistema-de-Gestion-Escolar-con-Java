package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Calculador y Gestor de Cuotas Escolares, Mensualidades y Recargos por Mora.
 * Permite a la administración escolar proyectar pagos de matrícula y colegiatura,
 * aplicar descuentos por pronto pago o hermanos matriculados, y calcular recargos por mora.
 */
public final class CalculadorFinanzasEscolares {

    public static final double TASA_DESCUENTO_HERMANO = 0.10; // 10% descuento por hermano
    public static final double TASA_DESCUENTO_PRONTO_PAGO = 0.05; // 5% por pago anticipado
    public static final double TASA_RECARGO_MORA_MENSUAL = 0.05; // 5% de recargo por mes vencido

    public record EstadoCuentaEstudiante(
            double montoBase,
            double descuentoAplicado,
            double recargoMora,
            double saldoFinalPagar,
            boolean alDia
    ) { }

    private CalculadorFinanzasEscolares() {
    }

    /**
     * Calcula la colegiatura mensual considerando beneficios de familia, pronto pago y días de retraso.
     */
    public static EstadoCuentaEstudiante calcularCuota(double montoMensualidad, int cantidadHermanos,
                                                      boolean prontoPago, int mesesVencidos) {
        double base = Math.max(0.0, montoMensualidad);
        BigDecimal baseBd = BigDecimal.valueOf(base);

        double pctDescuento = 0.0;
        if (cantidadHermanos > 1) {
            pctDescuento += TASA_DESCUENTO_HERMANO;
        }
        if (prontoPago && mesesVencidos == 0) {
            pctDescuento += TASA_DESCUENTO_PRONTO_PAGO;
        }

        BigDecimal descuento = baseBd.multiply(BigDecimal.valueOf(pctDescuento)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal recargo = BigDecimal.ZERO;
        if (mesesVencidos > 0) {
            double factorMora = Math.min(0.20, mesesVencidos * TASA_RECARGO_MORA_MENSUAL); // Tope 20%
            recargo = baseBd.multiply(BigDecimal.valueOf(factorMora)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal finalPagar = baseBd.subtract(descuento).add(recargo).setScale(2, RoundingMode.HALF_UP);
        boolean alDia = mesesVencidos == 0;

        return new EstadoCuentaEstudiante(base, descuento.doubleValue(), recargo.doubleValue(), finalPagar.doubleValue(), alDia);
    }
}
