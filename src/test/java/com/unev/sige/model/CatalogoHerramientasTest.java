package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogoHerramientasTest {

    @Test
    void elCatalogoTieneLas36HerramientasConIdentificadoresUnicos() {
        Set<String> ids = new HashSet<>();
        CatalogoHerramientas.todas().forEach(h -> assertTrue(ids.add(h.id()), "id repetido: " + h.id()));
        assertEquals(36, ids.size());
    }

    @Test
    void todasFuncionanConSusValoresIniciales() {
        for (HerramientaEscolar h : CatalogoHerramientas.todas()) {
            HerramientaEscolar.Resultado r = h.ejecutar(h.valoresIniciales());
            assertTrue(r.exito(), h.id() + " -> " + r.texto());
            assertFalse(r.texto().isBlank(), h.id());
        }
    }

    @Test
    void cadaHerramientaTieneAlMenosUnRolYElAdminLasVeTodas() {
        for (HerramientaEscolar h : CatalogoHerramientas.todas()) {
            assertFalse(h.roles().isEmpty(), h.id());
            assertTrue(h.visiblePara(Rol.ADMIN), h.id());
            assertFalse(h.campos().isEmpty(), h.id());
        }
        assertEquals(36, CatalogoHerramientas.paraRol(Rol.ADMIN).size());
    }

    @Test
    void cadaRolSoloVeLoQueLeCorresponde() {
        assertTrue(CatalogoHerramientas.buscar("evaluacion-docente").orElseThrow().visiblePara(Rol.DIRECTOR));
        assertFalse(CatalogoHerramientas.buscar("evaluacion-docente").orElseThrow().visiblePara(Rol.ESTUDIANTE));
        assertFalse(CatalogoHerramientas.buscar("evaluacion-docente").orElseThrow().visiblePara(Rol.PADRE));
        assertTrue(CatalogoHerramientas.buscar("excusa").orElseThrow().visiblePara(Rol.PADRE));
        assertFalse(CatalogoHerramientas.buscar("inventario").orElseThrow().visiblePara(Rol.ESTUDIANTE));
        assertFalse(CatalogoHerramientas.buscar("riesgo-desercion").orElseThrow().visiblePara(Rol.PADRE));
        assertFalse(CatalogoHerramientas.buscar("carga-docente").orElseThrow().visiblePara(Rol.PROFESOR));
        assertFalse(CatalogoHerramientas.buscar("x").isPresent());
        for (Rol r : Rol.values()) {
            assertFalse(CatalogoHerramientas.paraRol(r).isEmpty(), r.name());
        }
        assertFalse(CatalogoHerramientas.paraRol(null).iterator().hasNext());
    }

    @Test
    void losDatosNoNumericosDanUnMensajeYNoUnaExcepcion() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("riesgo-desercion").orElseThrow();
        Map<String, String> v = new LinkedHashMap<>(h.valoresIniciales());
        v.put("inasistencia", "mucho");
        HerramientaEscolar.Resultado r = h.ejecutar(v);
        assertFalse(r.exito());
        assertTrue(r.texto().startsWith("No se pudo calcular"));
        v.put("inasistencia", "NaN");
        assertFalse(h.ejecutar(v).exito());
    }

    @Test
    void elRiesgoAltoSeDetecta() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("riesgo-desercion").orElseThrow();
        Map<String, String> v = new LinkedHashMap<>(h.valoresIniciales());
        v.put("inasistencia", "60");
        v.put("promedio", "40");
        v.put("faltas", "3");
        assertTrue(h.ejecutar(v).texto().contains("Alto"));
    }

    @Test
    void elComedorDetectaElAlergenoDentroDeUnIngredienteCompuesto() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("comedor").orElseThrow();
        Map<String, String> v = new LinkedHashMap<>(h.valoresIniciales());
        v.put("ingredientes", "ARROZ, SALSA_CON_MANI");
        assertTrue(h.ejecutar(v).texto().contains("Menú apto: No"));
        assertTrue(h.ejecutar(h.valoresIniciales()).texto().contains("Menú apto: Sí"));
    }

    @Test
    void elCalendarioDescuentaLosFeriados() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("calendario").orElseThrow();
        Map<String, String> v = new LinkedHashMap<>(h.valoresIniciales());
        v.put("anio", "2026");
        v.put("desde", "2026-11-02");
        v.put("hasta", "2026-11-06");
        v.put("feriados", "");
        assertTrue(h.ejecutar(v).texto().contains(": 5"));
        v.put("feriados", "2026-11-04");
        assertTrue(h.ejecutar(v).texto().contains(": 4"));
        v.put("feriados", "no-es-fecha");
        assertFalse(h.ejecutar(v).exito());
        v.put("feriados", "");
        v.put("desde", "2026-11-10");
        assertFalse(h.ejecutar(v).exito());
    }

    @Test
    void lasListasConFormatoIncorrectoSeReportan() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("creditos").orElseThrow();
        Map<String, String> v = new LinkedHashMap<>(h.valoresIniciales());
        v.put("asignaturas", "solo-dos;campos");
        assertFalse(h.ejecutar(v).exito());
        v.put("asignaturas", "A;B;tres;80");
        assertFalse(h.ejecutar(v).exito());
    }

    @Test
    void laReunionSinQuorumNoInicia() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("reunion-consejo").orElseThrow();
        Map<String, String> v = new LinkedHashMap<>(h.valoresIniciales());
        v.put("asistentes", "D-1");
        HerramientaEscolar.Resultado r = h.ejecutar(v);
        assertTrue(r.exito());
        assertTrue(r.texto().contains("no se cumple"));
        v.put("asistentes", "D-9");
        assertFalse(h.ejecutar(v).exito());
    }

    @Test
    void cadaHerramientaYGrupoTieneIconoYColor() {
        for (HerramientaEscolar h : CatalogoHerramientas.todas()) {
            assertFalse(IconosHerramientas.icono(h.id()).equals("🧰") && !h.id().isEmpty(), "sin icono propio: " + h.id());
            assertTrue(CatalogoHerramientas.GRUPOS.contains(h.grupo()), "grupo fuera del orden: " + h.grupo());
        }
        for (String g : CatalogoHerramientas.GRUPOS) {
            assertFalse(IconosHerramientas.iconoGrupo(g).equals("🧰"), g);
            assertTrue(IconosHerramientas.acento(g).matches("#[0-9a-f]{6}"), g);
        }
    }

    @Test
    void losGruposVanEnOrdenFijoYLasHerramientasEnOrdenAlfabetico() {
        Map<String, java.util.List<HerramientaEscolar>> m = CatalogoHerramientas.porGrupo(Rol.ADMIN);
        assertEquals(CatalogoHerramientas.GRUPOS, new java.util.ArrayList<>(m.keySet()));
        int total = 0;
        java.text.Collator c = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es"));
        for (java.util.List<HerramientaEscolar> lista : m.values()) {
            total += lista.size();
            for (int i = 1; i < lista.size(); i++) assertTrue(c.compare(lista.get(i - 1).titulo(), lista.get(i).titulo()) <= 0, lista.get(i).titulo());
        }
        assertEquals(36, total);
        assertFalse(CatalogoHerramientas.porGrupo(Rol.ESTUDIANTE).containsKey("Docentes"));
        for (Rol r : Rol.values()) assertFalse(CatalogoHerramientas.porGrupo(r).isEmpty(), r.name());
    }

    @Test
    void elSlugNoLlevaTildesNiEspacios() {
        assertEquals("academico", CatalogoHerramientas.slug("Académico"));
        assertEquals("bienestar-y-seguridad", CatalogoHerramientas.slug("Bienestar y seguridad"));
        assertEquals("documentos-y-codigos", CatalogoHerramientas.slug("Documentos y códigos"));
        assertEquals("", CatalogoHerramientas.slug(null));
    }
}
