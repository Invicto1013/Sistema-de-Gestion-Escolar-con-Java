package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorHorariosTest {

    private static HorarioSeccion crear(String dia, String ini, String fin, String materia) {
        HorarioSeccion h = new HorarioSeccion();
        h.setDiaSemana(dia);
        h.setHoraInicio(LocalTime.parse(ini));
        h.setHoraFin(LocalTime.parse(fin));
        h.setAsignaturaNombre(materia);
        return h;
    }

    @Test
    @DisplayName("Bloques en días distintos nunca colisionan")
    void haySolapamiento_diasDistintos_noColisionan() {
        HorarioSeccion h1 = crear("LUNES", "08:00", "10:00", "Matemáticas");
        HorarioSeccion h2 = crear("MARTES", "08:00", "10:00", "Física");
        assertFalse(ValidadorHorarios.haySolapamiento(h1, h2));
    }

    @Test
    @DisplayName("Bloques consecutivos (uno termina cuando el otro empieza) no colisionan")
    void haySolapamiento_bloquesConsecutivos_noColisionan() {
        HorarioSeccion h1 = crear("LUNES", "08:00", "10:00", "Matemáticas");
        HorarioSeccion h2 = crear("LUNES", "10:00", "12:00", "Física");
        assertFalse(ValidadorHorarios.haySolapamiento(h1, h2));
    }

    @Test
    @DisplayName("Bloques que se cruzan en el mismo día colisionan")
    void haySolapamiento_bloquesSuperpuestos_colisionan() {
        HorarioSeccion h1 = crear("LUNES", "08:00", "10:00", "Matemáticas");
        HorarioSeccion h2 = crear("LUNES", "09:30", "11:30", "Física");
        assertTrue(ValidadorHorarios.haySolapamiento(h1, h2));
    }

    @Test
    @DisplayName("Detecta conflicto en una lista de asignaturas")
    void buscarConflicto_detectaPrimerConflicto() {
        List<HorarioSeccion> lista = List.of(
                crear("LUNES", "08:00", "10:00", "Matemáticas"),
                crear("LUNES", "10:00", "12:00", "Historia"),
                crear("LUNES", "09:00", "11:00", "Química")
        );

        ValidadorHorarios.ConflictoHorario c = ValidadorHorarios.buscarConflicto(lista);
        assertNotNull(c);
        assertTrue(c.motivo().contains("Matemáticas"));
        assertTrue(c.motivo().contains("Química"));
    }
}
