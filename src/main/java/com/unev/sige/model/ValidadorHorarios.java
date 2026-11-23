package com.unev.sige.model;

import java.time.LocalTime;
import java.util.Collection;

/**
 * Validador de consistencia y conflicto de horarios escolares (Choque de Horarios).
 * Verifica superposiciones horarias entre asignaturas inscritas por un estudiante,
 * asignaciones de un profesor o disponibilidad física de un aula.
 */
public final class ValidadorHorarios {

    public record ConflictoHorario(
            HorarioSeccion bloque1,
            HorarioSeccion bloque2,
            String motivo
    ) { }

    private ValidadorHorarios() {
    }

    /**
     * Comprueba si dos bloques horarios se solapan en el mismo día.
     */
    public static boolean haySolapamiento(HorarioSeccion a, HorarioSeccion b) {
        if (a == null || b == null) return false;
        if (a.getDiaSemana() == null || b.getDiaSemana() == null) return false;
        if (!a.getDiaSemana().trim().equalsIgnoreCase(b.getDiaSemana().trim())) return false;

        LocalTime inicioA = a.getHoraInicio();
        LocalTime finA = a.getHoraFin();
        LocalTime inicioB = b.getHoraInicio();
        LocalTime finB = b.getHoraFin();

        if (inicioA == null || finA == null || inicioB == null || finB == null) return false;

        // Choque si: inicioA < finB Y inicioB < finA
        return inicioA.isBefore(finB) && inicioB.isBefore(finA);
    }

    /**
     * Valida una lista de bloques horarios buscando solapamientos.
     * Retorna el primer conflicto encontrado, o null si el horario es válido.
     */
    public static ConflictoHorario buscarConflicto(Collection<HorarioSeccion> bloques) {
        if (bloques == null || bloques.size() < 2) {
            return null;
        }

        HorarioSeccion[] array = bloques.toArray(new HorarioSeccion[0]);
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (haySolapamiento(array[i], array[j])) {
                    String motivo = String.format("Conflicto el %s entre %s (%s-%s) y %s (%s-%s)",
                            array[i].getDiaSemana(),
                            array[i].getAsignaturaNombre() != null ? array[i].getAsignaturaNombre() : "Bloque " + array[i].getId(),
                            array[i].getHoraInicio(), array[i].getHoraFin(),
                            array[j].getAsignaturaNombre() != null ? array[j].getAsignaturaNombre() : "Bloque " + array[j].getId(),
                            array[j].getHoraInicio(), array[j].getHoraFin());
                    return new ConflictoHorario(array[i], array[j], motivo);
                }
            }
        }

        return null;
    }
}
