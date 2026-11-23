package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Gestor del Fondo Editorial y Depósito de Publicaciones Estudiantiles (Periódico Escolar y Revista Científica).
 * Asigna códigos de registro de artículos e investigaciones escolares.
 */
public final class GestorPublicacionEscolar {

    private static final Pattern PATRON_PUBLICACION = Pattern.compile("^PUB-[A-Z]{3}-[0-9]{4}-[0-9]{3}$");

    public record ArticuloEstudiantil(
            String codigoPublicacion,
            String titulo,
            String seccionPeriodico, // OPINION, CIENCIA, CULTURA, DEPORTES
            String autorEstudiante,
            boolean revisadoPorDocente
    ) { }

    private GestorPublicacionEscolar() {
    }

    public static boolean esCodigoPublicacionValido(String codigo) {
        if (codigo == null) return false;
        return PATRON_PUBLICACION.matcher(codigo.trim().toUpperCase()).matches();
    }
}
