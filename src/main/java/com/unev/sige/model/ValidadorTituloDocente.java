package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador del Número de Título Universitario y Colegiatura del Personal Docente (CODIA / ADP / MESCyT).
 * Registra y valida la idoneidad profesional de los educadores contratados por el centro.
 */
public final class ValidadorTituloDocente {

    private static final Pattern PATRON_REGISTRO_TITULO = Pattern.compile("^TIT-[0-9]{4}-[0-9]{4,6}$");

    public record AcreditacionDocente(
            String numeroRegistroTitulo,
            String tituloObtenido,
            String universidadEmisora,
            int anioGraduacion
    ) { }

    private ValidadorTituloDocente() {
    }

    public static boolean esNumeroRegistroValido(String numeroRegistro) {
        if (numeroRegistro == null) return false;
        return PATRON_REGISTRO_TITULO.matcher(numeroRegistro.trim().toUpperCase()).matches();
    }

    public static boolean esAcreditacionValida(AcreditacionDocente acre) {
        if (acre == null) return false;
        return esNumeroRegistroValido(acre.numeroRegistroTitulo())
                && acre.tituloObtenido() != null && !acre.tituloObtenido().isBlank()
                && acre.anioGraduacion() >= 1950 && acre.anioGraduacion() <= 2030;
    }
}
