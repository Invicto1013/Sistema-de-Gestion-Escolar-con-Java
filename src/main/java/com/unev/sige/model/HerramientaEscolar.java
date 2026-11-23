package com.unev.sige.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Una herramienta escolar del catálogo: reglas de negocio (calculadoras y validadores) expuestas con un
 * formulario simple y un resultado en texto. No guarda nada: es la misma lógica para la web y el escritorio.
 */
public record HerramientaEscolar(String id, String grupo, String titulo, String descripcion, Set<Rol> roles,
                                 List<Campo> campos, Function<Entrada, String> calcular) {

    public enum Tipo { TEXTO, NUMERO, ENTERO, FECHA, HORA, SI_NO, OPCION, LISTA }

    /** Campo del formulario. Para OPCION, {@code opciones} lista los valores; el primero es el predeterminado. */
    public record Campo(String nombre, String etiqueta, Tipo tipo, String valorInicial, List<String> opciones) {
        public static Campo texto(String n, String e, String v) { return new Campo(n, e, Tipo.TEXTO, v, List.of()); }
        public static Campo num(String n, String e, String v) { return new Campo(n, e, Tipo.NUMERO, v, List.of()); }
        public static Campo ent(String n, String e, String v) { return new Campo(n, e, Tipo.ENTERO, v, List.of()); }
        public static Campo fecha(String n, String e, String v) { return new Campo(n, e, Tipo.FECHA, v, List.of()); }
        public static Campo hora(String n, String e, String v) { return new Campo(n, e, Tipo.HORA, v, List.of()); }
        public static Campo sn(String n, String e, boolean v) { return new Campo(n, e, Tipo.SI_NO, v ? "si" : "no", List.of()); }
        public static Campo lista(String n, String e, String v) { return new Campo(n, e, Tipo.LISTA, v, List.of()); }
        public static Campo op(String n, String e, String... opciones) {
            return new Campo(n, e, Tipo.OPCION, opciones[0], List.of(opciones));
        }
        public static Campo op(String n, String e, Class<? extends Enum<?>> enumeracion) {
            return op(n, e, Arrays.stream(enumeracion.getEnumConstants()).map(Enum::name).toArray(String[]::new));
        }
    }

    /** Resultado de ejecutar una herramienta. {@code exito=false} significa que los datos no se pudieron interpretar. */
    public record Resultado(boolean exito, String texto) { }

    /** Lectura tipada de los valores del formulario, con mensajes en español cuando algo no se entiende. */
    public static final class Entrada {
        private final Map<String, String> valores;

        public Entrada(Map<String, String> valores) { this.valores = valores; }

        public String txt(String n) {
            String v = valores.get(n);
            return v == null ? "" : v.trim();
        }

        public double dbl(String n) {
            try {
                double d = Double.parseDouble(txt(n).replace(',', '.'));
                if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
                return d;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("«" + n + "» debe ser un número.");
            }
        }

        public BigDecimal dec(String n) {
            try {
                return new BigDecimal(txt(n).replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("«" + n + "» debe ser un número.");
            }
        }

        public int ent(String n) {
            try {
                return Integer.parseInt(txt(n));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("«" + n + "» debe ser un número entero.");
            }
        }

        public boolean sn(String n) {
            String v = txt(n).toLowerCase(java.util.Locale.ROOT);
            return v.equals("si") || v.equals("sí") || v.equals("on") || v.equals("true") || v.equals("1");
        }

        public LocalDate fecha(String n) {
            try {
                return LocalDate.parse(txt(n));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("«" + n + "» debe ser una fecha (AAAA-MM-DD).");
            }
        }

        public LocalTime hora(String n) {
            try {
                return LocalTime.parse(txt(n));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("«" + n + "» debe ser una hora (HH:MM).");
            }
        }

        public <E extends Enum<E>> E enumerado(Class<E> tipo, String n) {
            try {
                return Enum.valueOf(tipo, txt(n));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Opción no válida en «" + n + "».");
            }
        }

        /** Elementos separados por coma, punto y coma o salto de línea; descarta los vacíos. */
        public List<String> items(String n) {
            List<String> r = new ArrayList<>();
            for (String parte : txt(n).split("[,;\\n\\r]+")) {
                if (!parte.isBlank()) r.add(parte.trim());
            }
            return r;
        }

        /** Líneas no vacías (para listas donde cada línea tiene varios datos separados por «;»). */
        public List<String> lineas(String n) {
            List<String> r = new ArrayList<>();
            for (String linea : txt(n).split("\\r?\\n")) {
                if (!linea.isBlank()) r.add(linea.trim());
            }
            return r;
        }
    }

    public boolean visiblePara(Rol rol) {
        return rol != null && (rol == Rol.ADMIN || roles.contains(rol));
    }

    /** Valores iniciales de todos los campos (útil para el formulario y para pruebas). */
    public Map<String, String> valoresIniciales() {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        for (Campo c : campos) m.put(c.nombre(), c.valorInicial());
        return m;
    }

    public Resultado ejecutar(Map<String, String> valores) {
        try {
            return new Resultado(true, calcular.apply(new Entrada(valores)));
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException | ArithmeticException e) {
            String msg = e.getMessage() == null ? "datos incompletos" : e.getMessage();
            return new Resultado(false, "No se pudo calcular: " + msg);
        }
    }

    static Set<Rol> roles(Rol... r) {
        return r.length == 0 ? EnumSet.noneOf(Rol.class) : EnumSet.copyOf(Arrays.asList(r));
    }
}
