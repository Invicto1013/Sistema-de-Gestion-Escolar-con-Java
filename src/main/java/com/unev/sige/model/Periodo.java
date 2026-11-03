package com.unev.sige.model;

import java.time.LocalDate;

/** Periodo lectivo (por ejemplo "2026-1") con su estado y, si esta cerrado, quien lo cerro y cuando. */
public record Periodo(int id, String nombre, LocalDate inicio, LocalDate fin, EstadoPeriodo estado,
                      String cerradoPor, String fechaCierre) {

    public boolean cerrado() {
        return estado == EstadoPeriodo.CERRADO;
    }
}
