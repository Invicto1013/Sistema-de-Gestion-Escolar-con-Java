package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * Cálculo del índice académico acumulado y por período escolar (GPA / Promedio General).
 * Permite calcular el promedio simple o ponderado de calificaciones finales, determinar
 * honores académicos (Summa Cum Laude, Magna Cum Laude, Cum Laude / Cuadro de Honor)
 * y evaluar la condición académica del estudiante (Regular, A prueba, Alerta académica).
 */
public final class IndiceAcademico {

    public enum CondicionAcademica {
        EXCELENCIA("Cuadro de Honor / Excelencia"),
        REGULAR("Condición Normal"),
        ALERTA("Alerta Académica"),
        A_PRUEBA("A Prueba Académica");

        private final String descripcion;

        CondicionAcademica(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum HonorAcademico {
        SUMMA_CUM_LAUDE("Summa Cum Laude"),
        MAGNA_CUM_LAUDE("Magna Cum Laude"),
        CUM_LAUDE("Cum Laude"),
        NINGUNO("Sin distinción honorífica");

        private final String etiqueta;

        HonorAcademico(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    public record Resumen(
            int totalMaterias,
            int materiasAprobadas,
            int materiasReprobadas,
            double indice,
            String literalEquivalente,
            CondicionAcademica condicion,
            HonorAcademico honor
    ) {
        public boolean esPromovible() {
            return materiasReprobadas == 0 && indice >= EscalaCalificaciones.NOTA_MINIMA_APROBAR;
        }
    }

    private IndiceAcademico() {
    }

    /**
     * Calcula el índice académico promedio a partir de una lista de notas finales.
     * Retorna 0.0 si la lista está vacía o nula.
     */
    public static double calcularPromedio(Collection<Double> notasFinales) {
        if (notasFinales == null || notasFinales.isEmpty()) {
            return 0.0;
        }

        BigDecimal suma = BigDecimal.ZERO;
        int conteo = 0;

        for (Double nota : notasFinales) {
            if (nota != null && !Double.isNaN(nota) && nota >= 0.0) {
                suma = suma.add(BigDecimal.valueOf(nota));
                conteo++;
            }
        }

        if (conteo == 0) {
            return 0.0;
        }

        return suma.divide(BigDecimal.valueOf(conteo), 2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Evalúa el conjunto de líneas de boleta de un estudiante para consolidar su índice
     * y situación académica escolar.
     */
    public static Resumen evaluarBoleta(Collection<LineaBoleta> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return new Resumen(0, 0, 0, 0.0, "F", CondicionAcademica.REGULAR, HonorAcademico.NINGUNO);
        }

        BigDecimal suma = BigDecimal.ZERO;
        int total = 0;
        int aprobadas = 0;
        int reprobadas = 0;

        for (LineaBoleta linea : lineas) {
            if (linea == null) continue;
            double nf = Calificacion.limitar(linea.notaFinal());
            suma = suma.add(BigDecimal.valueOf(nf));
            total++;

            if (EscalaCalificaciones.aprueba(nf)) {
                aprobadas++;
            } else {
                reprobadas++;
            }
        }

        if (total == 0) {
            return new Resumen(0, 0, 0, 0.0, "F", CondicionAcademica.REGULAR, HonorAcademico.NINGUNO);
        }

        double indice = suma.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue();
        String literal = EscalaCalificaciones.literal(indice);

        CondicionAcademica condicion;
        if (indice >= 90.0 && reprobadas == 0) {
            condicion = CondicionAcademica.EXCELENCIA;
        } else if (indice >= EscalaCalificaciones.NOTA_MINIMA_APROBAR && reprobadas == 0) {
            condicion = CondicionAcademica.REGULAR;
        } else if (reprobadas == 1 || (indice >= 65.0 && indice < EscalaCalificaciones.NOTA_MINIMA_APROBAR)) {
            condicion = CondicionAcademica.ALERTA;
        } else {
            condicion = CondicionAcademica.A_PRUEBA;
        }

        HonorAcademico honor;
        if (reprobadas == 0) {
            if (indice >= 95.0) {
                honor = HonorAcademico.SUMMA_CUM_LAUDE;
            } else if (indice >= 90.0) {
                honor = HonorAcademico.MAGNA_CUM_LAUDE;
            } else if (indice >= 85.0) {
                honor = HonorAcademico.CUM_LAUDE;
            } else {
                honor = HonorAcademico.NINGUNO;
            }
        } else {
            honor = HonorAcademico.NINGUNO;
        }

        return new Resumen(total, aprobadas, reprobadas, indice, literal, condicion, honor);
    }
}
