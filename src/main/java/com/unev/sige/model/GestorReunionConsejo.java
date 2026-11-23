package com.unev.sige.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Gestiona reuniones del consejo de docentes/directivo del SIGE:
 * convocatoria, quórum, acta y resoluciones.
 */
public final class GestorReunionConsejo {

    public enum TipoReunion { ORDINARIA, EXTRAORDINARIA, EVALUACION, PLANIFICACION }
    public enum EstatusReunion { CONVOCADA, EN_CURSO, FINALIZADA, SUSPENDIDA }

    public record Resolucion(String numeroResolucion, String descripcion) {
        public Resolucion {
            Objects.requireNonNull(numeroResolucion);
            Objects.requireNonNull(descripcion);
            if (numeroResolucion.isBlank()) throw new IllegalArgumentException("Número de resolución vacío");
        }
    }

    private final String codigoReunion;
    private final TipoReunion tipo;
    private final LocalDateTime fechaHora;
    private final int quorumMinimo;
    private final List<String> convocados = new ArrayList<>();
    private final List<String> asistentes = new ArrayList<>();
    private final List<Resolucion> resoluciones = new ArrayList<>();
    private EstatusReunion estatus = EstatusReunion.CONVOCADA;

    public GestorReunionConsejo(String codigoReunion, TipoReunion tipo,
                                LocalDateTime fechaHora, int quorumMinimo) {
        if (codigoReunion == null || !codigoReunion.startsWith("CONS-")) {
            throw new IllegalArgumentException("Código de reunión inválido; debe iniciar con CONS-");
        }
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(fechaHora);
        if (quorumMinimo < 1) throw new IllegalArgumentException("El quórum mínimo debe ser al menos 1");
        this.codigoReunion = codigoReunion;
        this.tipo = tipo;
        this.fechaHora = fechaHora;
        this.quorumMinimo = quorumMinimo;
    }

    public void convocar(String codigoDocente) {
        Objects.requireNonNull(codigoDocente);
        if (!convocados.contains(codigoDocente)) convocados.add(codigoDocente);
    }

    public void registrarAsistencia(String codigoDocente) {
        if (!convocados.contains(codigoDocente)) {
            throw new IllegalStateException("El docente no fue convocado: " + codigoDocente);
        }
        if (!asistentes.contains(codigoDocente)) asistentes.add(codigoDocente);
    }

    public boolean hayQuorum() {
        return asistentes.size() >= quorumMinimo;
    }

    public void iniciarReunion() {
        if (estatus != EstatusReunion.CONVOCADA) {
            throw new IllegalStateException("La reunión no está en estado CONVOCADA");
        }
        if (!hayQuorum()) {
            throw new IllegalStateException("No se cumple el quórum mínimo de " + quorumMinimo);
        }
        this.estatus = EstatusReunion.EN_CURSO;
    }

    public void agregarResolucion(Resolucion resolucion) {
        if (estatus != EstatusReunion.EN_CURSO) {
            throw new IllegalStateException("Solo se pueden agregar resoluciones durante la reunión");
        }
        Objects.requireNonNull(resolucion);
        resoluciones.add(resolucion);
    }

    public void finalizarReunion() {
        if (estatus != EstatusReunion.EN_CURSO) {
            throw new IllegalStateException("La reunión no está en curso");
        }
        this.estatus = EstatusReunion.FINALIZADA;
    }

    public String generarActaResumen() {
        return String.format("ACTA %s | Tipo: %s | Fecha: %s | Asistentes: %d/%d | Resoluciones: %d",
                codigoReunion, tipo, fechaHora, asistentes.size(), convocados.size(), resoluciones.size());
    }

    public String getCodigoReunion() { return codigoReunion; }
    public TipoReunion getTipo() { return tipo; }
    public EstatusReunion getEstatus() { return estatus; }
    public List<String> getConvocados() { return Collections.unmodifiableList(convocados); }
    public List<String> getAsistentes() { return Collections.unmodifiableList(asistentes); }
    public List<Resolucion> getResoluciones() { return Collections.unmodifiableList(resoluciones); }
}
