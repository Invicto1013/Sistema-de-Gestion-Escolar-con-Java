package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de integración de UsuarioDAO contra MySQL real (omitidas si no hay base de prueba). */
class UsuarioDAOIntegracionTest {
    private static final String CLAVE = "Clave2026";
    private final UsuarioDAO dao = new UsuarioDAO();

    @BeforeEach
    void preparar() throws Exception {
        EntornoIntegracion.exigirBaseDePrueba();
    }

    @AfterEach
    void limpiar() throws Exception {
        if (EntornoIntegracion.habilitado()) EntornoIntegracion.limpiar();
    }

    @Test
    void registraUnEstudianteYPermiteAutenticarlo() throws Exception {
        String correo = EntornoIntegracion.correoUnico();
        Estudiante e = EntornoIntegracion.nuevoEstudiante(correo);
        dao.registrar(e, CLAVE);

        assertTrue(e.getId() > 0);
        Usuario guardado = dao.buscarPorCorreo(correo).orElseThrow();
        assertTrue(guardado.isActivo());
        assertTrue(guardado.autenticar(correo, CLAVE));
        assertFalse(guardado.autenticar(correo, "Incorrecta1"));
        assertTrue(dao.existeCorreo(correo));
        assertTrue(dao.existeMatricula(e.getMatricula()));
    }

    @Test
    void unCorreoDuplicadoFallaYNoDejaDatosAMedias() throws Exception {
        String correo = EntornoIntegracion.correoUnico();
        dao.registrar(EntornoIntegracion.nuevoEstudiante(correo), CLAVE);

        Estudiante repetido = EntornoIntegracion.nuevoEstudiante(correo);
        assertThrows(java.sql.SQLException.class, () -> dao.registrar(repetido, CLAVE));
        assertFalse(dao.existeMatricula(repetido.getMatricula()), "la transacción debe revertirse");
    }

    @Test
    void rechazaUnaContrasenaQueNoCumpleLaPolitica() throws Exception {
        String correo = EntornoIntegracion.correoUnico();
        assertThrows(IllegalArgumentException.class,
                () -> dao.registrar(EntornoIntegracion.nuevoEstudiante(correo), "corta"));
        assertFalse(dao.existeCorreo(correo));
    }

    @Test
    void unaSolicitudQuedaPendienteHastaQueUnAdminLaApruebe() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        String correo = EntornoIntegracion.correoUnico();
        Estudiante e = EntornoIntegracion.nuevoEstudiante(correo);
        dao.registrarSolicitud(e, CLAVE);

        Usuario pendiente = dao.buscarPorCorreo(correo).orElseThrow();
        assertFalse(pendiente.isActivo());
        assertTrue(dao.estaPendienteAprobacion(e.getId()));
        assertTrue(dao.listarSolicitudes().stream().anyMatch(s -> s.id() == e.getId()));
        assertTrue(dao.contarPendientes() >= 1);

        assertEquals(correo, dao.aprobarSolicitudComoAdmin(admin, e.getId()));
        assertTrue(dao.buscarPorCorreo(correo).orElseThrow().isActivo());
        assertFalse(dao.estaPendienteAprobacion(e.getId()));
    }

    @Test
    void rechazarEliminaLaCuentaYNoSePuedeAtenderDosVeces() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        String correo = EntornoIntegracion.correoUnico();
        Estudiante e = EntornoIntegracion.nuevoEstudiante(correo);
        dao.registrarSolicitud(e, CLAVE);

        dao.rechazarSolicitudComoAdmin(admin, e.getId());
        assertFalse(dao.existeCorreo(correo));
        assertFalse(dao.existeMatricula(e.getMatricula()));
        assertThrows(PermisoDenegadoException.class, () -> dao.rechazarSolicitudComoAdmin(admin, e.getId()));
    }

    @Test
    void soloUnAdministradorActivoPuedeAprobar() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        Estudiante solicitante = EntornoIntegracion.nuevoEstudiante(EntornoIntegracion.correoUnico());
        Estudiante intruso = EntornoIntegracion.nuevoEstudiante(EntornoIntegracion.correoUnico());
        dao.registrarSolicitud(solicitante, CLAVE);
        dao.registrar(intruso, CLAVE);

        assertThrows(PermisoDenegadoException.class,
                () -> dao.aprobarSolicitudComoAdmin(intruso.getId(), solicitante.getId()));
        assertTrue(dao.estaPendienteAprobacion(solicitante.getId()), "sigue pendiente");
        dao.aprobarSolicitudComoAdmin(admin, solicitante.getId());
    }

    @Test
    void elAdminPuedeDesactivarYReactivarUnaCuenta() throws Exception {
        int admin = EntornoIntegracion.idAdministrador();
        String correo = EntornoIntegracion.correoUnico();
        Estudiante e = EntornoIntegracion.nuevoEstudiante(correo);
        dao.registrar(e, CLAVE);

        dao.cambiarEstadoCuentaComoAdmin(admin, e.getId(), false);
        assertFalse(dao.buscarPorCorreo(correo).orElseThrow().isActivo());
        dao.cambiarEstadoCuentaComoAdmin(admin, e.getId(), true);
        assertTrue(dao.buscarPorCorreo(correo).orElseThrow().isActivo());
    }
}
