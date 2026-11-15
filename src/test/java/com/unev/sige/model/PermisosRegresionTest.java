package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Prueba de regresión de la matriz de permisos (práctica de agentes de IA).
 * Fija el comportamiento actual de cada función para cada rol: si una mejora cambia un permiso sin querer,
 * esta prueba falla y obliga a decidir si el cambio es intencional (y entonces se actualiza la tabla a propósito).
 */
class PermisosRegresionTest {

    private static final Map<String, Predicate<Rol>> FUNCIONES = Map.of(
            "veInformes", Permisos::veInformes,
            "publicaAnuncios", Permisos::publicaAnuncios,
            "gestionaHorarios", Permisos::gestionaHorarios,
            "atiendeSolicitudes", Permisos::atiendeSolicitudes,
            "gestionaVinculos", Permisos::gestionaVinculos,
            "veAuditoria", Permisos::veAuditoria,
            "registraConducta", Permisos::registraConducta,
            "veConductaCompleta", Permisos::veConductaCompleta,
            "eliminaConductaAjena", Permisos::eliminaConductaAjena,
            "esPersonalCreable", Permisos::esPersonalCreable);

    /** Roles que tienen el permiso concedido; cualquier otro rol debe tenerlo denegado. */
    private static final Map<String, Rol[]> CONCEDIDOS = Map.of(
            "veInformes", new Rol[] {Rol.ADMIN, Rol.DIRECTOR, Rol.SECRETARIA},
            "publicaAnuncios", new Rol[] {Rol.ADMIN, Rol.DIRECTOR, Rol.SECRETARIA},
            "gestionaHorarios", new Rol[] {Rol.ADMIN, Rol.SECRETARIA},
            "atiendeSolicitudes", new Rol[] {Rol.ADMIN, Rol.SECRETARIA},
            "gestionaVinculos", new Rol[] {Rol.ADMIN, Rol.SECRETARIA},
            "veAuditoria", new Rol[] {Rol.ADMIN, Rol.DIRECTOR},
            "registraConducta", new Rol[] {Rol.ADMIN, Rol.DIRECTOR, Rol.PROFESOR},
            "veConductaCompleta", new Rol[] {Rol.ADMIN, Rol.DIRECTOR},
            "eliminaConductaAjena", new Rol[] {Rol.ADMIN, Rol.DIRECTOR},
            "esPersonalCreable", new Rol[] {Rol.DIRECTOR, Rol.SECRETARIA});

    private static boolean contiene(Rol[] roles, Rol r) {
        for (Rol x : roles) if (x == r) return true;
        return false;
    }

    @Test void cadaPermisoCoincideExactamenteConLaMatrizAprobada() {
        assertEquals(CONCEDIDOS.keySet(), FUNCIONES.keySet(), "la tabla debe cubrir todas las funciones");
        for (var f : FUNCIONES.entrySet()) {
            for (Rol r : Rol.values()) {
                boolean esperado = contiene(CONCEDIDOS.get(f.getKey()), r);
                assertEquals(esperado, f.getValue().test(r), f.getKey() + " para " + r);
            }
        }
    }

    @Test void unRolNuloNuncaTieneNingunPermiso() {
        for (var f : FUNCIONES.entrySet()) assertFalse(f.getValue().test(null), f.getKey());
    }

    @Test void secretariaNoPuedeEliminarConductaAjenaNiVerAuditoria() {
        assertFalse(Permisos.eliminaConductaAjena(Rol.SECRETARIA));
        assertFalse(Permisos.veConductaCompleta(Rol.SECRETARIA));
        assertFalse(Permisos.veAuditoria(Rol.SECRETARIA));
    }

    @Test void estudiantesYPadresNoTienenNingunPermisoDeGestion() {
        for (Rol r : new Rol[] {Rol.ESTUDIANTE, Rol.PADRE}) {
            for (var f : FUNCIONES.entrySet()) assertFalse(f.getValue().test(r), f.getKey() + " para " + r);
        }
    }

    @Test void elAdministradorNoSeCreaDesdeCrearPersonal() {
        assertFalse(Permisos.esPersonalCreable(Rol.ADMIN));
        Map<Rol, Boolean> creables = new EnumMap<>(Rol.class);
        for (Rol r : Rol.values()) creables.put(r, Permisos.esPersonalCreable(r));
        assertEquals(2, creables.values().stream().filter(b -> b).count());
    }
}
