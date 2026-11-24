package com.unev.sige.data;

import com.unev.sige.model.CatalogoPlanes;
import com.unev.sige.model.SuscripcionCentro;
import com.unev.sige.model.ZonaVisual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Plan contratado por el centro (tabla suscripcion_centro, una fila por cambio; vale la más reciente). */
public class SuscripcionDAO {

    public Optional<SuscripcionCentro> actual() throws SQLException {
        String sql = "SELECT s.codigo_plan, " + ZonaVisual.sql("s.cambiado_en") + " AS desde, "
                + "COALESCE(CONCAT(u.nombre, ' ', u.apellido), 'Sistema') AS quien "
                + "FROM suscripcion_centro s LEFT JOIN usuarios u ON u.id_usuario = s.cambiado_por "
                + "ORDER BY s.id_suscripcion DESC LIMIT 1";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(new SuscripcionCentro(rs.getString(1), rs.getString(2), rs.getString(3))) : Optional.empty();
        }
    }

    /** Registra el cambio de plan. Lanza IllegalArgumentException si el código no existe. */
    public void cambiar(String codigoPlan, int idUsuario) throws SQLException {
        String codigo = CatalogoPlanes.buscar(codigoPlan).orElseThrow(() -> new IllegalArgumentException("Plan no válido.")).codigo();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("INSERT INTO suscripcion_centro (codigo_plan, cambiado_por) VALUES (?, ?)")) {
            ps.setString(1, codigo);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public int usuariosActivos() throws SQLException {
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM usuarios WHERE activo = 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static boolean tablaFaltante(SQLException e) {
        return "42S02".equals(e.getSQLState()) || e.getErrorCode() == 1146;
    }
}
