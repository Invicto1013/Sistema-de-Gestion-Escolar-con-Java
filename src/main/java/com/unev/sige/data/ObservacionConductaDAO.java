package com.unev.sige.data;

import com.unev.sige.model.ObservacionConducta;

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
 * Acceso a MySQL para la tabla `observaciones_conducta`.
 */
public class ObservacionConductaDAO {

    public List<ObservacionConducta> listarPorEstudiante(int idEstudiante) throws SQLException {
        String sql = "SELECT o.id_observacion, o.id_estudiante, CONCAT(ue.nombre, ' ', ue.apellido) AS estudiante_nombre, "
                + "e.matricula, o.id_profesor, CONCAT(up.nombre, ' ', up.apellido) AS profesor_nombre, "
                + "o.id_seccion, a.nombre AS asignatura_nombre, o.tipo, o.titulo, o.descripcion, o.fecha "
                + "FROM observaciones_conducta o "
                + "JOIN estudiantes e ON e.id_estudiante = o.id_estudiante "
                + "JOIN usuarios ue ON ue.id_usuario = e.id_estudiante "
                + "JOIN usuarios up ON up.id_usuario = o.id_profesor "
                + "LEFT JOIN secciones s ON s.id_seccion = o.id_seccion "
                + "LEFT JOIN asignaturas a ON a.id_asignatura = s.id_asignatura "
                + "WHERE o.id_estudiante = ? "
                + "ORDER BY o.fecha DESC";

        List<ObservacionConducta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstudiante);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<ObservacionConducta> listarPorProfesor(int idProfesor) throws SQLException {
        String sql = "SELECT o.id_observacion, o.id_estudiante, CONCAT(ue.nombre, ' ', ue.apellido) AS estudiante_nombre, "
                + "e.matricula, o.id_profesor, CONCAT(up.nombre, ' ', up.apellido) AS profesor_nombre, "
                + "o.id_seccion, a.nombre AS asignatura_nombre, o.tipo, o.titulo, o.descripcion, o.fecha "
                + "FROM observaciones_conducta o "
                + "JOIN estudiantes e ON e.id_estudiante = o.id_estudiante "
                + "JOIN usuarios ue ON ue.id_usuario = e.id_estudiante "
                + "JOIN usuarios up ON up.id_usuario = o.id_profesor "
                + "LEFT JOIN secciones s ON s.id_seccion = o.id_seccion "
                + "LEFT JOIN asignaturas a ON a.id_asignatura = s.id_asignatura "
                + "WHERE o.id_profesor = ? "
                + "ORDER BY o.fecha DESC";

        List<ObservacionConducta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProfesor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<ObservacionConducta> listarTodas() throws SQLException {
        String sql = "SELECT o.id_observacion, o.id_estudiante, CONCAT(ue.nombre, ' ', ue.apellido) AS estudiante_nombre, "
                + "e.matricula, o.id_profesor, CONCAT(up.nombre, ' ', up.apellido) AS profesor_nombre, "
                + "o.id_seccion, a.nombre AS asignatura_nombre, o.tipo, o.titulo, o.descripcion, o.fecha "
                + "FROM observaciones_conducta o "
                + "JOIN estudiantes e ON e.id_estudiante = o.id_estudiante "
                + "JOIN usuarios ue ON ue.id_usuario = e.id_estudiante "
                + "JOIN usuarios up ON up.id_usuario = o.id_profesor "
                + "LEFT JOIN secciones s ON s.id_seccion = o.id_seccion "
                + "LEFT JOIN asignaturas a ON a.id_asignatura = s.id_asignatura "
                + "ORDER BY o.fecha DESC LIMIT 200";

        List<ObservacionConducta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public int registrar(ObservacionConducta o) throws SQLException {
        String sql = "INSERT INTO observaciones_conducta (id_estudiante, id_profesor, id_seccion, tipo, titulo, descripcion) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getIdEstudiante());
            ps.setInt(2, o.getIdProfesor());
            if (o.getIdSeccion() != null && o.getIdSeccion() > 0) {
                ps.setInt(3, o.getIdSeccion());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setString(4, o.getTipo());
            ps.setString(5, o.getTitulo());
            ps.setString(6, o.getDescripcion());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    o.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /** Elimina la observación solo si fue registrada por ese profesor. */
    public void eliminar(int idObservacion, int idProfesor) throws SQLException {
        String sql = "DELETE FROM observaciones_conducta WHERE id_observacion = ? AND id_profesor = ?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idObservacion);
            ps.setInt(2, idProfesor);
            ps.executeUpdate();
        }
    }

    public void eliminar(int idObservacion) throws SQLException {
        String sql = "DELETE FROM observaciones_conducta WHERE id_observacion = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idObservacion);
            ps.executeUpdate();
        }
    }

    private ObservacionConducta mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha");
        LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        int idSec = rs.getInt("id_seccion");
        Integer idSeccion = rs.wasNull() ? null : idSec;

        return new ObservacionConducta(
                rs.getInt("id_observacion"),
                rs.getInt("id_estudiante"),
                rs.getString("estudiante_nombre"),
                rs.getString("matricula"),
                rs.getInt("id_profesor"),
                rs.getString("profesor_nombre"),
                idSeccion,
                rs.getString("asignatura_nombre"),
                rs.getString("tipo"),
                rs.getString("titulo"),
                rs.getString("descripcion"),
                fecha
        );
    }
}
