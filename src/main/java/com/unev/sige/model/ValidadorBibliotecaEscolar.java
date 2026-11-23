package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de ISBN y Código de Barra para recursos de Biblioteca Escolar y Material Didáctico.
 * Soporta validación de ISBN-10 e ISBN-13 con cálculo oficial de suma de comprobación (checksum).
 */
public final class ValidadorBibliotecaEscolar {

    private static final Pattern PATRON_ISBN10 = Pattern.compile("^[0-9]{9}[0-9X]$");
    private static final Pattern PATRON_ISBN13 = Pattern.compile("^(?:978|979)[0-9]{10}$");

    private ValidadorBibliotecaEscolar() {
    }

    /**
     * Limpia un ISBN eliminando guiones y espacios.
     */
    public static String limpiarIsbn(String isbn) {
        if (isbn == null) return "";
        return isbn.replaceAll("[^0-9Xx]", "").toUpperCase();
    }

    /**
     * Valida un ISBN-10 con su dígito de control por pesos descendentes 10 a 1.
     */
    public static boolean esIsbn10Valido(String isbn) {
        String limpio = limpiarIsbn(isbn);
        if (!PATRON_ISBN10.matcher(limpio).matches()) {
            return false;
        }

        int suma = 0;
        for (int i = 0; i < 9; i++) {
            suma += Character.getNumericValue(limpio.charAt(i)) * (10 - i);
        }

        char ultimo = limpio.charAt(9);
        int digitoFinal = (ultimo == 'X') ? 10 : Character.getNumericValue(ultimo);
        suma += digitoFinal;

        return (suma % 11) == 0;
    }

    /**
     * Valida un ISBN-13 con pesos alternados 1 y 3 (estándar EAN-13).
     */
    public static boolean esIsbn13Valido(String isbn) {
        String limpio = limpiarIsbn(isbn);
        if (!PATRON_ISBN13.matcher(limpio).matches()) {
            return false;
        }

        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(limpio.charAt(i));
            suma += (i % 2 == 0) ? digito : digito * 3;
        }

        int verificadorCalculado = (10 - (suma % 10)) % 10;
        int verificadorReal = Character.getNumericValue(limpio.charAt(12));

        return verificadorCalculado == verificadorReal;
    }

    /**
     * Valida si un texto es un ISBN válido (sea ISBN-10 o ISBN-13).
     */
    public static boolean esIsbnValido(String isbn) {
        String limpio = limpiarIsbn(isbn);
        if (limpio.length() == 10) {
            return esIsbn10Valido(limpio);
        } else if (limpio.length() == 13) {
            return esIsbn13Valido(limpio);
        }
        return false;
    }
}
