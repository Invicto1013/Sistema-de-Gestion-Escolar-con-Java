package com.unev.sige.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Gestor del Banco de Libros de Texto Escolares.
 * Controla el préstamo anual de libros, estado de conservación físico (Bueno, Regular, Deteriorado)
 * y cálculo de reposición por extravío.
 */
public final class GestorBancoLibrosTexto {

    public enum EstadoFisicoLibro {
        EXCELENTE,
        BUENO,
        REGULAR,
        DETERIORADO_REQUIERE_REEMPLAZO
    }

    public record AsignacionLibro(
            String codigoEjemplar,
            String tituloLibro,
            EstadoFisicoLibro estadoEntrega,
            EstadoFisicoLibro estadoDevolucion,
            boolean devuelto
    ) { }

    private GestorBancoLibrosTexto() {
    }

    public static boolean requiereCobroReposicion(EstadoFisicoLibro entrega, EstadoFisicoLibro devolucion, boolean devuelto) {
        if (!devuelto) return true; // Extraviado
        if (entrega == null || devolucion == null) return false;
        // Si se entregó Excelente o Bueno y regresa Deteriorado
        return (entrega == EstadoFisicoLibro.EXCELENTE || entrega == EstadoFisicoLibro.BUENO)
                && devolucion == EstadoFisicoLibro.DETERIORADO_REQUIERE_REEMPLAZO;
    }
}
