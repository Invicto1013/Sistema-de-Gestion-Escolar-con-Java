package com.unev.sige.web;

import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.Rol;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HTML de las herramientas escolares (índice por rol y formulario). Sin estado: se prueba sin servidor. */
final class HerramientasWeb {

    /** Prefijo de los campos del formulario, para no chocar con los nombres de otras pantallas. */
    static final String PREFIJO = "f_";

    private HerramientasWeb() { }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Pantalla de entrada: las herramientas del rol agrupadas por tema. */
    static String indice(Rol rol) {
        List<HerramientaEscolar> visibles = CatalogoHerramientas.paraRol(rol);
        StringBuilder b = new StringBuilder("<section class='card'><div class='eyebrow'>HERRAMIENTAS</div><h1>Herramientas escolares</h1>"
                + "<p>Calculadoras y validaciones de las reglas del centro para tu rol (" + esc(rol == null ? "" : rol.etiqueta())
                + "). Los resultados son de consulta: no se guarda nada.</p></section>");
        Map<String, List<HerramientaEscolar>> porGrupo = new LinkedHashMap<>();
        for (HerramientaEscolar h : visibles) porGrupo.computeIfAbsent(h.grupo(), k -> new java.util.ArrayList<>()).add(h);
        for (Map.Entry<String, List<HerramientaEscolar>> g : porGrupo.entrySet()) {
            b.append("<section class='card'><h2>").append(esc(g.getKey())).append("</h2><ul class='herramientas'>");
            for (HerramientaEscolar h : g.getValue()) {
                b.append("<li><a href='/herramienta?id=").append(esc(h.id())).append("'><strong>").append(esc(h.titulo()))
                        .append("</strong></a> — ").append(esc(h.descripcion())).append("</li>");
            }
            b.append("</ul></section>");
        }
        if (visibles.isEmpty()) b.append("<section class='card'><p>No hay herramientas disponibles para tu rol.</p></section>");
        return b.toString();
    }

    /** Formulario de una herramienta; {@code valores} y {@code resultado} pueden ser null (primera visita). */
    static String formulario(HerramientaEscolar h, String csrf, Map<String, String> valores, HerramientaEscolar.Resultado resultado) {
        Map<String, String> v = valores == null ? h.valoresIniciales() : valores;
        StringBuilder b = new StringBuilder("<section class='card'><div class='eyebrow'>")
                .append(esc(h.grupo().toUpperCase(java.util.Locale.ROOT))).append("</div><h1>").append(esc(h.titulo())).append("</h1><p>")
                .append(esc(h.descripcion())).append("</p><form method='post' action='/herramienta'><input type='hidden' name='_csrf' value='")
                .append(esc(csrf)).append("'><input type='hidden' name='id' value='").append(esc(h.id())).append("'>");
        for (HerramientaEscolar.Campo c : h.campos()) {
            String nombre = PREFIJO + c.nombre();
            String valor = v.getOrDefault(c.nombre(), c.valorInicial());
            b.append("<label>").append(esc(c.etiqueta())).append(control(c, nombre, valor)).append("</label>");
        }
        b.append("<button class='primary' type='submit'>Calcular</button> <a href='/herramientas'>Volver a herramientas</a></form></section>");
        if (resultado != null) {
            b.append("<section class='card' role='status' aria-live='polite'><h2>")
                    .append(resultado.exito() ? "Resultado" : "Revisa los datos").append("</h2><p>")
                    .append(esc(resultado.texto()).replace("\n", "<br>")).append("</p></section>");
        }
        return b.toString();
    }

    private static String control(HerramientaEscolar.Campo c, String nombre, String valor) {
        String n = esc(nombre), val = esc(valor);
        return switch (c.tipo()) {
            case LISTA -> "<textarea name='" + n + "' rows='4' maxlength='1500'>" + val + "</textarea>";
            case SI_NO -> "<select name='" + n + "'>" + opcion("si", "Sí", valor) + opcion("no", "No", valor) + "</select>";
            case OPCION -> {
                StringBuilder s = new StringBuilder("<select name='" + n + "'>");
                for (String o : c.opciones()) s.append(opcion(o, o.replace('_', ' '), valor));
                yield s.append("</select>").toString();
            }
            case FECHA -> "<input name='" + n + "' type='date' value='" + val + "' required>";
            case HORA -> "<input name='" + n + "' type='time' value='" + val + "' required>";
            case ENTERO -> "<input name='" + n + "' type='number' step='1' value='" + val + "' required>";
            case NUMERO -> "<input name='" + n + "' type='text' inputmode='decimal' value='" + val + "' required>";
            default -> "<input name='" + n + "' type='text' maxlength='120' value='" + val + "'>";
        };
    }

    private static String opcion(String valor, String etiqueta, String actual) {
        return "<option value='" + esc(valor) + "'" + (valor.equalsIgnoreCase(actual == null ? "" : actual.trim()) ? " selected" : "") + ">" + esc(etiqueta) + "</option>";
    }

    /** Extrae del formulario enviado solo los campos de la herramienta (quita el prefijo). */
    static Map<String, String> valoresDe(HerramientaEscolar h, Map<String, String> form) {
        Map<String, String> r = new LinkedHashMap<>();
        for (HerramientaEscolar.Campo c : h.campos()) {
            String v = form.get(PREFIJO + c.nombre());
            r.put(c.nombre(), v == null ? "" : v);
        }
        return r;
    }
}
