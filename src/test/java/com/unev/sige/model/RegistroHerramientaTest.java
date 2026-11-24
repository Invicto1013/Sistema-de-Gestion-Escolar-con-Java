package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RegistroHerramientaTest {

    private static RegistroHerramienta registro(int idUsuario) {
        return new RegistroHerramienta(7, idUsuario, "Ana Gómez", "riesgo-desercion", "Riesgo de deserción",
                "Índice de riesgo: 40", LocalDateTime.of(2026, 11, 24, 9, 30), List.of());
    }

    @Test
    void losDatosSeGuardanEnOrdenConSuEtiqueta() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("riesgo-desercion").orElseThrow();
        List<RegistroHerramienta.Dato> d = RegistroHerramienta.datosAGuardar(h, h.valoresIniciales());
        assertEquals(h.campos().size(), d.size());
        assertEquals(h.campos().get(0).nombre(), d.get(0).campo());
        assertEquals(h.campos().get(0).etiqueta(), d.get(0).etiqueta());
        assertEquals("25", d.get(0).valor());
    }

    @Test
    void losCamposSensiblesNuncaSeGuardan() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("retiro-estudiante").orElseThrow();
        for (RegistroHerramienta.Dato d : RegistroHerramienta.datosAGuardar(h, h.valoresIniciales())) {
            boolean sensible = d.campo().equals("pin") || d.campo().equals("pinIngresado") || d.campo().equals("cedula");
            assertEquals(sensible, d.valor().equals(RegistroHerramienta.NO_SE_GUARDA), d.campo());
        }
        assertTrue(CatalogoHerramientas.noSeGuarda("retiro-estudiante", "pin"));
        assertFalse(CatalogoHerramientas.noSeGuarda("retiro-estudiante", "nombre"));
        assertFalse(CatalogoHerramientas.noSeGuarda("excusa", "pin"));
    }

    @Test
    void losValoresLargosSeRecortan() {
        HerramientaEscolar h = CatalogoHerramientas.buscar("promedio-grupal").orElseThrow();
        String largo = "9,".repeat(2000);
        List<RegistroHerramienta.Dato> d = RegistroHerramienta.datosAGuardar(h, Map.of("notas", largo));
        assertEquals(RegistroHerramienta.MAX_VALOR, d.get(0).valor().length());
        assertEquals("", RegistroHerramienta.recortar(null, 10));
        assertEquals("abc", RegistroHerramienta.recortar("  abcdef ", 3));
    }

    @Test
    void verYEliminarSegunQuienEsYSuRol() {
        RegistroHerramienta r = registro(10);
        assertTrue(r.puedeVer(10, Rol.ESTUDIANTE));
        assertFalse(r.puedeVer(11, Rol.ESTUDIANTE));
        assertFalse(r.puedeVer(11, Rol.PADRE));
        assertTrue(r.puedeVer(11, Rol.DIRECTOR));
        assertTrue(r.puedeVer(11, Rol.ADMIN));
        assertTrue(r.puedeEliminar(10, Rol.ESTUDIANTE));
        assertFalse(r.puedeEliminar(11, Rol.DIRECTOR));
        assertTrue(r.puedeEliminar(11, Rol.ADMIN));
    }
}
