package com.unev.sige.model;

/** Relación entre una cuenta de padre/tutor y un estudiante. */
public record VinculoPadreEstudiante(int idPadre, String padre, int idEstudiante,
                                     String estudiante, String matricula, String parentesco) {
}
