package com.unev.sige.model;

/**
 * Una fila de la lista de una seccion: el estudiante inscrito, su periodo, el estado de la materia
 * y su calificacion. Es lo que ve el profesor al capturar notas.
 */
public record NotaEstudiante(Estudiante estudiante, String periodo, int idDetalleInscripcion,
                             String estadoMateria, Calificacion calificacion) {
}
