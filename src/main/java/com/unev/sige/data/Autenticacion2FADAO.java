package com.unev.sige.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Operaciones transaccionales para segundo factor y bitácora de acceso. */
public final class Autenticacion2FADAO {
    public record Estado(String secretoCifrado, boolean activo, boolean bloqueado) { }
    public record Intento(boolean bloqueado, int restantes) { }

    public Estado obtenerEstado(int idUsuario) throws SQLException {
        String sql = "SELECT dos_factor_secreto, dos_factor_activo, "
                + "(dos_factor_bloqueado_hasta IS NOT NULL AND dos_factor_bloqueado_hasta > CURRENT_TIMESTAMP) AS bloqueado "
                + "FROM usuarios WHERE id_usuario = ? AND activo = 1";
        try (Connection c = ConexionBD.obtener(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, idUsuario);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("La cuenta no existe o está desactivada.");
                return new Estado(r.getString("dos_factor_secreto"), r.getBoolean("dos_factor_activo"),
                        r.getBoolean("bloqueado"));
            }
        }
    }

    /** Persiste el secreto cifrado sin permitir que cancelar reinicie intentos o bloqueo. */
    public String guardarSecretoPendienteSiFalta(int id, String secretoCifrado) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                String actual;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT dos_factor_secreto, dos_factor_activo FROM usuarios WHERE id_usuario=? AND activo=1 FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("La cuenta no existe o está desactivada.");
                        actual = r.getString(1);
                        if (r.getBoolean(2)) throw new SQLException("El segundo factor ya está activo.");
                    }
                }
                if (actual == null || actual.isBlank()) {
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE usuarios SET dos_factor_secreto=? WHERE id_usuario=? AND dos_factor_activo=0")) {
                        p.setString(1, secretoCifrado); p.setInt(2, id); p.executeUpdate();
                    }
                    actual = secretoCifrado;
                }
                c.commit(); return actual;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public boolean estaBloqueado(int id) throws SQLException {
        try (Connection c = ConexionBD.obtener(); PreparedStatement p = c.prepareStatement(
                "SELECT dos_factor_bloqueado_hasta > CURRENT_TIMESTAMP FROM usuarios WHERE id_usuario=?")) {
            p.setInt(1, id); try (ResultSet r = p.executeQuery()) { return r.next() && r.getBoolean(1); }
        }
    }

    public Intento registrarIntentoFallido(int id) throws SQLException {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                int intentos;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT dos_factor_intentos, dos_factor_bloqueado_hasta > CURRENT_TIMESTAMP "
                                + "FROM usuarios WHERE id_usuario=? FOR UPDATE")) {
                    p.setInt(1,id); try(ResultSet r=p.executeQuery()) {
                        if (!r.next()) throw new SQLException("La cuenta no existe.");
                        if (r.getBoolean(2)) { c.commit(); return new Intento(true,0); }
                        intentos = r.getInt(1) + 1;
                    }
                }
                boolean bloquear = intentos >= 3;
                String sql = bloquear
                        ? "UPDATE usuarios SET dos_factor_intentos=0, dos_factor_bloqueado_hasta=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE) WHERE id_usuario=?"
                        : "UPDATE usuarios SET dos_factor_intentos=? WHERE id_usuario=?";
                try (PreparedStatement p=c.prepareStatement(sql)) {
                    if (bloquear) p.setInt(1,id); else {p.setInt(1,intentos);p.setInt(2,id);} p.executeUpdate();
                }
                c.commit(); return new Intento(bloquear, Math.max(0,3-intentos));
            } catch(SQLException e) {c.rollback();throw e;}
        }
    }

    public void registrarExito(int id, boolean activar) throws SQLException {
        String sql = "UPDATE usuarios SET dos_factor_activo=IF(?,1,dos_factor_activo), dos_factor_intentos=0, "
                + "dos_factor_bloqueado_hasta=NULL WHERE id_usuario=? AND activo=1";
        try (Connection c=ConexionBD.obtener(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setBoolean(1,activar);p.setInt(2,id);
            if(p.executeUpdate()!=1) throw new SQLException("No se pudo confirmar el código 2FA.");
        }
    }

    public void registrarAcceso(Integer id, String correo, String resultado, String detalle) throws SQLException {
        String sql="INSERT INTO bitacora_accesos (id_usuario, correo_ingresado, resultado, detalle) VALUES (?,?,?,?)";
        try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(sql)) {
            if(id==null)p.setNull(1,java.sql.Types.INTEGER);else p.setInt(1,id);
            p.setString(2,correo);p.setString(3,resultado);p.setString(4,detalle);p.executeUpdate();
        }
    }
}
