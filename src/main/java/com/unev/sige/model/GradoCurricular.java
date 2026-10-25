package com.unev.sige.model;

import java.util.List;

/** Grado del catalogo curricular y las areas asignadas a ese grado. */
public record GradoCurricular(int id, String codigo, String nivel, String ciclo, String nombre, List<String> areas) {
    public GradoCurricular { areas = List.copyOf(areas); }
    @Override public String toString() { return nombre; }
}