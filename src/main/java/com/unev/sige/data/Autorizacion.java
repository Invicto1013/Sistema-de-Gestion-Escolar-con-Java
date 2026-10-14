package com.unev.sige.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Comprobacion de permisos en la capa de datos: no basta con ocultar un boton en la pantalla,
 * cada operacion que modifica datos administrativos vuelve a verificar en MySQL que quien la
 * ejecuta sea un administrador con la cuenta activa.
 */
final class Autorizacion {

    private Autorizacion() {
    }

    static void exigirAdministrador(Connection conn, int idAdmin) throws SQLException, PermisoDenegadoException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM usuarios WHERE id_usuario = ? AND rol = 'ADMIN' AND activo = 1")) {
            ps.setInt(1, idAdmin);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PermisoDenegadoException("Solo un administrador con la cuenta activa puede realizar esta operación.");
                }
            }
        }
    }
}
