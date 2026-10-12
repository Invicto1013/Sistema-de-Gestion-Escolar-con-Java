package com.unev.sige.data;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.VinculoPadreEstudiante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la relación muchos-a-muchos entre padres/tutores y estudiantes. */
public class VinculacionDAO {
    public List<Padre> listarPadres() throws SQLException {
        String sql = """
                SELECT u.id_usuario, u.nombre, u.apellido, u.correo, p.id_padre,
                       p.telefono_contacto, p.ocupacion
                FROM padres p JOIN usuarios u ON u.id_usuario = p.id_usuario
                ORDER BY u.apellido, u.nombre
                """;
        List<Padre> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Padre(rs.getInt("id_usuario"), rs.getString("nombre"),
                    rs.getString("apellido"), rs.getString("correo"), null, rs.getInt("id_padre"),
                    rs.getString("telefono_contacto"), rs.getString("ocupacion")));
        }
        return lista;
    }

    public List<Estudiante> listarEstudiantes() throws SQLException {
        return new UsuarioDAO().listarEstudiantes();
    }

    public List<VinculoPadreEstudiante> listarVinculos() throws SQLException {
        String sql = """
                SELECT p.id_padre, CONCAT(up.nombre, ' ', up.apellido) AS padre,
                       e.id_estudiante, CONCAT(ue.nombre, ' ', ue.apellido) AS estudiante,
                       e.matricula, ep.parentesco
                FROM estudiante_padre ep
                JOIN padres p ON p.id_padre = ep.id_padre
                JOIN usuarios up ON up.id_usuario = p.id_usuario
                JOIN estudiantes e ON e.id_estudiante = ep.id_estudiante
                JOIN usuarios ue ON ue.id_usuario = e.id_estudiante
                ORDER BY up.apellido, ue.apellido, ue.nombre
                """;
        List<VinculoPadreEstudiante> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new VinculoPadreEstudiante(rs.getInt("id_padre"),
                    rs.getString("padre"), rs.getInt("id_estudiante"), rs.getString("estudiante"),
                    rs.getString("matricula"), rs.getString("parentesco")));
        }
        return lista;
    }

    public List<Estudiante> listarHijos(int idUsuarioPadre) throws SQLException {
        String sql = """
                SELECT e.id_estudiante, u.nombre, u.apellido, u.correo,
                       e.matricula, e.grado, e.promedio_academico
                FROM estudiante_padre ep
                JOIN padres p ON p.id_padre = ep.id_padre
                JOIN estudiantes e ON e.id_estudiante = ep.id_estudiante
                JOIN usuarios u ON u.id_usuario = e.id_estudiante
                WHERE p.id_usuario = ?
                ORDER BY u.apellido, u.nombre
                """;
        List<Estudiante> lista = new ArrayList<>();
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idUsuarioPadre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new Estudiante(rs.getInt("id_estudiante"), rs.getString("nombre"),
                        rs.getString("apellido"), rs.getString("correo"), null, rs.getString("matricula"),
                        rs.getString("grado"), rs.getDouble("promedio_academico")));
            }
        }
        return lista;
    }

    public boolean vincular(int idPadre, int idEstudiante, String parentesco) throws SQLException {
        String sql = "INSERT IGNORE INTO estudiante_padre (id_estudiante, id_padre, parentesco) VALUES (?, ?, ?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEstudiante);
            ps.setInt(2, idPadre);
            ps.setString(3, parentesco == null || parentesco.isBlank() ? "PADRE/MADRE/TUTOR" : parentesco.trim());
            return ps.executeUpdate() == 1;
        }
    }

    public void desvincular(int idPadre, int idEstudiante) throws SQLException {
        String sql = "DELETE FROM estudiante_padre WHERE id_padre = ? AND id_estudiante = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idPadre);
            ps.setInt(2, idEstudiante);
            ps.executeUpdate();
        }
    }
}
