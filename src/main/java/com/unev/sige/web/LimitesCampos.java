package com.unev.sige.web;

import java.util.Map;

/**
 * Longitud máxima (en caracteres) de cada campo de formulario. Se valida en el servidor porque
 * el atributo maxlength del HTML lo puede saltar cualquiera. Las cifras siguen las columnas de MySQL.
 */
final class LimitesCampos {
    /** Tamaño máximo de un formulario normal (sin archivos): 64 KB. */
    static final int MAX_BYTES_FORMULARIO = 64 * 1024;
    private static final int POR_DEFECTO = 255;

    private static final Map<String, Integer> MAXIMOS = Map.ofEntries(
            Map.entry("_csrf", 128), Map.entry("correo", 100), Map.entry("contrasena", 128),
            Map.entry("nueva", 128), Map.entry("actual", 128), Map.entry("codigo", 10),
            Map.entry("nombre", 50), Map.entry("apellido", 50), Map.entry("matricula", 20),
            Map.entry("grado", 50), Map.entry("telefono", 20), Map.entry("ocupacion", 100),
            Map.entry("especialidad", 50), Map.entry("codigoProfesor", 20), Map.entry("aula", 20),
            Map.entry("horario", 50), Map.entry("titulo", 120), Map.entry("descripcion", 5000),
            Map.entry("parentesco", 50), Map.entry("periodo", 20), Map.entry("q", 100),
            Map.entry("detalle", 255), Map.entry("grados", 500));

    private static final int NUMERICO = 20;
    private static final int FECHA = 32;
    private static final int ETIQUETA = 30;
    private static final Map<String, Integer> CORTOS = Map.ofEntries(
            Map.entry("id", NUMERICO), Map.entry("estudiante", NUMERICO), Map.entry("padre", NUMERICO),
            Map.entry("seccion", NUMERICO), Map.entry("profesor", NUMERICO), Map.entry("asignatura", NUMERICO),
            Map.entry("area", NUMERICO), Map.entry("cupo", NUMERICO), Map.entry("creditos", NUMERICO),
            Map.entry("id_seccion", NUMERICO), Map.entry("material", NUMERICO),
            Map.entry("fecha", FECHA), Map.entry("desde", FECHA), Map.entry("hasta", FECHA),
            Map.entry("fecha_limite", FECHA), Map.entry("estado", ETIQUETA), Map.entry("rol", ETIQUETA),
            Map.entry("tipo", ETIQUETA), Map.entry("accion", ETIQUETA), Map.entry("confirmar", ETIQUETA),
            Map.entry("activo", ETIQUETA));

    private LimitesCampos() { }

    /** Máximo permitido para un campo en una ruta. El nombre de una asignatura admite 100; el de una persona, 50. */
    static int maximo(String ruta, String campo) {
        if ("nombre".equals(campo) && "/guardar-asignatura".equals(ruta)) return 100;
        Integer largo = MAXIMOS.get(campo);
        if (largo != null) return largo;
        Integer corto = CORTOS.get(campo);
        return corto != null ? corto : POR_DEFECTO;
    }

    /** Devuelve un mensaje para el usuario si algún campo supera su máximo; null si todo es válido. */
    static String validar(String ruta, Map<String, String> valores) {
        for (Map.Entry<String, String> e : valores.entrySet()) {
            String valor = e.getValue();
            if (valor == null) continue;
            int max = maximo(ruta, e.getKey());
            if (valor.length() > max && valor.codePointCount(0, valor.length()) > max) {
                return "El campo «" + etiqueta(e.getKey()) + "» es demasiado largo (máximo " + max + " caracteres).";
            }
        }
        return null;
    }

    private static String etiqueta(String campo) {
        String limpio = campo.replace("_", " ");
        return limpio.length() > 40 ? limpio.substring(0, 40) : limpio;
    }
}
