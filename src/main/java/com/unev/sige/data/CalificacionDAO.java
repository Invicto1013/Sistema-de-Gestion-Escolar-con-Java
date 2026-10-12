package com.unev.sige.data;

import com.unev.sige.model.Calificacion;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.NotaEstudiante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a MySQL para las calificaciones (tabla `calificaciones`).
 * Al guardar tambien actualiza el estado de la materia y el promedio del estudiante.
 */
public class CalificacionDAO {

    private static final String SQL_LISTAR_POR_SECCION = """
            SELECT d.id_detalle_inscripcion, d.estado_materia, i.periodo_lectivo,
                   u.id_usuario, u.nombre, u.apellido, u.correo,
                   e.matricula, e.grado, e.promedio_academico,
                   c.id_calificacion, c.nota_parcial1, c.nota_parcial2, c.nota_examen_final,
                   c.nota_final, c.calificacion_literal
            FROM detalle_inscripciones d
            JOIN inscripciones i ON i.id_inscripcion = d.id_inscripcion
            JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
            JOIN usuarios u ON u.id_usuario = e.id_estudiante
            LEFT JOIN calificaciones c ON c.id_detalle_inscripcion = d.id_detalle_inscripcion
            WHERE d.id_seccion = ?
            ORDER BY i.periodo_lectivo DESC, u.apellido, u.nombre
            """;

    /**
     * Estudiantes inscritos en una seccion con sus notas (una fila por estudiante y periodo).
     * Si alguno no tuviera fila de calificaciones, se devuelve con las notas en 0.
     */
    public List<NotaEstudiante> listarPorSeccion(int idSeccion) throws SQLException {
        List<NotaEstudiante> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_SECCION)) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Estudiante estudiante = new Estudiante(rs.getInt("id_usuario"), rs.getString("nombre"),
                            rs.getString("apellido"), rs.getString("correo"), null,
                            rs.getString("matricula"), rs.getString("grado"), rs.getDouble("promedio_academico"));

                    int idDetalle = rs.getInt("id_detalle_inscripcion");
                    int idCalificacion = rs.getInt("id_calificacion");

                    Calificacion calificacion;
                    if (rs.wasNull()) {
                        calificacion = new Calificacion(0, idDetalle, 0, 0, 0, 0, "");
                    } else {
                        calificacion = new Calificacion(idCalificacion, idDetalle,
                                Calificacion.limitar(rs.getDouble("nota_parcial1")),
                                Calificacion.limitar(rs.getDouble("nota_parcial2")),
                                Calificacion.limitar(rs.getDouble("nota_examen_final")),
                                Calificacion.limitar(rs.getDouble("nota_final")),
                                rs.getString("calificacion_literal"));
                    }

                    lista.add(new NotaEstudiante(estudiante, rs.getString("periodo_lectivo"), idDetalle,
                            rs.getString("estado_materia"), calificacion));
                }
            }
        }
        return lista;
    }

    /**
     * Guarda las notas de una materia inscrita, a nombre de un profesor.
     * Solo se permite si la seccion de esa materia es del profesor indicado.
     * La nota final y la letra se recalculan aqui con EscalaCalificaciones (se ignoran las que traiga
     * el objeto). En la misma transaccion se actualiza el estado de la materia
     * ("Aprobada" o "Reprobada" cuando ya hay examen final, "Inscrita" mientras no) y el
     * promedio academico del estudiante (promedio de sus materias ya evaluadas).
     *
     * @throws PermisoDenegadoException si la materia no pertenece a una seccion de ese profesor
     */
    public void guardar(Calificacion calificacion, int idProfesor) throws SQLException, PermisoDenegadoException {
        calificacion.recalcular();

        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                guardarEnTransaccion(conn, calificacion, idProfesor);
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ----- Metodos privados -----

    private void guardarEnTransaccion(Connection conn, Calificacion c, int idProfesor)
            throws SQLException, PermisoDenegadoException {
        int idDetalle = c.getIdDetalleInscripcion();

        // 1) La materia debe ser de una seccion de este profesor.
        String sqlPermiso = """
                SELECT 1
                FROM detalle_inscripciones d
                JOIN secciones s ON s.id_seccion = d.id_seccion
                WHERE d.id_detalle_inscripcion = ? AND s.id_profesor = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlPermiso)) {
            ps.setInt(1, idDetalle);
            ps.setInt(2, idProfesor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PermisoDenegadoException("Solo puedes calificar estudiantes de tus propias secciones.");
                }
            }
        }

        // 2) Estudiante dueño de la inscripcion (para actualizar su promedio).
        int idEstudiante;
        String sqlEstudiante = """
                SELECT i.id_estudiante
                FROM detalle_inscripciones d
                JOIN inscripciones i ON i.id_inscripcion = d.id_inscripcion
                WHERE d.id_detalle_inscripcion = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlEstudiante)) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PermisoDenegadoException("La inscripción ya no existe.");
                }
                idEstudiante = rs.getInt(1);
            }
        }

        // 3) Guarda las notas (la fila se crea al inscribir; si faltara, se inserta).
        String sqlActualizar = "UPDATE calificaciones SET nota_parcial1 = ?, nota_parcial2 = ?, "
                + "nota_examen_final = ?, nota_final = ?, calificacion_literal = ? "
                + "WHERE id_detalle_inscripcion = ?";
        int filas;
        try (PreparedStatement ps = conn.prepareStatement(sqlActualizar)) {
            ps.setDouble(1, c.getNotaParcial1());
            ps.setDouble(2, c.getNotaParcial2());
            ps.setDouble(3, c.getNotaExamenFinal());
            ps.setDouble(4, c.getNotaFinal());
            ps.setString(5, c.getCalificacionLiteral());
            ps.setInt(6, idDetalle);
            filas = ps.executeUpdate();
        }
        if (filas == 0) {
            String sqlInsertar = "INSERT INTO calificaciones (nota_parcial1, nota_parcial2, nota_examen_final, "
                    + "nota_final, calificacion_literal, id_detalle_inscripcion) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
                ps.setDouble(1, c.getNotaParcial1());
                ps.setDouble(2, c.getNotaParcial2());
                ps.setDouble(3, c.getNotaExamenFinal());
                ps.setDouble(4, c.getNotaFinal());
                ps.setString(5, c.getCalificacionLiteral());
                ps.setInt(6, idDetalle);
                ps.executeUpdate();
            }
        }

        // 4) Estado de la materia: se decide cuando ya hay examen final.
        String estado = c.getNotaExamenFinal() > 0
                ? (c.esAprobatoria() ? "Aprobada" : "Reprobada")
                : "Inscrita";
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE detalle_inscripciones SET estado_materia = ? WHERE id_detalle_inscripcion = ?")) {
            ps.setString(1, estado);
            ps.setInt(2, idDetalle);
            ps.executeUpdate();
        }

        // 5) Promedio academico del estudiante = promedio de la nota final de sus materias ya evaluadas.
        String sqlPromedio = """
                UPDATE estudiantes
                SET promedio_academico = (
                    SELECT COALESCE(AVG(c.nota_final), 0)
                    FROM calificaciones c
                    JOIN detalle_inscripciones d ON d.id_detalle_inscripcion = c.id_detalle_inscripcion
                    JOIN inscripciones i ON i.id_inscripcion = d.id_inscripcion
                    WHERE i.id_estudiante = ? AND d.estado_materia IN ('Aprobada', 'Reprobada'))
                WHERE id_estudiante = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlPromedio)) {
            ps.setInt(1, idEstudiante);
            ps.setInt(2, idEstudiante);
            ps.executeUpdate();
        }
    }
}
