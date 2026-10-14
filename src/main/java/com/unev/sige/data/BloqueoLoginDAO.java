package com.unev.sige.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Bloqueo temporal de la cuenta tras varias contraseñas incorrectas seguidas.
 * Usa las columnas login_intentos y login_bloqueado_hasta (sql/fase6_seguridad_login.sql).
 * Es independiente del bloqueo del 2FA, que se aplica después de acertar la contraseña.
 */
public final class BloqueoLoginDAO {

    public static final int MAX_INTENTOS = 5;
    public static final int MINUTOS_BLOQUEO = 15;

    public record Intento(boolean bloqueado, int restantes) {
    }

    /** Segundos que faltan para que termine el bloqueo; 0 si la cuenta no está bloqueada. */
    public long segundosRestantes(int idUsuario) throws SQLException {
        String sql = "SELECT GREATEST(0, TIMESTAMPDIFF(SECOND, CURRENT_TIMESTAMP, login_bloqueado_hasta)) "
                + "FROM usuarios WHERE id_usuario = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /**
     * Cuenta una contraseña incorrecta. Al llegar a MAX_INTENTOS bloquea la cuenta MINUTOS_BLOQUEO
     * minutos y reinicia el contador. Si la cuenta ya estaba bloqueada no suma nada.
     */
    public Intento registrarFallo(int idUsuario) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                int intentos;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT login_intentos, COALESCE(login_bloqueado_hasta > CURRENT_TIMESTAMP, 0) "
                                + "FROM usuarios WHERE id_usuario = ? FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.commit();
                            return new Intento(false, MAX_INTENTOS);
                        }
                        if (rs.getBoolean(2)) {
                            c.commit();
                            return new Intento(true, 0);
                        }
                        intentos = rs.getInt(1) + 1;
                    }
                }

                boolean bloquear = intentos >= MAX_INTENTOS;
                if (bloquear) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE usuarios SET login_intentos = 0, "
                                    + "login_bloqueado_hasta = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL " + MINUTOS_BLOQUEO
                                    + " MINUTE) WHERE id_usuario = ?")) {
                        ps.setInt(1, idUsuario);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE usuarios SET login_intentos = ? WHERE id_usuario = ?")) {
                        ps.setInt(1, intentos);
                        ps.setInt(2, idUsuario);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                return new Intento(bloquear, Math.max(0, MAX_INTENTOS - intentos));
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Contraseña correcta: borra el contador y cualquier bloqueo vencido. */
    public void reiniciar(int idUsuario) throws SQLException {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(
                "UPDATE usuarios SET login_intentos = 0, login_bloqueado_hasta = NULL WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    /** true si el error de MySQL es "columna desconocida": falta ejecutar el script de la fase 6. */
    public static boolean faltaMigracion(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s && ("42S22".equals(s.getSQLState()) || s.getErrorCode() == 1054)) {
                return true;
            }
        }
        return false;
    }
}
