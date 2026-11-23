package com.unev.sige.model;

/**
 * Control y Certificación del Programa de Vinculación Comunitaria y Liderazgo Estudiantil.
 * Modelo autónomo y configurable por la institución escolar para acreditar proyectos de extensión,
 * servicio comunitario o liderazgo cívico con metas horarias adaptables al reglamento interno del centro.
 */
public final class ControlServicioSocial {

    /** Meta de horas comunitarias estándar institucional (configurable). */
    public static final int HORAS_DEFAULT_REQUERIDAS = 60;

    public enum EstadoServicio {
        COMPLETADO("Acreditación Completada"),
        EN_PROCESO("En Progreso"),
        INICIAL("Pendiente de Asignación");

        private final String descripcion;

        EstadoServicio(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record ExpedienteServicio(
            int horasAcumuladas,
            int horasMeta,
            int horasRestantes,
            double porcentajeCumplido,
            EstadoServicio estado,
            boolean acreditado
    ) { }

    private ControlServicioSocial() {
    }

    /**
     * Evalúa el cumplimiento con meta horaria configurable por la institución.
     */
    public static ExpedienteServicio evaluarConMeta(int horasAcumuladas, int metaHoras) {
        int meta = metaHoras > 0 ? metaHoras : HORAS_DEFAULT_REQUERIDAS;
        int acum = Math.max(0, horasAcumuladas);
        int restantes = Math.max(0, meta - acum);
        double pct = Math.min(100.0, (acum * 100.0) / meta);

        EstadoServicio estado;
        boolean acreditado;

        if (acum >= meta) {
            estado = EstadoServicio.COMPLETADO;
            acreditado = true;
        } else if (acum > 0) {
            estado = EstadoServicio.EN_PROCESO;
            acreditado = false;
        } else {
            estado = EstadoServicio.INICIAL;
            acreditado = false;
        }

        return new ExpedienteServicio(acum, meta, restantes, Math.round(pct * 100.0) / 100.0, estado, acreditado);
    }

    /**
     * Evalúa con la meta horaria estándar de 60 horas.
     */
    public static ExpedienteServicio evaluar(int horasAcumuladas) {
        return evaluarConMeta(horasAcumuladas, HORAS_DEFAULT_REQUERIDAS);
    }
}
