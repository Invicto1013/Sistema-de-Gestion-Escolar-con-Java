package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PermisosTest {
    @Test void administradorTieneTodasLasFuncionesDeGestion() {
        assertTrue(Permisos.veInformes(Rol.ADMIN));
        assertTrue(Permisos.publicaAnuncios(Rol.ADMIN));
        assertTrue(Permisos.gestionaHorarios(Rol.ADMIN));
        assertTrue(Permisos.atiendeSolicitudes(Rol.ADMIN));
        assertTrue(Permisos.gestionaVinculos(Rol.ADMIN));
        assertTrue(Permisos.veAuditoria(Rol.ADMIN));
    }
    @Test void directorSupervisaPeroNoAdministraLasCuentas() {
        assertTrue(Permisos.veInformes(Rol.DIRECTOR));
        assertTrue(Permisos.veAuditoria(Rol.DIRECTOR));
        assertTrue(Permisos.publicaAnuncios(Rol.DIRECTOR));
        assertTrue(Permisos.registraConducta(Rol.DIRECTOR));
        assertTrue(Permisos.eliminaConductaAjena(Rol.DIRECTOR));
        assertFalse(Permisos.atiendeSolicitudes(Rol.DIRECTOR));
        assertFalse(Permisos.gestionaVinculos(Rol.DIRECTOR));
        assertFalse(Permisos.gestionaHorarios(Rol.DIRECTOR));
    }
    @Test void secretariaHaceLaAdministracionDiaria() {
        assertTrue(Permisos.atiendeSolicitudes(Rol.SECRETARIA));
        assertTrue(Permisos.gestionaVinculos(Rol.SECRETARIA));
        assertTrue(Permisos.gestionaHorarios(Rol.SECRETARIA));
        assertTrue(Permisos.veInformes(Rol.SECRETARIA));
        assertFalse(Permisos.veAuditoria(Rol.SECRETARIA));
        assertFalse(Permisos.registraConducta(Rol.SECRETARIA));
    }
    @Test void profesoresEstudiantesYPadresNoGestionan() {
        for (Rol r : new Rol[] {Rol.PROFESOR, Rol.ESTUDIANTE, Rol.PADRE}) {
            assertFalse(Permisos.veInformes(r), r.name());
            assertFalse(Permisos.publicaAnuncios(r), r.name());
            assertFalse(Permisos.veAuditoria(r), r.name());
            assertFalse(Permisos.esPersonalCreable(r), r.name());
        }
        assertTrue(Permisos.registraConducta(Rol.PROFESOR));
        assertFalse(Permisos.eliminaConductaAjena(Rol.PROFESOR));
    }
    @Test void soloDirectorYSecretariaSePuedenCrearComoPersonal() {
        assertTrue(Permisos.esPersonalCreable(Rol.DIRECTOR));
        assertTrue(Permisos.esPersonalCreable(Rol.SECRETARIA));
        assertFalse(Permisos.esPersonalCreable(Rol.ADMIN));
        assertFalse(Permisos.esPersonalCreable(null));
        assertFalse(Permisos.veInformes(null));
    }
    @Test void rolesNuevosSonPersonalAdministrativo() {
        assertTrue(Rol.DIRECTOR.esPersonalAdministrativo());
        assertTrue(Rol.SECRETARIA.esPersonalAdministrativo());
        assertFalse(Rol.PROFESOR.esPersonalAdministrativo());
        assertEquals("Director/a", Rol.DIRECTOR.etiqueta());
        assertEquals("Secretario/a", Rol.SECRETARIA.etiqueta());
    }
}
