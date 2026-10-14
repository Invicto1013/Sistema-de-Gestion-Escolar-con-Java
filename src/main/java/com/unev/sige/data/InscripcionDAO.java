package com.unev.sige.data;

import com.unev.sige.model.Asignatura;
import com.unev.sige.model.Calificacion;
import com.unev.sige.model.DetalleInscripcion;
import com.unev.sige.model.Inscripcion;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Seccion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a MySQL para las inscripciones de estudiantes en secciones
 * (tablas `inscripciones`, `detalle_inscripciones` y la fila vacia de `calificaciones`).
 */
public class InscripcionDAO {

    private static final String SQL_LISTAR_POR_ESTUDIANTE = """
            SELECT i.id_inscripcion, i.fecha_inscripcion, i.periodo_lectivo, i.estado,
                   d.id_detalle_inscripcion, d.estado_materia,
                   s.id_seccion, s.aula, s.horario, s.cupo_maximo,
                   a.id_asignatura, a.codigo_asignatura, a.nombre AS asignatura_nombre,
                   a.creditos, a.descripcion,
                   p.id_profesor, p.codigo_profesor, p.especialidad,
                   u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, u.correo AS profesor_correo,
                   c.id_calificacion, c.nota_parcial1, c.nota_parcial2, c.nota_examen_final,
                   c.nota_final, c.calificacion_literal
            FROM inscripciones i
            JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
            JOIN secciones s ON s.id_seccion = d.id_seccion
            JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
            LEFT JOIN profesores p ON p.id_profesor = s.id_profesor
            LEFT JOIN usuarios u ON u.id_usuario = p.id_profesor
            LEFT JOIN calificaciones c ON c.id_detalle_inscripcion = d.id_detalle_inscripcion
            WHERE i.id_estudiante = ?
            ORDER BY i.periodo_lectivo DESC, i.id_inscripcion, a.codigo_asignatura
            """;

    /**
     * Inscribe al estudiante en una seccion para un periodo lectivo.
     * Todo ocurre en una transaccion (si algo falla no se guarda nada):
     * comprueba el cupo, impide repetir la asignatura en el mismo periodo, reutiliza o crea la
     * inscripcion del periodo, agrega la materia y deja lista su fila de calificaciones.
     *
     * @return el id del detalle de inscripcion creado
     * @throws InscripcionInvalidaException si se incumple una regla (mensaje listo para mostrar)
     */
    public int inscribir(int idEstudiante, int idSeccion, String periodo)
            throws SQLException, InscripcionInvalidaException {
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                int idDetalle = inscribirEnTransaccion(conn, idEstudiante, idSeccion, periodo);
                conn.commit();
                return idDetalle;
            } catch (SQLException | InscripcionInvalidaException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Retira una materia: borra el detalle de inscripcion y, en cascada, su calificacion.
     */
    public void retirar(int idDetalleInscripcion) throws SQLException {
        String sql = "DELETE FROM detalle_inscripciones WHERE id_detalle_inscripcion = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDetalleInscripcion);
            ps.executeUpdate();
        }
    }

    /**
     * Inscripciones del estudiante (una por periodo) con sus materias cargadas:
     * seccion, asignatura y profesor. Las mas recientes primero.
     */
    public List<Inscripcion> listarPorEstudiante(int idEstudiante) throws SQLException {
        return consultarInscripciones(SQL_LISTAR_POR_ESTUDIANTE, idEstudiante, null);
    }

    /** Materias de un estudiante, solo si está vinculado a la cuenta del padre/tutor. */
    public List<Inscripcion> listarParaPadre(int idUsuarioPadre, int idEstudiante) throws SQLException {
        String sql = SQL_LISTAR_POR_ESTUDIANTE
                .replace("FROM inscripciones i", "FROM inscripciones i "
                        + "JOIN estudiante_padre ep ON ep.id_estudiante = i.id_estudiante "
                        + "JOIN padres pa ON pa.id_padre = ep.id_padre")
                .replace("WHERE i.id_estudiante = ?", "WHERE pa.id_usuario = ? AND i.id_estudiante = ?");
        return consultarInscripciones(sql, idUsuarioPadre, idEstudiante);
    }

    private List<Inscripcion> consultarInscripciones(String sql, int primerId, Integer estudianteAdicional)
            throws SQLException {
        Map<Integer, Inscripcion> porId = new LinkedHashMap<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, primerId);
            if (estudianteAdicional != null) ps.setInt(2, estudianteAdicional);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idInscripcion = rs.getInt("id_inscripcion");
                    Inscripcion inscripcion = porId.get(idInscripcion);
                    if (inscripcion == null) {
                        inscripcion = new Inscripcion(idInscripcion,
                                rs.getDate("fecha_inscripcion").toLocalDate(),
                                rs.getString("periodo_lectivo"), rs.getString("estado"), null);
                        porId.put(idInscripcion, inscripcion);
                    }
                    inscripcion.agregarDetalle(mapearDetalle(rs));
                }
            }
        }
        return new ArrayList<>(porId.values());
    }

    // ----- Metodos privados -----

    private int inscribirEnTransaccion(Connection conn, int idEstudiante, int idSeccion, String periodo)
            throws SQLException, InscripcionInvalidaException {
        // 1) Bloquea la seccion para que dos inscripciones simultaneas no superen el cupo.
        int cupo;
        int idAsignatura;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT cupo_maximo, id_asignatura FROM secciones WHERE id_seccion = ? FOR UPDATE")) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new InscripcionInvalidaException("La sección ya no existe.");
                }
                cupo = rs.getInt("cupo_maximo");
                idAsignatura = rs.getInt("id_asignatura");
            }
        }

        // 2) Cupo disponible.
        int inscritos;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM detalle_inscripciones WHERE id_seccion = ?")) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                inscritos = rs.next() ? rs.getInt(1) : 0;
            }
        }
        if (inscritos >= cupo) {
            throw new InscripcionInvalidaException("La sección no tiene cupo disponible (" + inscritos + " / "
                    + cupo + ").");
        }

        // 3) No repetir la misma asignatura (en cualquier seccion) dentro del mismo periodo.
        String sqlRepetida = """
                SELECT 1
                FROM detalle_inscripciones d
                JOIN inscripciones i ON i.id_inscripcion = d.id_inscripcion
                JOIN secciones s ON s.id_seccion = d.id_seccion
                WHERE i.id_estudiante = ? AND i.periodo_lectivo = ? AND s.id_asignatura = ?
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlRepetida)) {
            ps.setInt(1, idEstudiante);
            ps.setString(2, periodo);
            ps.setInt(3, idAsignatura);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new InscripcionInvalidaException(
                            "El estudiante ya está inscrito en esa asignatura en el periodo " + periodo + ".");
                }
            }
        }

        // 4) Reutiliza la inscripcion activa del periodo o crea una nueva.
        int idInscripcion = buscarInscripcionActiva(conn, idEstudiante, periodo);
        if (idInscripcion == 0) {
            idInscripcion = crearInscripcion(conn, idEstudiante, periodo);
        }

        // 5) Agrega la materia (estado_materia queda en "Inscrita" por defecto).
        int idDetalle;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO detalle_inscripciones (id_inscripcion, id_seccion) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idInscripcion);
            ps.setInt(2, idSeccion);
            ps.executeUpdate();
            idDetalle = claveGenerada(ps);
        }

        // 6) Fila de calificaciones lista (todas las notas en 0 por defecto).
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO calificaciones (id_detalle_inscripcion) VALUES (?)")) {
            ps.setInt(1, idDetalle);
            ps.executeUpdate();
        }

        return idDetalle;
    }

    private int buscarInscripcionActiva(Connection conn, int idEstudiante, String periodo) throws SQLException {
        String sql = "SELECT id_inscripcion FROM inscripciones "
                + "WHERE id_estudiante = ? AND periodo_lectivo = ? AND estado = 'Activa' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstudiante);
            ps.setString(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int crearInscripcion(Connection conn, int idEstudiante, String periodo) throws SQLException {
        String sql = "INSERT INTO inscripciones (fecha_inscripcion, periodo_lectivo, id_estudiante) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setString(2, periodo);
            ps.setInt(3, idEstudiante);
            ps.executeUpdate();
            return claveGenerada(ps);
        }
    }

    private int claveGenerada(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("MySQL no devolvio el id generado.");
        }
    }

    private DetalleInscripcion mapearDetalle(ResultSet rs) throws SQLException {
        Asignatura asignatura = new Asignatura(rs.getInt("id_asignatura"),
                rs.getString("codigo_asignatura"), rs.getString("asignatura_nombre"),
                rs.getInt("creditos"), rs.getString("descripcion"));

        Profesor profesor = null;
        int idProfesor = rs.getInt("id_profesor");
        if (!rs.wasNull()) {
            profesor = new Profesor(idProfesor, rs.getString("profesor_nombre"),
                    rs.getString("profesor_apellido"), rs.getString("profesor_correo"), null,
                    rs.getString("codigo_profesor"), rs.getString("especialidad"));
        }

        Seccion seccion = new Seccion(rs.getInt("id_seccion"), rs.getString("aula"), rs.getString("horario"),
                rs.getInt("cupo_maximo"), asignatura, profesor);

        DetalleInscripcion detalle = new DetalleInscripcion(rs.getInt("id_detalle_inscripcion"),
                rs.getString("estado_materia"), seccion);

        int idCalificacion = rs.getInt("id_calificacion");
        if (!rs.wasNull()) {
            detalle.setCalificacion(new Calificacion(idCalificacion, detalle.getId(),
                    Calificacion.limitar(rs.getDouble("nota_parcial1")),
                    Calificacion.limitar(rs.getDouble("nota_parcial2")),
                    Calificacion.limitar(rs.getDouble("nota_examen_final")),
                    Calificacion.limitar(rs.getDouble("nota_final")),
                    rs.getString("calificacion_literal")));
        }
        return detalle;
    }
}
