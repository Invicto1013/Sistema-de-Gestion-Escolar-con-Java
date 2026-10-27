package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Comprueba el pool y el esquema real (omitidas si no hay base de prueba). */
class ConexionBDIntegracionTest {
    @BeforeEach
    void preparar() {
        EntornoIntegracion.exigirBaseDePrueba();
    }

    @Test
    void elPoolEntregaConexionesValidas() throws Exception {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
        assertFalse(ConexionBD.estadoPool().isBlank());
    }

    @Test
    void elScriptCompletoCreaLasTablasEsenciales() throws Exception {
        for (String tabla : new String[]{"usuarios", "estudiantes", "profesores", "padres",
                "calificaciones", "sesiones_web", "aula_archivos", "bitacora_accesos"}) {
            try (Connection c = ConexionBD.obtener();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
                ps.setString(1, tabla);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(1, rs.getInt(1), "falta la tabla " + tabla);
                }
            }
        }
    }

    @Test
    void usuariosTieneLaColumnaDeAprobacion() throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT pendiente_aprobacion FROM usuarios LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.getMetaData().getColumnCount() == 1);
        }
    }
}
