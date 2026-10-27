package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unev.sige.model.Estudiante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de integración de las sesiones web persistentes (omitidas si no hay base de prueba). */
class SesionWebDAOIntegracionTest {
    private final SesionWebDAO sesiones = new SesionWebDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();

    @BeforeEach
    void preparar() {
        EntornoIntegracion.exigirBaseDePrueba();
    }

    @AfterEach
    void limpiar() throws Exception {
        if (EntornoIntegracion.habilitado()) EntornoIntegracion.limpiar();
    }

    private int nuevoUsuario() throws Exception {
        Estudiante e = EntornoIntegracion.nuevoEstudiante(EntornoIntegracion.correoUnico());
        usuarios.registrar(e, "Clave2026");
        return e.getId();
    }

    private static String token() {
        return "tok_" + System.nanoTime();
    }

    @Test
    void guardaYRecuperaUnaSesionPendienteDeDosFactores() throws Exception {
        int id = nuevoUsuario();
        String token = token();
        sesiones.guardar(token, "csrf-abc", null, id, true, null);

        SesionWebDAO.Datos datos = sesiones.cargar(token, 3600);
        assertNotNull(datos);
        assertEquals("csrf-abc", datos.csrf());
        assertEquals(id, datos.idPendiente().intValue());
        assertNull(datos.idUsuario());
        assertTrue(datos.mfaActivo());
    }

    @Test
    void nuncaGuardaElTokenEnTextoPlano() throws Exception {
        int id = nuevoUsuario();
        String token = token();
        sesiones.guardar(token, "c", null, id, false, null);
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM sesiones_web WHERE token_hash = ?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    void unTokenDesconocidoNoDevuelveSesion() throws Exception {
        assertNull(sesiones.cargar("no-existe-" + System.nanoTime(), 3600));
    }

    @Test
    void cerrarSesionYBorrarUnUsuarioEliminanSusSesiones() throws Exception {
        int id = nuevoUsuario();
        String t1 = token();
        String t2 = token() + "b";
        sesiones.guardar(t1, "c1", null, id, false, null);
        sesiones.guardar(t2, "c2", null, id, false, null);

        sesiones.eliminar(t1);
        assertNull(sesiones.cargar(t1, 3600));
        assertNotNull(sesiones.cargar(t2, 3600));

        EntornoIntegracion.limpiar();
        assertNull(sesiones.cargar(t2, 3600), "ON DELETE CASCADE al borrar el usuario");
    }

    @Test
    void laInactividadInvalidaLaSesionYLaPurgaLaBorra() throws Exception {
        int id = nuevoUsuario();
        String token = token();
        sesiones.guardar(token, "c", null, id, false, null);
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE sesiones_web SET ultimo_acceso = NOW() - INTERVAL 2 HOUR WHERE token_hash = ?")) {
            ps.setString(1, SesionWebDAO.hash(token));
            ps.executeUpdate();
        }
        assertNull(sesiones.cargar(token, 3600));
        assertTrue(sesiones.purgarCaducadas(3600) >= 1);
    }
}
