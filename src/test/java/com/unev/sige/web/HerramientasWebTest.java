package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.RegistroHerramienta;
import com.unev.sige.model.Rol;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HerramientasWebTest {

    @Test
    void elIndiceSoloEnlazaLasHerramientasDelRol() {
        String estudiante = HerramientasWeb.indice(Rol.ESTUDIANTE);
        assertTrue(estudiante.contains("/herramienta?id=excusa"));
        assertFalse(estudiante.contains("/herramienta?id=evaluacion-docente"));
        assertFalse(estudiante.contains("/herramienta?id=inventario"));
        String director = HerramientasWeb.indice(Rol.DIRECTOR);
        assertTrue(director.contains("/herramienta?id=evaluacion-docente"));
        String admin = HerramientasWeb.indice(Rol.ADMIN);
        for (HerramientaEscolar h : CatalogoHerramientas.todas()) assertTrue(admin.contains("id=" + h.id()), h.id());
    }

    @Test
    void elFormularioLlevaCsrfIdentificadorYCamposConPrefijo() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("riesgo-desercion").orElseThrow();
        String html = HerramientasWeb.formulario(h, "TOKEN123", null, null);
        assertTrue(html.contains("name='_csrf' value='TOKEN123'"));
        assertTrue(html.contains("name='id' value='riesgo-desercion'"));
        assertTrue(html.contains("name='f_inasistencia'"));
        assertTrue(html.contains("action='/herramienta'"));
        assertFalse(html.contains("<script"));
    }

    @Test
    void losValoresYElResultadoSeEscapan() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("prestamo-dispositivo").orElseThrow();
        Map<String, String> v = new HashMap<>(h.valoresIniciales());
        v.put("esperada", "'><script>alert(1)</script>");
        String html = HerramientasWeb.formulario(h, "t", v, new HerramientaEscolar.Resultado(false, "<img src=x onerror=alert(1)>\nlinea"));
        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<img"));
        assertTrue(html.contains("&lt;img"));
        assertTrue(html.contains("<br>"));
        assertTrue(html.contains("Revisa los datos"));
    }

    @Test
    void elResultadoExitosoSeMuestra() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("aptitud-fisica").orElseThrow();
        String html = HerramientasWeb.formulario(h, "t", h.valoresIniciales(), h.ejecutar(h.valoresIniciales()));
        assertTrue(html.contains("Desempeño:"));
        assertTrue(html.contains("role='status'"));
    }

    @Test
    void sinSeleccionActualElSiNoRespetaElValor() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("comedor").orElseThrow();
        Map<String, String> v = new HashMap<>(h.valoresIniciales());
        v.put("supervision", "no");
        String html = HerramientasWeb.formulario(h, "t", v, null);
        assertTrue(html.contains("<option value='no' selected>"));
    }

    @Test
    void soloSeLeenLosCamposDeLaHerramienta() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("club").orElseThrow();
        Map<String, String> form = new HashMap<>();
        form.put("f_clubes", "2");
        form.put("f_horas", "4");
        form.put("f_otro", "x");
        form.put("rol", "ADMIN");
        Map<String, String> v = HerramientasWeb.valoresDe(h, form);
        assertEquals(2, v.size());
        assertEquals("2", v.get("clubes"));
        assertTrue(h.ejecutar(v).exito());
    }

    private static RegistroHerramienta registro(int id, int dueno, String titulo, String resultado) {
        return new RegistroHerramienta(id, dueno, "Ana <b>Gómez</b>", "excusa", titulo, resultado,
                LocalDateTime.of(2026, 11, 24, 9, 30), List.of(new RegistroHerramienta.Dato("tipo", "Tipo", "MEDICA\n<i>x</i>")));
    }

    @Test
    void elFormularioOfreceCalcularYGuardar() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("excusa").orElseThrow();
        String html = HerramientasWeb.formulario(h, "t", null, null, HerramientasWeb.avisoGuardado(15));
        assertTrue(html.contains("name='accion' value='guardar'"));
        assertTrue(html.contains("name='accion' value='calcular'"));
        assertTrue(html.contains("/herramientas-registro?id=15"));
        assertTrue(html.contains("/herramientas-registro.pdf?id=15"));
        assertTrue(html.contains("/herramientas-registros"));
    }

    @Test
    void elListadoEscapaMuestraElAutorSoloAQuienVeTodosYLimitaElBorrado() {
        List<RegistroHerramienta> lista = List.of(registro(1, 10, "Excusa <script>", "Excusa válida: Sí\nlínea 2"), registro(2, 11, "Otra", "ok"));
        String propio = HerramientasWeb.listado(lista, false, "TOK", 10, Rol.ESTUDIANTE, "");
        assertFalse(propio.contains("<script>"));
        assertFalse(propio.contains("<th>Autor</th>"));
        assertTrue(propio.contains("Excusa válida: Sí"));
        assertFalse(propio.contains("línea 2"));
        assertTrue(propio.contains("name='id' value='1'"));
        assertFalse(propio.contains("name='id' value='2'"));
        String director = HerramientasWeb.listado(lista, true, "TOK", 5, Rol.DIRECTOR, "");
        assertTrue(director.contains("<th>Autor</th>"));
        assertFalse(director.contains("<b>Gómez</b>"));
        assertFalse(director.contains("action='/herramientas-registro-eliminar'"));
        String admin = HerramientasWeb.listado(lista, true, "TOK", 5, Rol.ADMIN, "Registro eliminado.");
        assertTrue(admin.contains("name='id' value='2'"));
        assertTrue(admin.contains("Registro eliminado."));
        assertTrue(admin.contains("/herramientas-registros.pdf"));
    }

    @Test
    void elListadoVacioDaUnaPista() {
        String html = HerramientasWeb.listado(List.of(), false, "t", 1, Rol.ESTUDIANTE, "");
        assertTrue(html.contains("Todavía no hay registros"));
        assertFalse(html.contains("<table"));
    }

    @Test
    void elDetalleEscapaYOfrecePdfYEliminarSoloSiCorresponde() {
        RegistroHerramienta r = registro(9, 10, "Excusa", "Resultado\nsegunda línea");
        String conBorrar = HerramientasWeb.detalle(r, "TOK", true);
        assertFalse(conBorrar.contains("<i>x</i>"));
        assertTrue(conBorrar.contains("&lt;i&gt;x&lt;/i&gt;"));
        assertTrue(conBorrar.contains("/herramientas-registro.pdf?id=9"));
        assertTrue(conBorrar.contains("Resultado<br>segunda línea"));
        assertTrue(conBorrar.contains("action='/herramientas-registro-eliminar'"));
        assertFalse(HerramientasWeb.detalle(r, "TOK", false).contains("herramientas-registro-eliminar"));
    }
}
