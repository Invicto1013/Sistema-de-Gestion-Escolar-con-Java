package com.unev.sige.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gestiona el calendario académico institucional del SIGE:
 * períodos, feriados, fechas de exámenes y días pedagógicos.
 */
public final class GestorCalendarioAcademico {

    public enum TipoEvento {
        INICIO_PERIODO, FIN_PERIODO, EXAMEN_PARCIAL, EXAMEN_FINAL,
        DIA_PEDAGOGICO, FERIADO_NACIONAL, SUSPENSION_DOCENTE, ACTO_GRADUACION
    }

    public record EventoAcademico(
            String codigo,
            String descripcion,
            LocalDate fecha,
            TipoEvento tipo
    ) {
        public EventoAcademico {
            Objects.requireNonNull(codigo, "El código no puede ser nulo");
            Objects.requireNonNull(descripcion, "La descripción no puede ser nula");
            Objects.requireNonNull(fecha, "La fecha no puede ser nula");
            Objects.requireNonNull(tipo, "El tipo no puede ser nulo");
            if (codigo.isBlank()) throw new IllegalArgumentException("El código no puede estar vacío");
        }
    }

    private final List<EventoAcademico> eventos = new ArrayList<>();
    private final int anioAcademico;

    public GestorCalendarioAcademico(int anioAcademico) {
        if (anioAcademico < 2000 || anioAcademico > 2100) {
            throw new IllegalArgumentException("Año académico fuera de rango: " + anioAcademico);
        }
        this.anioAcademico = anioAcademico;
    }

    public void registrarEvento(EventoAcademico evento) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        boolean duplicado = eventos.stream()
                .anyMatch(e -> e.codigo().equals(evento.codigo()));
        if (duplicado) {
            throw new IllegalStateException("Ya existe un evento con código: " + evento.codigo());
        }
        eventos.add(evento);
    }

    public List<EventoAcademico> obtenerEventosPorTipo(TipoEvento tipo) {
        return eventos.stream()
                .filter(e -> e.tipo() == tipo)
                .sorted((a, b) -> a.fecha().compareTo(b.fecha()))
                .toList();
    }

    public List<EventoAcademico> obtenerEventosEnRango(LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(desde);
        Objects.requireNonNull(hasta);
        if (desde.isAfter(hasta)) throw new IllegalArgumentException("El rango de fechas es inválido");
        return eventos.stream()
                .filter(e -> !e.fecha().isBefore(desde) && !e.fecha().isAfter(hasta))
                .sorted((a, b) -> a.fecha().compareTo(b.fecha()))
                .toList();
    }

    public boolean esDiaHabil(LocalDate fecha) {
        Objects.requireNonNull(fecha);
        return eventos.stream()
                .filter(e -> e.fecha().equals(fecha))
                .noneMatch(e -> e.tipo() == TipoEvento.FERIADO_NACIONAL
                             || e.tipo() == TipoEvento.SUSPENSION_DOCENTE);
    }

    public int contarDiasHabilesEnRango(LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(desde);
        Objects.requireNonNull(hasta);
        int dias = 0;
        LocalDate cursor = desde;
        while (!cursor.isAfter(hasta)) {
            int dow = cursor.getDayOfWeek().getValue();
            if (dow <= 5 && esDiaHabil(cursor)) {
                dias++;
            }
            cursor = cursor.plusDays(1);
        }
        return dias;
    }

    public int getAnioAcademico() { return anioAcademico; }
    public List<EventoAcademico> getEventos() { return Collections.unmodifiableList(eventos); }
}
