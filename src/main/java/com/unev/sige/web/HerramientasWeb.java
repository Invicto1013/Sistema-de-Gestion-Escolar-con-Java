package com.unev.sige.web;

import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.IconosHerramientas;
import com.unev.sige.model.RegistroHerramienta;
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

    /** Pantalla de entrada con los grupos plegables abiertos según {@code abrir}: null (el primero), «todos», «ninguno» o el slug de un grupo. */
    static String indice(Rol rol) {
        return indice(rol, null);
    }

    static String indice(Rol rol, String abrir) {
        Map<String, List<HerramientaEscolar>> grupos = CatalogoHerramientas.porGrupo(rol);
        int total = grupos.values().stream().mapToInt(List::size).sum();
        StringBuilder b = new StringBuilder("<section class='card herr-hero'><span class='herr-hero-icono' aria-hidden='true'>🧰</span><div class='herr-hero-texto'>"
                + "<div class='eyebrow'>HERRAMIENTAS</div><h1>Herramientas escolares</h1>"
                + "<p>Calculadoras y validaciones de las reglas del centro para tu rol (" + esc(rol == null ? "" : rol.etiqueta())
                + "). «Calcular» solo consulta; «Calcular y guardar» guarda el registro para descargarlo en PDF.</p>"
                + "<p class='herr-acciones'><a href='/herramientas?abrir=todos'>Expandir todo</a><a href='/herramientas?abrir=ninguno'>Contraer todo</a>"
                + "<a href='/herramientas-registros'>💾 Mis registros</a></p></div>"
                + "<span class='herr-contador'><strong>" + total + "</strong> herramientas<br>" + grupos.size() + " grupos</span></section>");
        boolean primero = true;
        int fila = 0;
        for (Map.Entry<String, List<HerramientaEscolar>> g : grupos.entrySet()) {
            String slug = CatalogoHerramientas.slug(g.getKey());
            boolean abierto = "todos".equals(abrir) || slug.equals(abrir) || (abrir == null && primero);
            primero = false;
            b.append("<details class='herr-grupo' id='").append(slug).append("' style='--acento:").append(IconosHerramientas.acento(g.getKey()))
                    .append("'").append(abierto ? " open" : "").append("><summary><span class='herr-icono' aria-hidden='true'>")
                    .append(IconosHerramientas.iconoGrupo(g.getKey())).append("</span><span class='herr-grupo-titulo'>").append(esc(g.getKey()))
                    .append("</span><span class='herr-cuenta'>").append(g.getValue().size()).append("</span></summary><div class='herr-lista'>");
            int i = 0;
            for (HerramientaEscolar h : g.getValue()) {
                b.append("<a class='herr-tarjeta' style='--i:").append(i++).append("' href='/herramienta?id=").append(esc(h.id())).append("'>")
                        .append("<span class='herr-icono' aria-hidden='true'>").append(IconosHerramientas.icono(h.id())).append("</span>")
                        .append("<span class='herr-texto'><strong>").append(esc(h.titulo())).append("</strong><small>").append(esc(h.descripcion()))
                        .append("</small></span></a>");
            }
            b.append("</div></details>");
            fila++;
        }
        if (fila == 0) b.append("<section class='card'><p>No hay herramientas disponibles para tu rol.</p></section>");
        return b.toString();
    }

    /** Formulario de una herramienta; {@code valores} y {@code resultado} pueden ser null (primera visita). */
    static String formulario(HerramientaEscolar h, String csrf, Map<String, String> valores, HerramientaEscolar.Resultado resultado) {
        return formulario(h, csrf, valores, resultado, "");
    }

    /** Igual que el anterior, con un aviso ya escapado (por ejemplo «Registro guardado») bajo el formulario. */
    static String formulario(HerramientaEscolar h, String csrf, Map<String, String> valores, HerramientaEscolar.Resultado resultado, String avisoHtml) {
        Map<String, String> v = valores == null ? h.valoresIniciales() : valores;
        String slug = CatalogoHerramientas.slug(h.grupo());
        StringBuilder b = new StringBuilder("<section class='card herr-form' style='--acento:").append(IconosHerramientas.acento(h.grupo()))
                .append("'><div class='herr-form-cab'><span class='herr-icono herr-icono-grande' aria-hidden='true'>").append(IconosHerramientas.icono(h.id()))
                .append("</span><div><div class='eyebrow'>").append(esc(IconosHerramientas.iconoGrupo(h.grupo()) + " " + h.grupo().toUpperCase(java.util.Locale.ROOT)))
                .append("</div><h1>").append(esc(h.titulo())).append("</h1><p>").append(esc(h.descripcion()))
                .append("</p></div></div><form method='post' action='/herramienta'><input type='hidden' name='_csrf' value='")
                .append(esc(csrf)).append("'><input type='hidden' name='id' value='").append(esc(h.id())).append("'>");
        for (HerramientaEscolar.Campo c : h.campos()) {
            String nombre = PREFIJO + c.nombre();
            String valor = v.getOrDefault(c.nombre(), c.valorInicial());
            b.append("<label>").append(esc(c.etiqueta())).append(control(c, nombre, valor)).append("</label>");
        }
        b.append("<div class='herr-acciones'><button class='primary' type='submit' name='accion' value='calcular'>Calcular</button>"
                + "<button class='herr-sec' type='submit' name='accion' value='guardar'>💾 Calcular y guardar</button></div>"
                + "<p class='herr-enlaces'><a href='/herramientas-registros'>💾 Mis registros</a>"
                + "<a href='/herramientas?abrir=" + slug + "#" + slug + "'>← Volver a herramientas</a></p></form></section>");
        if (avisoHtml != null && !avisoHtml.isEmpty()) b.append("<section class='card herr-aviso' role='status'>").append(avisoHtml).append("</section>");
        if (resultado != null) {
            b.append("<section class='card herr-resultado" + (resultado.exito() ? "" : " herr-error") + "' role='status' aria-live='polite'><h2>")
                    .append(resultado.exito() ? "Resultado" : "Revisa los datos").append("</h2><p>")
                    .append(esc(resultado.texto()).replace("\n", "<br>")).append("</p></section>");
        }
        return b.toString();
    }

    /** Aviso de que el cálculo se guardó, con enlaces al detalle y al PDF. */
    static String avisoGuardado(int idRegistro) {
        return "Registro N.º " + idRegistro + " guardado. <a href='/herramientas-registro?id=" + idRegistro + "'>Ver registro</a> · "
                + "<a href='/herramientas-registro.pdf?id=" + idRegistro + "'>Descargar PDF</a>";
    }

    private static final java.time.format.DateTimeFormatter FECHA = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static String fecha(RegistroHerramienta r) {
        return r.creado() == null ? "" : r.creado().format(FECHA);
    }

    /** Listado de registros. {@code verAutor} se activa para administración y dirección, que ven los de todos. */
    static String listado(List<RegistroHerramienta> registros, boolean verAutor, String csrf, int idActual, Rol rol, String mensaje) {
        StringBuilder b = new StringBuilder("<section class='card'><div class='eyebrow'>HERRAMIENTAS</div><h1>Registros guardados</h1><p>"
                + (verAutor ? "Como " + esc(rol.etiqueta()) + " ves los registros de todos los usuarios." : "Estos son los cálculos que guardaste.")
                + " <a href='/herramientas'>Volver a herramientas</a></p>");
        if (mensaje != null && !mensaje.isEmpty()) b.append("<p role='status'>").append(esc(mensaje)).append("</p>");
        if (registros.isEmpty()) {
            return b.append("<p>Todavía no hay registros. Usa «Calcular y guardar» en cualquier herramienta.</p></section>").toString();
        }
        b.append("<p><a class='button' href='/herramientas-registros.pdf'>Descargar listado en PDF</a></p><div class='table-wrap'><table><thead><tr><th>Fecha</th><th>Herramienta</th>")
                .append(verAutor ? "<th>Autor</th>" : "").append("<th>Resultado</th><th>Acciones</th></tr></thead><tbody>");
        for (RegistroHerramienta r : registros) {
            String primera = r.resultado() == null ? "" : r.resultado().split("\r?\n", 2)[0];
            b.append("<tr><td>").append(esc(fecha(r))).append("</td><td>").append(esc(r.titulo())).append("</td>")
                    .append(verAutor ? "<td>" + esc(r.autor()) + "</td>" : "")
                    .append("<td>").append(esc(primera)).append("</td><td><a href='/herramientas-registro?id=").append(r.id()).append("'>Ver</a> · <a href='/herramientas-registro.pdf?id=")
                    .append(r.id()).append("'>PDF</a>");
            if (r.puedeEliminar(idActual, rol)) {
                b.append(" · <form method='post' action='/herramientas-registro-eliminar' style='display:inline'><input type='hidden' name='_csrf' value='")
                        .append(esc(csrf)).append("'><input type='hidden' name='id' value='").append(r.id())
                        .append("'><button type='submit' class='danger'>Eliminar</button></form>");
            }
            b.append("</td></tr>");
        }
        return b.append("</tbody></table></div></section>").toString();
    }

    /** Detalle de un registro: datos ingresados y resultado. */
    static String detalle(RegistroHerramienta r, String csrf, boolean puedeEliminar) {
        StringBuilder b = new StringBuilder("<section class='card'><div class='eyebrow'>REGISTRO N.º " + r.id() + "</div><h1>" + esc(r.titulo()) + "</h1><p>"
                + esc(r.autor()) + " · " + esc(fecha(r)) + "</p><h2>Datos ingresados</h2><dl>");
        for (RegistroHerramienta.Dato d : r.datos()) {
            b.append("<dt>").append(esc(d.etiqueta())).append("</dt><dd>").append(esc(d.valor()).replace("\n", "<br>")).append("</dd>");
        }
        b.append("</dl><h2>Resultado</h2><p>").append(esc(r.resultado()).replace("\n", "<br>")).append("</p><p><a class='button' href='/herramientas-registro.pdf?id=")
                .append(r.id()).append("'>Descargar PDF</a> <a href='/herramientas-registros'>Volver a registros</a></p>");
        if (puedeEliminar) {
            b.append("<form method='post' action='/herramientas-registro-eliminar'><input type='hidden' name='_csrf' value='").append(esc(csrf))
                    .append("'><input type='hidden' name='id' value='").append(r.id()).append("'><button type='submit' class='danger'>Eliminar este registro</button></form>");
        }
        return b.append("</section>").toString();
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
