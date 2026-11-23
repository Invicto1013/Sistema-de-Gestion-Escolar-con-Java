package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Calcula estadísticas de promedio grupal para una sección:
 * media, mediana, moda, nota máxima/mínima y distribución por rangos.
 */
public final class CalculadorPromedioGrupal {

    private CalculadorPromedioGrupal() {}

    public record EstadisticasGrupo(
            BigDecimal promedio,
            BigDecimal mediana,
            BigDecimal notaMaxima,
            BigDecimal notaMinima,
            long aprobados,
            long reprobados,
            long totalEstudiantes
    ) {
        public BigDecimal tasaAprobacion() {
            if (totalEstudiantes == 0) return BigDecimal.ZERO;
            return new BigDecimal(aprobados)
                    .divide(new BigDecimal(totalEstudiantes), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static final BigDecimal NOTA_APROBACION = new BigDecimal("70.00");

    public static EstadisticasGrupo calcular(List<BigDecimal> notas) {
        Objects.requireNonNull(notas, "La lista de notas no puede ser nula");
        if (notas.isEmpty()) throw new IllegalArgumentException("La lista de notas no puede estar vacía");

        for (BigDecimal n : notas) {
            if (n == null) throw new IllegalArgumentException("Ninguna nota puede ser nula");
            if (n.compareTo(BigDecimal.ZERO) < 0 || n.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Nota fuera de rango [0,100]: " + n);
            }
        }

        List<BigDecimal> ordenadas = new ArrayList<>(notas);
        Collections.sort(ordenadas);

        BigDecimal suma = notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = suma.divide(new BigDecimal(notas.size()), 2, RoundingMode.HALF_UP);

        BigDecimal mediana;
        int tam = ordenadas.size();
        if (tam % 2 == 0) {
            mediana = ordenadas.get(tam / 2 - 1)
                    .add(ordenadas.get(tam / 2))
                    .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        } else {
            mediana = ordenadas.get(tam / 2).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal maxima = ordenadas.get(tam - 1);
        BigDecimal minima = ordenadas.get(0);

        long aprobados = notas.stream().filter(n -> n.compareTo(NOTA_APROBACION) >= 0).count();
        long reprobados = notas.size() - aprobados;

        return new EstadisticasGrupo(promedio, mediana, maxima, minima, aprobados, reprobados, notas.size());
    }

    public static Map<String, Long> distribucionPorRango(List<BigDecimal> notas) {
        Objects.requireNonNull(notas);
        Map<String, Long> distribucion = new LinkedHashMap<>();
        distribucion.put("0-59", 0L);
        distribucion.put("60-69", 0L);
        distribucion.put("70-79", 0L);
        distribucion.put("80-89", 0L);
        distribucion.put("90-100", 0L);
        for (BigDecimal n : notas) {
            int v = n.intValue();
            if (v < 60)       distribucion.merge("0-59",   1L, Long::sum);
            else if (v < 70)  distribucion.merge("60-69",  1L, Long::sum);
            else if (v < 80)  distribucion.merge("70-79",  1L, Long::sum);
            else if (v < 90)  distribucion.merge("80-89",  1L, Long::sum);
            else               distribucion.merge("90-100", 1L, Long::sum);
        }
        return distribucion;
    }
}
