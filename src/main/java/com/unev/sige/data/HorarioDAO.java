package com.unev.sige.data;

import com.unev.sige.model.HorarioSeccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a MySQL para la tabla `horarios_secciones`.
 */
public class HorarioDAO {

    public List<HorarioSeccion> listarPorSeccion(int idSeccion) throws SQLException {
        String sql = "SELECT h.id_horario, h.id_seccion, h.dia_semana, h.hora_inicio, h.hora_fin, h.aula, "
                + "a.nombre AS asignatura_nombre, CONCAT(u.nombre, ' ', u.apellido) AS profesor_nombre "
                + "FROM horarios_secciones h "
                + "JOIN secciones s ON s.id_seccion = h.id_seccion "
                + "JOIN asignaturas a ON a.id_asignatura = s.id_asignatura "
                + "LEFT JOIN profesores p ON p.id_profesor = s.id_profesor "
                + "LEFT JOIN usuarios u ON u.id_usuario = p.id_profesor "
                + "WHERE h.id_seccion = ? "
                + "ORDER BY FIELD(h.dia_semana, 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'), h.hora_inicio";

        List<HorarioSeccion> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<HorarioSeccion> listarPorProfesor(int idProfesor) throws SQLException {
        String sql = "SELECT h.id_horario, h.id_seccion, h.dia_semana, h.hora_inicio, h.hora_fin, h.aula, "
                + "a.nombre AS asignatura_nombre, CONCAT(u.nombre, ' ', u.apellido) AS profesor_nombre "
                + "FROM horarios_secciones h "
                + "JOIN secciones s ON s.id_seccion = h.id_seccion "
                + "JOIN asignaturas a ON a.id_asignatura = s.id_asignatura "
                + "JOIN profesores p ON p.id_profesor = s.id_profesor "
                + "JOIN usuarios u ON u.id_usuario = p.id_profesor "
                + "WHERE s.id_profesor = ? "
                + "ORDER BY FIELD(h.dia_semana, 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'), h.hora_inicio";

        List<HorarioSeccion> lista = new ArrayList<>();
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

    public List<HorarioSeccion> listarPorEstudiante(int idEstudiante) throws SQLException {
        String sql = "SELECT h.id_horario, h.id_seccion, h.dia_semana, h.hora_inicio, h.hora_fin, h.aula, "
                + "a.nombre AS asignatura_nombre, CONCAT(u.nombre, ' ', u.apellido) AS profesor_nombre "
                + "FROM inscripciones i "
                + "JOIN detalle_inscripciones di ON di.id_inscripcion = i.id_inscripcion "
                + "JOIN secciones s ON s.id_seccion = di.id_seccion "
                + "JOIN horarios_secciones h ON h.id_seccion = s.id_seccion "
                + "JOIN asignaturas a ON a.id_asignatura = s.id_asignatura "
                + "LEFT JOIN profesores p ON p.id_profesor = s.id_profesor "
                + "LEFT JOIN usuarios u ON u.id_usuario = p.id_profesor "
                + "WHERE i.id_estudiante = ? AND i.estado = 'ACTIVA' "
                + "ORDER BY FIELD(h.dia_semana, 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'), h.hora_inicio";

        List<HorarioSeccion> lista = new ArrayList<>();
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

    public boolean hayConflictoProfesor(int idSeccion, String diaSemana, LocalTime inicio, LocalTime fin, int idHorarioExcluir) throws SQLException {
        String sql = "SELECT 1 FROM horarios_secciones h "
                + "JOIN secciones s ON s.id_seccion = h.id_seccion "
                + "WHERE s.id_profesor = (SELECT id_profesor FROM secciones WHERE id_seccion = ?) "
                + "AND h.dia_semana = ? "
                + "AND h.id_horario <> ? "
                + "AND ((h.hora_inicio < ? AND h.hora_fin > ?) OR (h.hora_inicio >= ? AND h.hora_inicio < ?)) "
                + "LIMIT 1";

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            ps.setString(2, diaSemana);
            ps.setInt(3, idHorarioExcluir);
            ps.setTime(4, Time.valueOf(fin));
            ps.setTime(5, Time.valueOf(inicio));
            ps.setTime(6, Time.valueOf(inicio));
            ps.setTime(7, Time.valueOf(fin));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int agregar(HorarioSeccion h) throws SQLException {
        String sql = "INSERT INTO horarios_secciones (id_seccion, dia_semana, hora_inicio, hora_fin, aula) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, h.getIdSeccion());
            ps.setString(2, h.getDiaSemana());
            ps.setTime(3, Time.valueOf(h.getHoraInicio()));
            ps.setTime(4, Time.valueOf(h.getHoraFin()));
            ps.setString(5, h.getAula());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    h.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public void eliminar(int idHorario) throws SQLException {
        String sql = "DELETE FROM horarios_secciones WHERE id_horario = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idHorario);
            ps.executeUpdate();
        }
    }

    private HorarioSeccion mapear(ResultSet rs) throws SQLException {
        Time tIni = rs.getTime("hora_inicio");
        Time tFin = rs.getTime("hora_fin");
        HorarioSeccion h = new HorarioSeccion(
                rs.getInt("id_horario"),
                rs.getInt("id_seccion"),
                rs.getString("dia_semana"),
                tIni != null ? tIni.toLocalTime() : LocalTime.of(8, 0),
                tFin != null ? tFin.toLocalTime() : LocalTime.of(9, 0),
                rs.getString("aula")
        );
        h.setAsignaturaNombre(rs.getString("asignatura_nombre"));
        h.setProfesorNombre(rs.getString("profesor_nombre"));
        h.setSeccionEtiqueta("Sección #" + h.getIdSeccion() + " (" + h.getAsignaturaNombre() + ")");
        return h;
    }
}
