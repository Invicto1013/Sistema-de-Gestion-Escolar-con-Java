package com.unev.sige.data;

import com.unev.sige.model.LineaReporteInscripcion;
import com.unev.sige.model.Rol;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas de informes con autorización y filtros de alcance por rol en la capa de datos. */
public class ReporteDAO {

    public List<LineaReporteInscripcion> listarInscripciones(LocalDate desde, LocalDate hasta,
            Rol rolSolicitante, int idSolicitante) throws SQLException, PermisoDenegadoException {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new PermisoDenegadoException("El rango de fechas no es válido.");
        }
        if (rolSolicitante == null) {
            throw new PermisoDenegadoException("No se pudo determinar el rol de la sesión.");
        }

        String sql = """
                SELECT i.fecha_inscripcion, CONCAT(ue.nombre, ' ', ue.apellido) AS estudiante,
                       e.matricula, i.periodo_lectivo, a.codigo_asignatura, a.nombre AS asignatura,
                       s.id_seccion, cg.nombre AS grado, ar.nombre AS area, d.estado_materia
                FROM inscripciones i
                JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
                JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
                JOIN usuarios ue ON ue.id_usuario = e.id_estudiante
                JOIN secciones s ON s.id_seccion = d.id_seccion
                JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
                LEFT JOIN curriculo_grados cg ON cg.id = s.id_grado
                LEFT JOIN asignatura_grado_area aga ON aga.id_asignatura = a.id_asignatura AND aga.id_grado = s.id_grado
                LEFT JOIN areas_curriculares ar ON ar.id = aga.id_area
                WHERE i.fecha_inscripcion BETWEEN ? AND ?
                """;
        sql += switch (rolSolicitante) {
            case ADMIN -> "";
            case PROFESOR -> " AND s.id_profesor = ? ";
            case ESTUDIANTE -> " AND i.id_estudiante = ? ";
            case PADRE -> """
                     AND EXISTS (
                         SELECT 1 FROM padres p
                         JOIN estudiante_padre ep ON ep.id_padre = p.id_padre
                         WHERE p.id_usuario = ? AND ep.id_estudiante = i.id_estudiante
                     )
                    """;
        };
        sql += " ORDER BY i.fecha_inscripcion DESC, ue.apellido, ue.nombre, a.codigo_asignatura";

        List<LineaReporteInscripcion> filas = new ArrayList<>();
        try (Connection c = ConexionBD.obtener()) {
            validarRol(c, rolSolicitante, idSolicitante);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(desde));
                ps.setDate(2, Date.valueOf(hasta));
                if (rolSolicitante != Rol.ADMIN) ps.setInt(3, idSolicitante);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        filas.add(new LineaReporteInscripcion(rs.getDate("fecha_inscripcion").toLocalDate(),
                                rs.getString("estudiante"), rs.getString("matricula"),
                                rs.getString("periodo_lectivo"), rs.getString("codigo_asignatura") + " - "
                                        + rs.getString("asignatura"), rs.getString("grado"), rs.getString("area"),
                                rs.getInt("id_seccion"), rs.getString("estado_materia")));
                    }
                }
            }
        }
        return filas;
    }

    private void validarRol(Connection c, Rol rolSolicitante, int idSolicitante)
            throws SQLException, PermisoDenegadoException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT rol FROM usuarios WHERE id_usuario = ? AND activo = 1")) {
            ps.setInt(1, idSolicitante);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rolSolicitante.name().equals(rs.getString("rol"))) {
                    throw new PermisoDenegadoException("El rol de la sesión ya no coincide con la cuenta.");
                }
            }
        }
    }
}
