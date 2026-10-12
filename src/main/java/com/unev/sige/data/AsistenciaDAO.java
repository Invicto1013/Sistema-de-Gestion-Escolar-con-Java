package com.unev.sige.data;

import com.unev.sige.model.FilaAsistencia;
import com.unev.sige.model.ResumenAsistencia;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso JDBC a la tabla asistencias. */
public class AsistenciaDAO {

    /** Estudiantes inscritos en una sección y estado guardado para la fecha elegida. */
    public List<FilaAsistencia> listarPorSeccionYFecha(int idSeccion, java.time.LocalDate fecha)
            throws SQLException {
        String sql = """
                SELECT d.id_detalle_inscripcion, e.matricula, u.nombre, u.apellido, a.presente
                FROM detalle_inscripciones d
                JOIN inscripciones i ON i.id_inscripcion = d.id_inscripcion
                JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
                JOIN usuarios u ON u.id_usuario = e.id_estudiante
                LEFT JOIN asistencias a ON a.id_detalle_inscripcion = d.id_detalle_inscripcion AND a.fecha = ?
                WHERE d.id_seccion = ?
                ORDER BY u.apellido, u.nombre
                """;
        List<FilaAsistencia> filas = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ps.setInt(2, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean presente = rs.getBoolean("presente");
                    Boolean estado = rs.wasNull() ? null : presente;
                    filas.add(new FilaAsistencia(rs.getInt("id_detalle_inscripcion"),
                            rs.getString("matricula"), rs.getString("apellido") + ", " + rs.getString("nombre"),
                            fecha, estado));
                }
            }
        }
        return filas;
    }

    /** Guarda o actualiza la lista de una fecha; solo acepta registros de secciones del profesor. */
    public void guardarPorSeccion(int idSeccion, int idProfesor, java.time.LocalDate fecha,
                                  List<FilaAsistencia> filas) throws SQLException {
        String permiso = "SELECT 1 FROM secciones WHERE id_seccion = ? AND id_profesor = ? FOR UPDATE";
        String validaDetalle = "SELECT 1 FROM detalle_inscripciones WHERE id_detalle_inscripcion = ? AND id_seccion = ?";
        String upsert = "INSERT INTO asistencias (id_detalle_inscripcion, fecha, presente) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE presente = VALUES(presente)";
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(permiso)) {
                    ps.setInt(1, idSeccion);
                    ps.setInt(2, idProfesor);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Solo puedes tomar asistencia de tus propias secciones.");
                    }
                }
                try (PreparedStatement valida = conn.prepareStatement(validaDetalle);
                     PreparedStatement guarda = conn.prepareStatement(upsert)) {
                    for (FilaAsistencia fila : filas) {
                        if (fila.presente() == null) continue;
                        valida.setInt(1, fila.idDetalleInscripcion());
                        valida.setInt(2, idSeccion);
                        try (ResultSet rs = valida.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Hay un estudiante que ya no pertenece a esta sección.");
                        }
                        guarda.setInt(1, fila.idDetalleInscripcion());
                        guarda.setDate(2, Date.valueOf(fecha));
                        guarda.setBoolean(3, fila.presente());
                        guarda.addBatch();
                    }
                    guarda.executeBatch();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Resumen por materia inscrita, usando como denominador los días con lista registrada. */
    public List<ResumenAsistencia> listarResumenPorEstudiante(int idEstudiante) throws SQLException {
        String sql = """
                SELECT i.periodo_lectivo, a.codigo_asignatura, a.nombre AS asignatura,
                       COUNT(x.id_asistencia) AS clases_registradas,
                       COALESCE(SUM(CASE WHEN x.presente = 1 THEN 1 ELSE 0 END), 0) AS presentes,
                       COALESCE(SUM(CASE WHEN x.presente = 0 THEN 1 ELSE 0 END), 0) AS ausentes
                FROM inscripciones i
                JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
                JOIN secciones s ON s.id_seccion = d.id_seccion
                JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
                LEFT JOIN asistencias x ON x.id_detalle_inscripcion = d.id_detalle_inscripcion
                WHERE i.id_estudiante = ?
                GROUP BY d.id_detalle_inscripcion, i.periodo_lectivo, a.codigo_asignatura, a.nombre
                ORDER BY i.periodo_lectivo DESC, a.codigo_asignatura
                """;
        List<ResumenAsistencia> resumen = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstudiante);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resumen.add(new ResumenAsistencia(rs.getString("periodo_lectivo"),
                        rs.getString("codigo_asignatura"), rs.getString("asignatura"),
                        rs.getInt("clases_registradas"), rs.getInt("presentes"), rs.getInt("ausentes")));
            }
        }
        return resumen;
    }

    /** Asistencia del estudiante solo cuando existe vínculo con la cuenta del padre/tutor. */
    public List<ResumenAsistencia> listarResumenParaPadre(int idUsuarioPadre, int idEstudiante)
            throws SQLException {
        String sql = """
                SELECT i.periodo_lectivo, a.codigo_asignatura, a.nombre AS asignatura,
                       COUNT(x.id_asistencia) AS clases_registradas,
                       COALESCE(SUM(CASE WHEN x.presente = 1 THEN 1 ELSE 0 END), 0) AS presentes,
                       COALESCE(SUM(CASE WHEN x.presente = 0 THEN 1 ELSE 0 END), 0) AS ausentes
                FROM estudiante_padre ep
                JOIN padres p ON p.id_padre = ep.id_padre
                JOIN inscripciones i ON i.id_estudiante = ep.id_estudiante
                JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
                JOIN secciones s ON s.id_seccion = d.id_seccion
                JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
                LEFT JOIN asistencias x ON x.id_detalle_inscripcion = d.id_detalle_inscripcion
                WHERE p.id_usuario = ? AND ep.id_estudiante = ?
                GROUP BY d.id_detalle_inscripcion, i.periodo_lectivo, a.codigo_asignatura, a.nombre
                ORDER BY i.periodo_lectivo DESC, a.codigo_asignatura
                """;
        List<ResumenAsistencia> resumen = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuarioPadre);
            ps.setInt(2, idEstudiante);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resumen.add(new ResumenAsistencia(rs.getString("periodo_lectivo"),
                        rs.getString("codigo_asignatura"), rs.getString("asignatura"),
                        rs.getInt("clases_registradas"), rs.getInt("presentes"), rs.getInt("ausentes")));
            }
        }
        return resumen;
    }
}
