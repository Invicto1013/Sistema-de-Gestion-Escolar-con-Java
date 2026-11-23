package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.Rol;
import java.util.HashMap;
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
}
