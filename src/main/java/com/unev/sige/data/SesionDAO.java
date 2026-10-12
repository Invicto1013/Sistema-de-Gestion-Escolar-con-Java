package com.unev.sige.data;

import com.unev.sige.model.ControladorSesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
}
