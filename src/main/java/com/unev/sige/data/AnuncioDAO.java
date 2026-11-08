package com.unev.sige.data;

import com.unev.sige.model.Anuncio;
import com.unev.sige.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a MySQL para la tabla `anuncios_escolares`.
 */
public class AnuncioDAO {

    public List<Anuncio> listarParaUsuario(Rol rol) throws SQLException {
        String sql = "SELECT a.id_anuncio, a.titulo, a.contenido, a.id_autor, CONCAT(u.nombre, ' ', u.apellido) AS autor_nombre, "
                + "a.destinatarios, a.prioridad, a.fecha_publicacion, a.activo "
                + "FROM anuncios_escolares a "
                + "JOIN usuarios u ON u.id_usuario = a.id_autor "
                + "WHERE a.activo = 1 AND (a.destinatarios = 'TODOS' OR a.destinatarios = ? OR ? = 'ADMIN') "
                + "ORDER BY CASE a.prioridad WHEN 'URGENTE' THEN 1 WHEN 'IMPORTANTE' THEN 2 ELSE 3 END, "
                + "a.fecha_publicacion DESC LIMIT 50";

        List<Anuncio> lista = new ArrayList<>();
        String rolStr = rol != null ? rol.name() : "TODOS";

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rolStr);
            ps.setString(2, rolStr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<Anuncio> listarTodos() throws SQLException {
        String sql = "SELECT a.id_anuncio, a.titulo, a.contenido, a.id_autor, CONCAT(u.nombre, ' ', u.apellido) AS autor_nombre, "
                + "a.destinatarios, a.prioridad, a.fecha_publicacion, a.activo "
                + "FROM anuncios_escolares a "
                + "JOIN usuarios u ON u.id_usuario = a.id_autor "
                + "ORDER BY a.fecha_publicacion DESC LIMIT 100";

        List<Anuncio> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public int crear(Anuncio anuncio) throws SQLException {
        String sql = "INSERT INTO anuncios_escolares (titulo, contenido, id_autor, destinatarios, prioridad, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, anuncio.getTitulo());
            ps.setString(2, anuncio.getContenido());
            ps.setInt(3, anuncio.getIdAutor());
            ps.setString(4, anuncio.getDestinatarios() != null ? anuncio.getDestinatarios() : "TODOS");
            ps.setString(5, anuncio.getPrioridad() != null ? anuncio.getPrioridad() : "NORMAL");
            ps.setBoolean(6, anuncio.isActivo());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    anuncio.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM anuncios_escolares WHERE id_anuncio = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void cambiarEstado(int id, boolean activo) throws SQLException {
        String sql = "UPDATE anuncios_escolares SET activo = ? WHERE id_anuncio = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Anuncio mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_publicacion");
        LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        return new Anuncio(
                rs.getInt("id_anuncio"),
                rs.getString("titulo"),
                rs.getString("contenido"),
                rs.getInt("id_autor"),
                rs.getString("autor_nombre"),
                rs.getString("destinatarios"),
                rs.getString("prioridad"),
                fecha,
                rs.getBoolean("activo")
        );
    }
}
