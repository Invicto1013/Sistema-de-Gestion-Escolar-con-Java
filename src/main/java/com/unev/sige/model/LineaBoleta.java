package com.unev.sige.model;

/** Una materia dentro de la boleta de calificaciones de un período. */
public record LineaBoleta(String periodo, String codigoAsignatura, String asignatura,
                          double parcial1, double parcial2, double examenFinal,
                          double notaFinal, String literal, String estado) {
}
