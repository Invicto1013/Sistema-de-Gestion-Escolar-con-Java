package com.unev.sige.data;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.unev.sige.model.Estudiante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Ayudas de las pruebas de integración contra MySQL real.
 *
 * Se ejecutan SOLO si SIGE_IT_DB=true y la base configurada es local (localhost o 127.0.0.1).
 * Así nunca tocan la base de Aiven aunque tengas SIGE_DB_URL apuntando a ella: sin esas dos
 * condiciones, las pruebas se omiten y 'mvn test' sigue verde en cualquier equipo.
 */
final class EntornoIntegracion {
    static final String SUFIJO = "@test.sige";

    private EntornoIntegracion() { }

    static boolean habilitado() {
        if (!"true".equalsIgnoreCase(System.getenv("SIGE_IT_DB"))) return false;
        String url = ConexionBD.configuracion("SIGE_DB_URL", "db.url");
        return url != null && (url.contains("//localhost") || url.contains("//127.0.0.1"));
    }

    static void exigirBaseDePrueba() {
        assumeTrue(habilitado(), "Integración omitida: define SIGE_IT_DB=true y una base MySQL local");
    }

    static String correoUnico() {
        return "it_" + System.nanoTime() + SUFIJO;
    }

    static String matriculaUnica() {
        return "IT" + (System.nanoTime() % 100_000_000L);
    }

    static Estudiante nuevoEstudiante(String correo) throws SQLException {
        return new Estudiante(0, "Prueba", "Integración", correo, "", matriculaUnica(), gradoValido(), 0.0);
    }

    static int idAdministrador() throws SQLException {
        return consultaEntera("SELECT id_usuario FROM usuarios WHERE rol='ADMIN' AND activo=1 ORDER BY id_usuario LIMIT 1");
    }

    static String gradoValido() throws SQLException {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("SELECT grado FROM estudiantes WHERE grado IS NOT NULL LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "1ro";
        }
    }

    static int consultaEntera(String sql) throws SQLException {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /** Borra todo lo creado por las pruebas (las tablas hijas caen por ON DELETE CASCADE). */
    static void limpiar() throws SQLException {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE correo LIKE ?")) {
            ps.setString(1, "it\\_%" + SUFIJO);
            ps.executeUpdate();
        }
    }
}
