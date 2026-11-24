package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class CatalogoPlanesTest {

    @Test
    void hayTresPlanesUnicosOrdenadosPorPrecioYElInicialExiste() {
        var planes = CatalogoPlanes.todos();
        assertEquals(3, planes.size());
        assertEquals(3, new HashSet<>(planes.stream().map(PlanServicio::codigo).toList()).size());
        for (int i = 1; i < planes.size(); i++) assertTrue(planes.get(i).precioMensual() > planes.get(i - 1).precioMensual());
        assertTrue(CatalogoPlanes.buscar(CatalogoPlanes.PLAN_INICIAL).isPresent());
        assertTrue(CatalogoPlanes.buscar("estandar").isPresent(), "la búsqueda ignora mayúsculas");
        assertTrue(CatalogoPlanes.buscar("XYZ").isEmpty());
        assertTrue(CatalogoPlanes.buscar(null).isEmpty());
    }

    @Test
    void elUsoSeClasificaPorLimite() {
        PlanServicio p = CatalogoPlanes.buscar("ESENCIAL").orElseThrow();
        assertEquals("Dentro del límite", p.estadoUso(100));
        assertEquals("Cerca del límite", p.estadoUso(140));
        assertEquals("Excede el límite del plan", p.estadoUso(151));
        assertEquals(67, p.porcentajeUso(100));
        PlanServicio inst = CatalogoPlanes.buscar("INSTITUCIONAL").orElseThrow();
        assertTrue(inst.ilimitado());
        assertEquals("Sin límite de usuarios", inst.estadoUso(100000));
    }

    @Test
    void soloAdminCambiaYAdminDirectorVen() {
        assertTrue(CatalogoPlanes.puedeCambiar(Rol.ADMIN));
        assertFalse(CatalogoPlanes.puedeCambiar(Rol.DIRECTOR));
        assertTrue(CatalogoPlanes.puedeVer(Rol.DIRECTOR));
        assertFalse(CatalogoPlanes.puedeVer(Rol.PADRE));
        assertFalse(CatalogoPlanes.puedeVer(Rol.ESTUDIANTE));
    }

    @Test
    void elPrecioLlevaSeparadorDeMiles() {
        assertEquals("6,500", CatalogoPlanes.precio(6500));
    }
}
