package com.unev.sige.model;

import java.time.LocalDate;

/** Fila de un informe de inscripciones, limitada por el rol del solicitante. */
public record LineaReporteInscripcion(LocalDate fecha, String estudiante, String matricula,
        String periodo, String asignatura, String grado, String area, int idSeccion, String estado) {
}