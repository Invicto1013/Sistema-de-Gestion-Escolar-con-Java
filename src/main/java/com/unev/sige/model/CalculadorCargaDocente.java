package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Calculador de carga docente escolar (Créditos, horas lectivas y límite de secciones).
 * Previene la sobrecarga horaria del profesorado, valida la dedicación laboral
 * (Tiempo Completo vs Medio Tiempo) y calcula el ratio de alumnos atendidos.
 */
public final class CalculadorCargaDocente {

    public static final int MAX_HORAS_SEMANALES_COMPLETO = 40;
    public static final int MAX_HORAS_SEMANALES_MEDIO = 20;
    public static final int MAX_SECCIONES_RECOMENDADO = 6;

    public enum ModalidadContrato {
        TIEMPO_COMPLETO(MAX_HORAS_SEMANALES_COMPLETO),
        MEDIO_TIEMPO(MAX_HORAS_SEMANALES_MEDIO);

        private final int limiteHoras;

        ModalidadContrato(int limiteHoras) {
            this.limiteHoras = limiteHoras;
        }

        public int getLimiteHoras() {
            return limiteHoras;
        }
    }

    public record EvaluacionCarga(
            int totalHorasSemanales,
            int totalSecciones,
            int totalEstudiantes,
            double promedioEstudiantesPorSeccion,
            boolean sobrecargaHoras,
            boolean sobrecargaSecciones,
            String estadoCarga
    ) { }

    private CalculadorCargaDocente() {
    }

    /**
     * Evalúa la carga académica de un profesor según sus horas y secciones asignadas.
     */
    public static EvaluacionCarga evaluar(int horasSemanales, int secciones, int estudiantes, ModalidadContrato contrato) {
        int horas = Math.max(0, horasSemanales);
        int sec = Math.max(0, secciones);
        int est = Math.max(0, estudiantes);
        ModalidadContrato mod = contrato != null ? contrato : ModalidadContrato.TIEMPO_COMPLETO;

        boolean sobreHoras = horas > mod.getLimiteHoras();
        boolean sobreSec = sec > MAX_SECCIONES_RECOMENDADO;

        double promEst = sec > 0
                ? BigDecimal.valueOf(est).divide(BigDecimal.valueOf(sec), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        String estado;
        if (sobreHoras || sobreSec) {
            estado = "SOBRECARGA";
        } else if (horas >= mod.getLimiteHoras() * 0.8) {
            estado = "CARGA_COMPLETA";
        } else {
            estado = "CARGA_MODERADA";
        }

        return new EvaluacionCarga(horas, sec, est, promEst, sobreHoras, sobreSec, estado);
    }
}
