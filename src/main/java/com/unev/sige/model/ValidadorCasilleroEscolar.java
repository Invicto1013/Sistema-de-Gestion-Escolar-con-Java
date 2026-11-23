package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Códigos de Armilleros o Casilleros Escolares (Lockers).
 * Asigna y valida los casilleros de estudiantes por piso y pabellón escolar.
 */
public final class ValidadorCasilleroEscolar {

    private static final Pattern PATRON_CASILLERO = Pattern.compile("^CAS-[A-Z]-[0-9]{3}$");

    private ValidadorCasilleroEscolar() {
    }

    public static boolean esCodigoCasilleroValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_CASILLERO.matcher(codigo.trim().toUpperCase()).matches();
    }

    public static String generarCodigo(char pabellon, int numeroCasillero) {
        char p = Character.toUpperCase(pabellon);
        int num = Math.max(1, Math.min(999, numeroCasillero));
        return String.format("CAS-%c-%03d", p, num);
    }
}
