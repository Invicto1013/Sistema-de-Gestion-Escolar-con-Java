package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unev.sige.model.Estudiante;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integración contra MySQL local (omitida si no hay base de prueba): borrado total y auditoría. */
class EliminacionUsuarioDAOIntegracionTest {
    private static final String CLAVE = "Clave2026";
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final EliminacionUsuarioDAO dao = new EliminacionUsuarioDAO();

    @BeforeEach
    void preparar() {
        EntornoIntegracion.exigirBaseDePrueba();
    }

    @AfterEach
    void limpiar() throws Exception {
        if (EntornoIntegracion.habilitado()) EntornoIntegracion.limpiar();
    }

    @Test
    void eliminaLaCuentaYSusDatosYDejaRastroSinCorreo() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        String correo = EntornoIntegracion.correoUnico();
        Estudiante e = EntornoIntegracion.nuevoEstudiante(correo);
        usuarios.registrar(e, CLAVE);

        EliminacionUsuarioDAO.Resultado r = dao.eliminarComoAdmin(admin, e.getId(), correo);

        assertEquals("ESTUDIANTE", r.rol());
        assertFalse(usuarios.existeCorreo(correo));
        assertFalse(usuarios.existeMatricula(e.getMatricula()));
        assertEquals(0, EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM estudiantes WHERE id_estudiante=" + e.getId()));
        assertEquals(1, EntornoIntegracion.consultaEntera(
                "SELECT COUNT(*) FROM auditoria WHERE accion='CUENTA_ELIMINADA' AND id_entidad='" + e.getId()
                        + "' AND id_usuario=" + admin + " AND detalle NOT LIKE '%@%'"));
    }

    @Test
    void conUnCorreoIncorrectoNoSeBorraNada() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        String correo = EntornoIntegracion.correoUnico();
        Estudiante e = EntornoIntegracion.nuevoEstudiante(correo);
        usuarios.registrar(e, CLAVE);

        assertThrows(PermisoDenegadoException.class, () -> dao.eliminarComoAdmin(admin, e.getId(), "otro@test.sige"));
        assertTrue(usuarios.existeCorreo(correo));
    }

    @Test
    void soloUnAdministradorPuedeEliminar() throws Exception {
        String correo = EntornoIntegracion.correoUnico();
        Estudiante victima = EntornoIntegracion.nuevoEstudiante(correo);
        Estudiante intruso = EntornoIntegracion.nuevoEstudiante(EntornoIntegracion.correoUnico());
        usuarios.registrar(victima, CLAVE);
        usuarios.registrar(intruso, CLAVE);

        assertThrows(PermisoDenegadoException.class, () -> dao.eliminarComoAdmin(intruso.getId(), victima.getId(), correo));
        assertTrue(usuarios.existeCorreo(correo));
    }

    @Test
    void elAdministradorNoPuedeEliminarseASiMismo() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        assertThrows(PermisoDenegadoException.class, () -> dao.eliminarComoAdmin(admin, admin, "x"));
        assertTrue(EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM usuarios WHERE id_usuario=" + admin) == 1);
    }
}
