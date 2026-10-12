package com.unev.sige.data;

import com.unev.sige.model.Asignatura;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Seccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a MySQL para la tabla `secciones` (con su asignatura, su profesor y cuantos inscritos tiene).
 */
public class SeccionDAO {

    private static final String SQL_BASE = """
            SELECT s.id_seccion, s.aula, s.horario, s.cupo_maximo,
                   (SELECT COUNT(*) FROM detalle_inscripciones d WHERE d.id_seccion = s.id_seccion) AS inscritos,
                   a.id_asignatura, a.codigo_asignatura, a.nombre AS asignatura_nombre,
                   a.creditos, a.descripcion,
                   p.id_profesor, p.codigo_profesor, p.especialidad,
                   u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, u.correo AS profesor_correo
            FROM secciones s
            JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
            LEFT JOIN profesores p ON p.id_profesor = s.id_profesor
            LEFT JOIN usuarios u ON u.id_usuario = p.id_profesor
            """;

    private static final String SQL_ORDEN = " ORDER BY a.codigo_asignatura, s.id_seccion";

    public List<Seccion> listarTodas() throws SQLException {
        return consultar(SQL_BASE + SQL_ORDEN, null);
    }

    /**
     * Secciones asignadas a un profesor (el id del profesor es el mismo id de su usuario).
     */
    public List<Seccion> listarPorProfesor(int idProfesor) throws SQLException {
        return consultar(SQL_BASE + " WHERE s.id_profesor = ?" + SQL_ORDEN, idProfesor);
    }

    /**
     * Inserta la seccion y deja en el objeto el id generado. El profesor puede ser null.
     */
    public void insertar(Seccion s) throws SQLException {
        String sql = "INSERT INTO secciones (aula, horario, cupo_maximo, id_asignatura, id_profesor) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignarCampos(ps, s);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setId(keys.getInt(1));
                }
            }
        }
    }

    public void actualizar(Seccion s) throws SQLException {
        String sql = "UPDATE secciones SET aula = ?, horario = ?, cupo_maximo = ?, id_asignatura = ?, "
                + "id_profesor = ? WHERE id_seccion = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            asignarCampos(ps, s);
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Cuenta los estudiantes inscritos en la seccion.
     */
    public int contarInscritos(int idSeccion) throws SQLException {
        String sql = "SELECT COUNT(*) FROM detalle_inscripciones WHERE id_seccion = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Elimina la seccion. Llamar solo si no tiene inscritos (ver contarInscritos): en la base de datos
     * borrarla eliminaria en cascada las inscripciones de esa seccion y sus calificaciones.
     */
    public void eliminar(int idSeccion) throws SQLException {
        String sql = "DELETE FROM secciones WHERE id_seccion = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSeccion);
            ps.executeUpdate();
        }
    }

    // ----- Metodos privados -----

    private List<Seccion> consultar(String sql, Integer parametro) throws SQLException {
        List<Seccion> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private Seccion mapear(ResultSet rs) throws SQLException {
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
        seccion.setInscritos(rs.getInt("inscritos"));
        return seccion;
    }

    private void asignarCampos(PreparedStatement ps, Seccion s) throws SQLException {
        ps.setString(1, s.getAula());
        ps.setString(2, s.getHorario());
        ps.setInt(3, s.getCupoMaximo());
        ps.setInt(4, s.getAsignatura().getId());
        if (s.getProfesor() == null) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, s.getProfesor().getId());
        }
    }
}
