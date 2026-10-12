package com.unev.sige.model;

import java.time.LocalDate;

/** Asistencia de un estudiante para una materia inscrita en una fecha. */
public record FilaAsistencia(int idDetalleInscripcion, String matricula, String estudiante,
                             LocalDate fecha, Boolean presente) {
}
