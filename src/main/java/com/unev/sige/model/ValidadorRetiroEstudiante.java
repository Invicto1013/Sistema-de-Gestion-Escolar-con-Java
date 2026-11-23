package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Carnets y Autorizaciones de Retiro de Estudiantes por Terceros.
 * Controla la entrega segura de alumnos al salir del plantel mediante código PIN o credencial.
 */
public final class ValidadorRetiroEstudiante {

    private static final Pattern PATRON_PIN = Pattern.compile("^[0-9]{4,6}$");

    public record AutorizadoRetiro(
            String nombrePersona,
            String parentesco,
            String cedulaIdentidad,
            String pinSeguridad,
            boolean estaActivo
    ) { }

    private ValidadorRetiroEstudiante() {
    }

    public static boolean esPinValido(String pin) {
        if (pin == null) return false;
        return PATRON_PIN.matcher(pin.trim()).matches();
    }

    public static boolean puedeRetirar(AutorizadoRetiro persona, String pinIngresado) {
        if (persona == null || !persona.estaActivo() || pinIngresado == null) return false;
        return persona.pinSeguridad() != null && persona.pinSeguridad().trim().equals(pinIngresado.trim());
    }
}
