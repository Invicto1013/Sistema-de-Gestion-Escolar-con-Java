package com.unev.sige.model;

/** Calificación de una materia visible para el padre/tutor vinculado. */
public record CalificacionHijo(String periodo, String codigoAsignatura, String asignatura,
                               double parcial1, double parcial2, double examenFinal,
                               double notaFinal, String literal, String estado) {
}
