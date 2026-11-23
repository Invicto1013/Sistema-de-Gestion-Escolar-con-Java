package com.unev.sige.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Valida permisos de permanencia extendida de estudiantes en el plantel
 * fuera del horario regular, según políticas del SIGE.
 */
public final class ValidadorPermisoPermanencia {

    private static final LocalTime HORA_SALIDA_REGULAR = LocalTime.of(17, 0);
    private static final LocalTime HORA_MAXIMA_PERMITIDA = LocalTime.of(21, 0);
    private static final int MAX_DIAS_CONSECUTIVOS = 5;

    public enum MotivoPermanencia {
        ACTIVIDAD_EXTRACURRICULAR, PROYECTO_ACADEMICO, ENSAYO_CULTURAL,
        ENTRENAMIENTO_DEPORTIVO, EVENTO_INSTITUCIONAL, OTRO
    }

    public record SolicitudPermiso(
            String codigoPermiso,
            String codigoEstudiante,
            LocalTime horaSalida,
            int diasConsecutivos,
            MotivoPermanencia motivo,
            boolean autorizadoPorTutor
    ) {}

    private ValidadorPermisoPermanencia() {}

    public static ResultadoValidacion validar(SolicitudPermiso solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud no puede ser nula");

        if (solicitud.codigoPermiso() == null || !solicitud.codigoPermiso().startsWith("PERM-")) {
            return ResultadoValidacion.rechazar("El código debe iniciar con PERM-");
        }
        if (solicitud.codigoEstudiante() == null || solicitud.codigoEstudiante().isBlank()) {
            return ResultadoValidacion.rechazar("El código de estudiante es obligatorio");
        }
        if (solicitud.horaSalida() == null) {
            return ResultadoValidacion.rechazar("La hora de salida es obligatoria");
        }
        if (!solicitud.horaSalida().isAfter(HORA_SALIDA_REGULAR)) {
            return ResultadoValidacion.rechazar(
                    "La hora de salida debe ser posterior a las " + HORA_SALIDA_REGULAR);
        }
        if (solicitud.horaSalida().isAfter(HORA_MAXIMA_PERMITIDA)) {
            return ResultadoValidacion.rechazar(
                    "No se permiten permanencias más allá de las " + HORA_MAXIMA_PERMITIDA);
        }
        if (solicitud.diasConsecutivos() < 1 || solicitud.diasConsecutivos() > MAX_DIAS_CONSECUTIVOS) {
            return ResultadoValidacion.rechazar(
                    "Los días consecutivos deben estar entre 1 y " + MAX_DIAS_CONSECUTIVOS);
        }
        if (!solicitud.autorizadoPorTutor()) {
            return ResultadoValidacion.rechazar("Se requiere autorización del tutor/encargado");
        }
        if (solicitud.motivo() == null) {
            return ResultadoValidacion.rechazar("El motivo de permanencia es obligatorio");
        }
        return ResultadoValidacion.aprobar();
    }

    public static boolean esHorarioPermitido(LocalTime hora) {
        Objects.requireNonNull(hora);
        return hora.isAfter(HORA_SALIDA_REGULAR) && !hora.isAfter(HORA_MAXIMA_PERMITIDA);
    }

    public record ResultadoValidacion(boolean aprobado, String mensaje) {
        static ResultadoValidacion aprobar() {
            return new ResultadoValidacion(true, "Permiso de permanencia válido");
        }
        static ResultadoValidacion rechazar(String razon) {
            return new ResultadoValidacion(false, razon);
        }
    }
}
