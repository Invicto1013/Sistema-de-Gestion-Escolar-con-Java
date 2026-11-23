package com.unev.sige.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Gestiona el inventario de bienes, mobiliario y suministros
 * escolares del plantel en el SIGE.
 */
public final class GestorInventarioEscolar {

    public enum CategoriaArticulo {
        MOBILIARIO, EQUIPO_TECNOLOGICO, MATERIAL_DIDACTICO,
        SUMINISTRO_OFICINA, HERRAMIENTA, OTRO
    }

    public enum EstadoArticulo { NUEVO, BUENO, REGULAR, DETERIORADO, DADO_DE_BAJA }

    public record ArticuloInventario(
            String codigoArticulo,
            String descripcion,
            CategoriaArticulo categoria,
            int cantidadExistente,
            BigDecimal valorUnitario,
            EstadoArticulo estado
    ) {
        public ArticuloInventario {
            if (codigoArticulo == null || !codigoArticulo.startsWith("INV-")) {
                throw new IllegalArgumentException("Código de artículo debe iniciar con INV-");
            }
            if (cantidadExistente < 0) throw new IllegalArgumentException("Cantidad no puede ser negativa");
            if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Valor unitario inválido");
            }
        }

        public BigDecimal valorTotal() {
            return valorUnitario.multiply(new BigDecimal(cantidadExistente))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    private final Map<String, ArticuloInventario> inventario = new LinkedHashMap<>();

    public void registrarArticulo(ArticuloInventario articulo) {
        Objects.requireNonNull(articulo);
        if (inventario.containsKey(articulo.codigoArticulo())) {
            throw new IllegalStateException("Ya existe artículo con código: " + articulo.codigoArticulo());
        }
        inventario.put(articulo.codigoArticulo(), articulo);
    }

    public Optional<ArticuloInventario> buscarPorCodigo(String codigo) {
        return Optional.ofNullable(inventario.get(codigo));
    }

    public List<ArticuloInventario> filtrarPorCategoria(CategoriaArticulo categoria) {
        Objects.requireNonNull(categoria);
        return inventario.values().stream()
                .filter(a -> a.categoria() == categoria)
                .toList();
    }

    public List<ArticuloInventario> obtenerArticulosDeterioro() {
        return inventario.values().stream()
                .filter(a -> a.estado() == EstadoArticulo.DETERIORADO
                          || a.estado() == EstadoArticulo.DADO_DE_BAJA)
                .toList();
    }

    public BigDecimal calcularValorTotalInventario() {
        return inventario.values().stream()
                .map(ArticuloInventario::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public int totalArticulos() { return inventario.size(); }
    public Map<String, ArticuloInventario> getInventario() { return Collections.unmodifiableMap(inventario); }
}
