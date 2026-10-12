package com.unev.sige.model;

/** Resumen de asistencias de una materia inscrita. */
public record ResumenAsistencia(String periodo, String codigoAsignatura, String asignatura,
                                int clasesRegistradas, int presentes, int ausentes) {
    public double porcentaje() {
        return clasesRegistradas == 0 ? 0.0 : presentes * 100.0 / clasesRegistradas;
    }
}
