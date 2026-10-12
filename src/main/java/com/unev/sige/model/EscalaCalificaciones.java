package com.unev.sige.model;

/**
 * Reglas de calificacion del centro, TODAS en un solo lugar: pesos de cada nota, nota minima
 * para aprobar y escala de letras. Si tu institucion usa otras reglas, cambia solo este archivo.
 * Las notas van de 0 a 100.
 */
public final class EscalaCalificaciones {

    /** Peso de cada nota en la nota final. Deben sumar 1.0 (100%). */
    public static final double PESO_PARCIAL1 = 0.30;
    public static final double PESO_PARCIAL2 = 0.30;
    public static final double PESO_EXAMEN_FINAL = 0.40;

    /** Nota final minima para aprobar una materia. */
    public static final double NOTA_MINIMA_APROBAR = 70.0;

    private EscalaCalificaciones() {
    }

    /**
     * Nota final = parcial 1 * peso + parcial 2 * peso + examen final * peso, redondeada a 2 decimales.
     */
    public static double calcularNotaFinal(double parcial1, double parcial2, double examenFinal) {
        double nota = parcial1 * PESO_PARCIAL1 + parcial2 * PESO_PARCIAL2 + examenFinal * PESO_EXAMEN_FINAL;
        return Math.round(nota * 100.0) / 100.0;
    }

    /**
     * Letra de la nota: A (90 o mas), B (80), C (70), D (60) y F (menos de 60).
     */
    public static String literal(double notaFinal) {
        if (notaFinal >= 90.0) {
            return "A";
        }
        if (notaFinal >= 80.0) {
            return "B";
        }
        if (notaFinal >= 70.0) {
            return "C";
        }
        if (notaFinal >= 60.0) {
            return "D";
        }
        return "F";
    }

    public static boolean aprueba(double notaFinal) {
        return notaFinal >= NOTA_MINIMA_APROBAR;
    }

    /**
     * Texto con la formula, para mostrarlo en pantalla (por ejemplo "30% parcial 1 + ...").
     */
    public static String descripcionFormula() {
        return String.format("Nota final = %.0f%% parcial 1 + %.0f%% parcial 2 + %.0f%% examen final. "
                        + "Se aprueba con %.0f o más.",
                PESO_PARCIAL1 * 100, PESO_PARCIAL2 * 100, PESO_EXAMEN_FINAL * 100, NOTA_MINIMA_APROBAR);
    }
}
