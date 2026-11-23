package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Identificadores de Red y Direcciones MAC para Aulas de Informática y Laboratorios STEM.
 * Controla el acceso seguro a la red WiFi del plantel y equipos asignados en salas de cómputo.
 */
public final class ValidadorLaboratorioInformatica {

    private static final Pattern PATRON_MAC = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    public record EquipoComputo(
            String codigoEquipo,
            String direccionMac,
            String laboratorioAsignado,
            boolean operativo
    ) { }

    private ValidadorLaboratorioInformatica() {
    }

    public static boolean esDireccionMacValida(String mac) {
        if (mac == null) return false;
        return PATRON_MAC.matcher(mac.trim()).matches();
    }
}
