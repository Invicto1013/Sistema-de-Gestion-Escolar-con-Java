package com.unev.sige.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validador de Actividades y Clubes Extracurriculares (Deportes, Música, Robótica, Debate, Arte).
 * Fomenta el desarrollo integral de los estudiantes validando la inscripción en clubes escolares,
 * control de cupos y horas semanales de dedicación complementaria.
 */
public final class ValidadorClubExtracurricular {

    public static final int MAX_CLUBES_POR_ESTUDIANTE = 3;

    public enum CategoriaClub {
        DEPORTIVO("Club Deportivo / Atletismo"),
        CIENCIA_TECNOLOGIA("Robótica, Ajedrez y Ciencias"),
        ARTE_CULTURA("Música, Teatro y Artes Plásticas"),
        LIDERAZGO_DEBATE("Modelo de Naciones Unidas y Debate");

        private final String descripcion;

        CategoriaClub(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public record RegistroClub(
            String codigoClub,
            String nombreClub,
            CategoriaClub categoria,
            int horasSemanales
    ) { }

    private ValidadorClubExtracurricular() {
    }

    /**
     * Valida si un estudiante puede inscribir un club adicional según el límite reglamentario (máx. 3).
     */
    public static boolean puedeInscribirClub(int clubesInscritosActualmente) {
        return clubesInscritosActualmente >= 0 && clubesInscritosActualmente < MAX_CLUBES_POR_ESTUDIANTE;
    }

    /**
     * Valida que las horas semanales del club sean razonables (entre 1 y 10 horas semanales).
     */
    public static boolean sonHorasValidas(int horasSemanales) {
        return horasSemanales >= 1 && horasSemanales <= 10;
    }
}
