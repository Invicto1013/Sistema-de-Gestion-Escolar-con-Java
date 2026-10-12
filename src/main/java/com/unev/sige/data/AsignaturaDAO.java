package com.unev.sige.data;

import com.unev.sige.model.Asignatura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a MySQL para la tabla `asignaturas`.
 */
public class AsignaturaDAO {

    public List<Asignatura> listarTodas() throws SQLException {
        String sql = "SELECT id_asignatura, codigo_asignatura, nombre, creditos, descripcion "
                + "FROM asignaturas ORDER BY codigo_asignatura";
        List<Asignatura> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Asignatura(rs.getInt("id_asignatura"), rs.getString("codigo_asignatura"),
                        rs.getString("nombre"), rs.getInt("creditos"), rs.getString("descripcion")));
            }
        }
        return lista;
    }

    /**
     * Indica si ya existe otra asignatura con ese codigo.
     *
     * @param idExcluido id de la asignatura que se esta editando (0 si es nueva)
     */
    public boolean existeCodigo(String codigo, int idExcluido) throws SQLException {
        String sql = "SELECT 1 FROM asignaturas WHERE codigo_asignatura = ? AND id_asignatura <> ? LIMIT 1";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserta la asignatura y deja en el objeto el id generado.
     */
    public void insertar(Asignatura a) throws SQLException {
        String sql = "INSERT INTO asignaturas (codigo_asignatura, nombre, creditos, descripcion) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getCodigo());
            ps.setString(2, a.getNombre());
            ps.setInt(3, a.getCreditos());
            ps.setString(4, a.getDescripcion());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getInt(1));
                }
            }
        }
    }

    public void actualizar(Asignatura a) throws SQLException {
        String sql = "UPDATE asignaturas SET codigo_asignatura = ?, nombre = ?, creditos = ?, descripcion = ? "
                + "WHERE id_asignatura = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getCodigo());
            ps.setString(2, a.getNombre());
            ps.setInt(3, a.getCreditos());
            ps.setString(4, a.getDescripcion());
            ps.setInt(5, a.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Indica si la asignatura tiene secciones. La pantalla lo consulta antes de eliminar,
     * porque en la base de datos borrar una asignatura borraria en cascada sus secciones,
     * y con ellas las inscripciones y calificaciones.
     */
    public boolean tieneSecciones(int idAsignatura) throws SQLException {
        String sql = "SELECT 1 FROM secciones WHERE id_asignatura = ? LIMIT 1";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAsignatura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Elimina la asignatura. Llamar solo si no tiene secciones (ver tieneSecciones).
     */
    public void eliminar(int idAsignatura) throws SQLException {
        String sql = "DELETE FROM asignaturas WHERE id_asignatura = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAsignatura);
            ps.executeUpdate();
        }
    }
}
