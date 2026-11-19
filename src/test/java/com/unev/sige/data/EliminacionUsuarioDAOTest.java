package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas sin base de datos de las reglas de la eliminación de cuentas. */
class EliminacionUsuarioDAOTest {

    @Test
    void elCorreoDebeCoincidirSinImportarMayusculasNiEspaciosExternos() {
        assertTrue(EliminacionUsuarioDAO.correoCoincide("ana@escuela.edu", "ana@escuela.edu"));
        assertTrue(EliminacionUsuarioDAO.correoCoincide("Ana@Escuela.edu", "  ana@escuela.EDU "));
    }

    @Test
    void unCorreoDistintoOVacioNoConfirma() {
        assertFalse(EliminacionUsuarioDAO.correoCoincide("ana@escuela.edu", "ana@escuela.com"));
        assertFalse(EliminacionUsuarioDAO.correoCoincide("ana@escuela.edu", ""));
        assertFalse(EliminacionUsuarioDAO.correoCoincide("ana@escuela.edu", "   "));
        assertFalse(EliminacionUsuarioDAO.correoCoincide("ana@escuela.edu", null));
        assertFalse(EliminacionUsuarioDAO.correoCoincide(null, "ana@escuela.edu"));
    }

    @Test
    void unAdministradorNoPuedeEliminarseASiMismoAntesDeTocarLaBase() {
        // Falla antes de abrir conexión, por eso funciona sin MySQL.
        assertThrows(PermisoDenegadoException.class, () -> new EliminacionUsuarioDAO().eliminarComoAdmin(1, 1, "x@x.com"));
    }
}
