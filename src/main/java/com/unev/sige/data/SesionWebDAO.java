package com.unev.sige.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HexFormat;

/**
 * Guarda en MySQL (tabla sesiones_web) las sesiones de la web para que sobrevivan a reinicios del servidor.
 * Solo se persisten las sesiones con usuario (o con contraseña verificada y 2FA pendiente).
 * Seguridad: en la base se guarda el hash SHA-256 del token de la cookie, nunca el token; así, una copia
 * de la tabla no permite suplantar a nadie. El secreto TOTP tampoco se guarda aquí: se vuelve a leer, cifrado,
 * de la tabla de 2FA cuando hace falta.
 */
public final class SesionWebDAO {

    /** Estado persistido de una sesión. idLogin es la fila de controlador_sesiones (historial). */
    public record Datos(String csrf, Integer idUsuario, Integer idPendiente, boolean mfaActivo, Integer idLogin) { }

    /** SHA-256 en hexadecimal del token de la cookie. */
    public static String hash(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static void entero(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }

    /** Crea o actualiza la sesión y renueva su último acceso. */
    public void guardar(String token, String csrf, Integer idUsuario, Integer idPendiente,
                        boolean mfaActivo, Integer idLogin) throws SQLException {
        String sql = "INSERT INTO sesiones_web(token_hash,csrf,id_usuario,id_pendiente,mfa_activo,id_login) "
                + "VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE csrf=VALUES(csrf),id_usuario=VALUES(id_usuario),"
                + "id_pendiente=VALUES(id_pendiente),mfa_activo=VALUES(mfa_activo),id_login=VALUES(id_login),"
                + "ultimo_acceso=NOW()";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash(token));
            ps.setString(2, csrf);
            entero(ps, 3, idUsuario);
            entero(ps, 4, idPendiente);
            ps.setBoolean(5, mfaActivo);
            entero(ps, 6, idLogin);
            ps.executeUpdate();
        }
    }

    /**
     * Devuelve la sesión si existe, no superó la inactividad máxima y, cuando ya inició sesión,
     * su registro en controlador_sesiones sigue activo (un administrador puede revocarla).
     */
    public Datos cargar(String token, long maxInactividadSegundos) throws SQLException {
        String sql = "SELECT w.csrf,w.id_usuario,w.id_pendiente,w.mfa_activo,w.id_login FROM sesiones_web w "
                + "LEFT JOIN controlador_sesiones c ON c.id_sesion=w.id_login "
                + "WHERE w.token_hash=? AND w.ultimo_acceso > NOW() - INTERVAL ? SECOND "
                + "AND (w.id_usuario IS NULL OR c.estado_activo=1)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash(token));
            ps.setLong(2, maxInactividadSegundos);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int usuario = rs.getInt("id_usuario");
                Integer idUsuario = rs.wasNull() ? null : usuario;
                int pendiente = rs.getInt("id_pendiente");
                Integer idPendiente = rs.wasNull() ? null : pendiente;
                int login = rs.getInt("id_login");
                Integer idLogin = rs.wasNull() ? null : login;
                return new Datos(rs.getString("csrf"), idUsuario, idPendiente, rs.getBoolean("mfa_activo"), idLogin);
            }
        }
    }

    /** Renueva el último acceso (la web lo llama como máximo una vez por minuto por sesión). */
    public void tocar(String token) throws SQLException {
        ejecutar("UPDATE sesiones_web SET ultimo_acceso=NOW() WHERE token_hash=?", hash(token));
    }

    public void eliminar(String token) throws SQLException {
        ejecutar("DELETE FROM sesiones_web WHERE token_hash=?", hash(token));
    }

    /** Cierra todas las sesiones web de un usuario (por ejemplo, tras restablecer su contraseña). */
    public void eliminarDeUsuario(int idUsuario) throws SQLException {
        String sql = "DELETE FROM sesiones_web WHERE id_usuario=? OR id_pendiente=?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    /** Elimina la sesión web asociada a una fila del historial (cuando un administrador la cierra). */
    public void eliminarPorLogin(int idLogin) throws SQLException {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sesiones_web WHERE id_login=?")) {
            ps.setInt(1, idLogin);
            ps.executeUpdate();
        }
    }

    public void eliminarTodas() throws SQLException {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement("DELETE FROM sesiones_web")) {
            ps.executeUpdate();
        }
    }

    /** Borra las sesiones que superaron la inactividad máxima. */
    public int purgarCaducadas(long maxInactividadSegundos) throws SQLException {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM sesiones_web WHERE ultimo_acceso <= NOW() - INTERVAL ? SECOND")) {
            ps.setLong(1, maxInactividadSegundos);
            return ps.executeUpdate();
        }
    }

    private static void ejecutar(String sql, String valor) throws SQLException {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.executeUpdate();
        }
    }
}
