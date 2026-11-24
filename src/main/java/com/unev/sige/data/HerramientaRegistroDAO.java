package com.unev.sige.data;

import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.RegistroHerramienta;
import com.unev.sige.model.Rol;
import com.unev.sige.model.ZonaVisual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda y consulta los cálculos de «Herramientas» (tablas herramienta_registros y herramienta_registro_datos).
 * Los hijos se borran de forma explícita porque en TiDB las claves foráneas pueden no aplicarse.
 */
public class HerramientaRegistroDAO {

    private static final String CABECERA = "SELECT r.id_registro, r.id_usuario, CONCAT(u.nombre, ' ', u.apellido) AS autor, "
            + "r.herramienta, r.titulo, r.resultado, " + ZonaVisual.sql("r.creado_en") + " AS creado "
            + "FROM herramienta_registros r JOIN usuarios u ON u.id_usuario = r.id_usuario ";

    /** Guarda el cálculo y devuelve el número del registro. Lanza IllegalStateException si se llegó al tope. */
    public int guardar(int idUsuario, HerramientaEscolar herramienta, Map<String, String> valores, String resultado) throws SQLException {
        List<RegistroHerramienta.Dato> datos = RegistroHerramienta.datosAGuardar(herramienta, valores);
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM herramienta_registros WHERE id_usuario = ?")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) >= RegistroHerramienta.MAX_POR_USUARIO) {
                            throw new IllegalStateException("Llegaste al límite de " + RegistroHerramienta.MAX_POR_USUARIO
                                    + " registros guardados. Elimina alguno para continuar.");
                        }
                    }
                }
                int id;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO herramienta_registros (id_usuario, herramienta, titulo, resultado) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idUsuario);
                    ps.setString(2, herramienta.id());
                    ps.setString(3, RegistroHerramienta.recortar(herramienta.titulo(), 120));
                    ps.setString(4, RegistroHerramienta.recortar(resultado, RegistroHerramienta.MAX_RESULTADO));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el número del registro.");
                        id = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO herramienta_registro_datos (id_registro, orden, campo, etiqueta, valor) VALUES (?, ?, ?, ?, ?)")) {
                    int orden = 1;
                    for (RegistroHerramienta.Dato d : datos) {
                        ps.setInt(1, id);
                        ps.setInt(2, orden++);
                        ps.setString(3, d.campo());
                        ps.setString(4, d.etiqueta());
                        ps.setString(5, d.valor());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Registros de un usuario, o de todos si {@code verTodos} (administración y dirección). Los más recientes primero. */
    public List<RegistroHerramienta> listar(int idUsuario, boolean verTodos) throws SQLException {
        String sql = CABECERA + (verTodos ? "" : "WHERE r.id_usuario = ? ") + "ORDER BY r.creado_en DESC, r.id_registro DESC LIMIT 200";
        List<RegistroHerramienta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!verTodos) ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs, List.of()));
            }
        }
        return lista;
    }

    /** Un registro con todos sus datos. El permiso de verlo lo decide quien llama con {@code puedeVer}. */
    public Optional<RegistroHerramienta> obtener(int idRegistro) throws SQLException {
        try (Connection conn = ConexionBD.obtener()) {
            RegistroHerramienta base;
            try (PreparedStatement ps = conn.prepareStatement(CABECERA + "WHERE r.id_registro = ?")) {
                ps.setInt(1, idRegistro);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    base = mapear(rs, List.of());
                }
            }
            List<RegistroHerramienta.Dato> datos = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT campo, etiqueta, valor FROM herramienta_registro_datos WHERE id_registro = ? ORDER BY orden")) {
                ps.setInt(1, idRegistro);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) datos.add(new RegistroHerramienta.Dato(rs.getString(1), rs.getString(2), rs.getString(3)));
                }
            }
            return Optional.of(new RegistroHerramienta(base.id(), base.idUsuario(), base.autor(), base.herramienta(),
                    base.titulo(), base.resultado(), base.creado(), datos));
        }
    }

    /** Elimina un registro (y sus datos) si es suyo o si es administrador. Devuelve false si no existe. */
    public boolean eliminar(int idRegistro, int idQuienElimina, Rol rol) throws SQLException, PermisoDenegadoException {
        Optional<RegistroHerramienta> existente = obtener(idRegistro);
        if (existente.isEmpty()) return false;
        RegistroHerramienta r = existente.get();
        if (!r.puedeEliminar(idQuienElimina, rol)) {
            throw new PermisoDenegadoException("Solo el dueño del registro o un administrador puede eliminarlo.");
        }
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM herramienta_registro_datos WHERE id_registro = ?")) {
                    ps.setInt(1, idRegistro);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM herramienta_registros WHERE id_registro = ?")) {
                    ps.setInt(1, idRegistro);
                    ps.executeUpdate();
                }
                if (r.idUsuario() != idQuienElimina) {
                    AuditoriaDAO.registrar(conn, idQuienElimina, "HERRAMIENTA_REGISTRO_ELIMINADO", "herramienta_registro",
                            String.valueOf(idRegistro), "Registro N." + idRegistro + " (" + r.titulo() + ") de " + r.autor() + " eliminado.", null, "OK");
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static RegistroHerramienta mapear(ResultSet rs, List<RegistroHerramienta.Dato> datos) throws SQLException {
        java.sql.Timestamp t = rs.getTimestamp("creado");
        return new RegistroHerramienta(rs.getInt("id_registro"), rs.getInt("id_usuario"), rs.getString("autor"),
                rs.getString("herramienta"), rs.getString("titulo"), rs.getString("resultado"),
                t == null ? null : t.toLocalDateTime(), datos);
    }
}
