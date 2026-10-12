package com.unev.sige.data;

import com.unev.sige.model.ControladorSesion;
import com.unev.sige.model.SesionRegistrada;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra los inicios y cierres de sesion en la tabla `controlador_sesiones`.
 */
public class SesionDAO {

    /**
     * Abre una sesion para el usuario. fecha_inicio y estado_activo los pone MySQL por defecto.
     */
    public ControladorSesion iniciar(int idUsuario, String direccionIP) throws SQLException {
        String sql = "INSERT INTO controlador_sesiones (id_usuario, direccion_ip) VALUES (?, ?)";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, direccionIP);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new ControladorSesion(keys.getInt(1), idUsuario, direccionIP);
                }
                throw new SQLException("MySQL no devolvio el id de la sesion.");
            }
        }
    }

    /**
     * Cierra la sesion: guarda la fecha de cierre y la marca como inactiva.
     * Si ya estaba cerrada no hace nada.
     */
    public void cerrar(int idSesion) throws SQLException {
        String sql = "UPDATE controlador_sesiones SET fecha_cierre = CURRENT_TIMESTAMP, estado_activo = 0 "
                + "WHERE id_sesion = ? AND estado_activo = 1";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSesion);
            ps.executeUpdate();
        }
    }

    /**
     * Devuelve como máximo 500 sesiones recientes. La consulta también verifica en MySQL
     * que el usuario solicitante conserve el rol ADMIN.
     */
    public List<SesionRegistrada> listarRecientes(int idAdministrador) throws SQLException {
        String sql = """
                SELECT cs.id_sesion,
                       CONCAT(u.nombre, ' ', u.apellido) AS usuario,
                       u.correo,
                       DATE_FORMAT(cs.fecha_inicio, '%Y-%m-%d %H:%i:%s') AS inicio,
                       COALESCE(DATE_FORMAT(cs.fecha_cierre, '%Y-%m-%d %H:%i:%s'), '—') AS cierre,
                       COALESCE(cs.direccion_ip, '—') AS direccion_ip,
                       CASE WHEN cs.estado_activo = 1 THEN 'Activa' ELSE 'Cerrada' END AS estado
                FROM controlador_sesiones cs
                JOIN usuarios u ON u.id_usuario = cs.id_usuario
                WHERE EXISTS (
                    SELECT 1 FROM usuarios admin
                    WHERE admin.id_usuario = ? AND admin.rol = 'ADMIN'
                )
                ORDER BY cs.fecha_inicio DESC, cs.id_sesion DESC
                LIMIT 500
                """;
        List<SesionRegistrada> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAdministrador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new SesionRegistrada(
                            rs.getInt("id_sesion"), rs.getString("usuario"), rs.getString("correo"),
                            rs.getString("inicio"), rs.getString("cierre"), rs.getString("direccion_ip"),
                            rs.getString("estado")));
                }
            }
        }
        return resultado;
    }
}
