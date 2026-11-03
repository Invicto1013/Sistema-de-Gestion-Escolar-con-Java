package com.unev.sige.data;

import com.unev.sige.model.Calificacion;
import com.unev.sige.model.CalificacionHijo;
import com.unev.sige.model.EstadoPeriodo;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.NotaEstudiante;
import com.unev.sige.model.ReglasPeriodo;

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

    /** Notas de un estudiante solo si está vinculado a la cuenta del padre/tutor. */
    public List<CalificacionHijo> listarParaPadre(int idUsuarioPadre, int idEstudiante) throws SQLException {
        String sql = """
                SELECT i.periodo_lectivo, a.codigo_asignatura, a.nombre AS asignatura,
                       c.nota_parcial1, c.nota_parcial2, c.nota_examen_final, c.nota_final,
                       c.calificacion_literal, d.estado_materia
                FROM estudiante_padre ep
                JOIN padres p ON p.id_padre = ep.id_padre
                JOIN estudiantes e ON e.id_estudiante = ep.id_estudiante
                JOIN inscripciones i ON i.id_estudiante = e.id_estudiante
                JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
                JOIN secciones s ON s.id_seccion = d.id_seccion
                JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
                LEFT JOIN calificaciones c ON c.id_detalle_inscripcion = d.id_detalle_inscripcion
                WHERE p.id_usuario = ? AND e.id_estudiante = ?
                ORDER BY i.periodo_lectivo DESC, a.codigo_asignatura
                """;
        List<CalificacionHijo> filas = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuarioPadre);
            ps.setInt(2, idEstudiante);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) filas.add(new CalificacionHijo(rs.getString("periodo_lectivo"),
                        rs.getString("codigo_asignatura"), rs.getString("asignatura"),
                        Calificacion.limitar(rs.getDouble("nota_parcial1")),
                        Calificacion.limitar(rs.getDouble("nota_parcial2")),
                        Calificacion.limitar(rs.getDouble("nota_examen_final")),
                        Calificacion.limitar(rs.getDouble("nota_final")), rs.getString("calificacion_literal"),
                        rs.getString("estado_materia")));
            }
        }
        return filas;
    }

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

    private enum Modo { PROFESOR, ADMIN, CORRECCION }

    /**
     * Guarda las notas de una materia inscrita, a nombre de un profesor.
     * Solo se permite si la seccion de esa materia es del profesor indicado y el periodo esta ABIERTO.
     * La nota final y la letra se recalculan aqui con EscalaCalificaciones (se ignoran las que traiga
     * el objeto). En la misma transaccion se actualiza el estado de la materia
     * ("Aprobada" o "Reprobada" cuando ya hay examen final, "Inscrita" mientras no), el
     * promedio academico del estudiante (promedio de sus materias ya evaluadas) y la auditoria.
     *
     * @throws PermisoDenegadoException si la materia no pertenece a una seccion de ese profesor
     * @throws PeriodoCerradoException si el periodo de esa inscripcion ya esta cerrado
     */
    public void guardar(Calificacion calificacion, int idProfesor) throws SQLException, PermisoDenegadoException {
        guardar(calificacion, idProfesor, Modo.PROFESOR, null);
    }

    /** Guarda notas como administrador (periodo abierto) tras verificar su rol y que la cuenta este activa. */
    public void guardarComoAdministrador(Calificacion calificacion, int idAdmin)
            throws SQLException, PermisoDenegadoException {
        guardar(calificacion, idAdmin, Modo.ADMIN, null);
    }

    /**
     * Correccion excepcional de notas de un periodo CERRADO: solo un administrador, con un motivo obligatorio.
     * La auditoria guarda la nota anterior, la nueva y el motivo; el historial academico del estudiante se actualiza.
     */
    public void corregirNotaCerrada(Calificacion calificacion, int idAdmin, String motivo)
            throws SQLException, PermisoDenegadoException {
        String razon;
        try {
            razon = ReglasPeriodo.validarMotivo(motivo);
        } catch (IllegalArgumentException e) {
            throw new PermisoDenegadoException(e.getMessage());
        }
        guardar(calificacion, idAdmin, Modo.CORRECCION, razon);
    }

    private void guardar(Calificacion calificacion, int idActor, Modo modo, String motivo)
            throws SQLException, PermisoDenegadoException {
        calificacion.recalcular();

        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                guardarEnTransaccion(conn, calificacion, idActor, modo, motivo);
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                if (e instanceof PermisoDenegadoException) {
                    // El rechazo tambien deja rastro (en su propia conexion, ya sin la transaccion revertida).
                    new AuditoriaDAO().registrarDenegado(idActor,
                            modo == Modo.CORRECCION ? "NOTA_CORRECCION_DENEGADA" : "NOTA_DENEGADA", "calificacion",
                            String.valueOf(calificacion.getIdDetalleInscripcion()), e.getMessage());
                }
                throw e;
            }
        }
    }

    // ----- Metodos privados -----

    private void guardarEnTransaccion(Connection conn, Calificacion c, int idActor, Modo modo, String motivo)
            throws SQLException, PermisoDenegadoException {
        int idDetalle = c.getIdDetalleInscripcion();

        // 1) Verifica que el actor tenga permiso para guardar estas notas.
        if (modo == Modo.PROFESOR) {
            String sqlPermiso = """
                    SELECT 1
                    FROM detalle_inscripciones d
                    JOIN secciones s ON s.id_seccion = d.id_seccion
                    WHERE d.id_detalle_inscripcion = ? AND s.id_profesor = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sqlPermiso)) {
                ps.setInt(1, idDetalle);
                ps.setInt(2, idActor);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new PermisoDenegadoException("Solo puedes calificar estudiantes de tus propias secciones.");
                    }
                }
            }
        } else {
            Autorizacion.exigirAdministrador(conn, idActor);
        }

        // 2) Estudiante y periodo de la inscripcion (para el promedio, el bloqueo y la auditoria).
        int idEstudiante;
        String periodo;
        String sqlEstudiante = """
                SELECT i.id_estudiante, i.periodo_lectivo
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
                periodo = rs.getString(2);
            }
        }

        // 2b) Periodo cerrado: las notas estan bloqueadas salvo la correccion excepcional del administrador.
        EstadoPeriodo estadoPeriodo = PeriodoDAO.estadoCompartido(conn, periodo).orElse(EstadoPeriodo.ABIERTO);
        if (modo == Modo.CORRECCION) {
            if (estadoPeriodo != EstadoPeriodo.CERRADO) {
                throw new PermisoDenegadoException("El período " + periodo
                        + " está abierto: registra la nota de forma normal, sin corrección excepcional.");
            }
        } else if (estadoPeriodo == EstadoPeriodo.CERRADO) {
            throw new PeriodoCerradoException("El período " + periodo + " está cerrado y sus notas están bloqueadas. "
                    + "Solo un administrador puede hacer una corrección excepcional, con motivo.");
        }

        // 2c) Notas actuales (para la auditoria); la fila queda bloqueada hasta el commit.
        Calificacion antes = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nota_parcial1, nota_parcial2, nota_examen_final, nota_final FROM calificaciones "
                        + "WHERE id_detalle_inscripcion = ? FOR UPDATE")) {
            ps.setInt(1, idDetalle);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    antes = new Calificacion(0, idDetalle, Calificacion.limitar(rs.getDouble(1)),
                            Calificacion.limitar(rs.getDouble(2)), Calificacion.limitar(rs.getDouble(3)),
                            Calificacion.limitar(rs.getDouble(4)), "");
                }
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

        // 6) Auditoria (misma transaccion): quien, que cambio y, en una correccion, por que.
        if (modo == Modo.CORRECCION || ReglasPeriodo.hayCambios(antes, c)) {
            AuditoriaDAO.registrar(conn, idActor, modo == Modo.CORRECCION ? "NOTA_CORREGIDA" : "NOTA_GUARDADA",
                    "calificacion", String.valueOf(idDetalle),
                    "Período " + periodo + ", estudiante " + idEstudiante + ": " + ReglasPeriodo.describirCambio(antes, c),
                    motivo, "OK");
        }
        if (modo == Modo.CORRECCION) {
            PeriodoDAO.actualizarHistorial(conn, periodo, idEstudiante);
        }
    }
}
