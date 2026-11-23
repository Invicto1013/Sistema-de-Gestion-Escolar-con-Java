package com.unev.sige.model;

import java.util.Collection;

/**
 * Evaluación disciplinaria y formativa escolar (MINERD / Normas de Convivencia Escolar).
 * Consolida los méritos, tardanzas y faltas conductuales para asignar la calificación
 * de conducta (A, B, C, D) y determinar si requiere intervención del Departamento
 * de Orientación y Psicología.
 */
public final class EvaluacionConducta {

    public enum CalificacionConducta {
        A("Excelente convivencia y méritos destacados"),
        B("Buena conducta escolar"),
        C("Regular / Requiere seguimiento"),
        D("Deficiente / Citación a padres y orientación");

        private final String descripcion;

        CalificacionConducta(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record ResumenConducta(
            int totalObservaciones,
            int meritos,
            int faltasConducta,
            int tardanzas,
            int tareasIncumplidas,
            int puntajeNeto,
            CalificacionConducta calificacion,
            boolean requiereOrientacion
    ) { }

    private EvaluacionConducta() {
    }

    /**
     * Evalúa el expediente de observaciones disciplinarias de un estudiante.
     * Base inicial: 100 puntos.
     * - Cada MERITO suma +5 puntos (máximo 100).
     * - Cada FALTA CONDUCTA resta -10 puntos.
     * - Cada TARDANZA resta -2 puntos.
     * - Cada TAREA resta -3 puntos.
     */
    public static ResumenConducta evaluar(Collection<ObservacionConducta> observaciones) {
        if (observaciones == null || observaciones.isEmpty()) {
            return new ResumenConducta(0, 0, 0, 0, 0, 100, CalificacionConducta.A, false);
        }

        int meritos = 0;
        int faltas = 0;
        int tardanzas = 0;
        int tareas = 0;

        for (ObservacionConducta obs : observaciones) {
            if (obs == null || obs.getTipo() == null) continue;
            String tipo = obs.getTipo().toUpperCase().trim();
            switch (tipo) {
                case "MERITO" -> meritos++;
                case "CONDUCTA" -> faltas++;
                case "TARDANZA" -> tardanzas++;
                case "TAREA" -> tareas++;
                default -> { } // NOTA_INFORMATIVA no afecta puntuación
            }
        }

        int total = meritos + faltas + tardanzas + tareas;
        int puntaje = 100 + (meritos * 5) - (faltas * 10) - (tardanzas * 2) - (tareas * 3);
        puntaje = Math.max(0, Math.min(100, puntaje));

        CalificacionConducta calif;
        if (puntaje >= 90) {
            calif = CalificacionConducta.A;
        } else if (puntaje >= 80) {
            calif = CalificacionConducta.B;
        } else if (puntaje >= 70) {
            calif = CalificacionConducta.C;
        } else {
            calif = CalificacionConducta.D;
        }

        boolean orientacion = faltas >= 3 || tardanzas >= 5 || puntaje < 75;

        return new ResumenConducta(total, meritos, faltas, tardanzas, tareas, puntaje, calif, orientacion);
    }
}
