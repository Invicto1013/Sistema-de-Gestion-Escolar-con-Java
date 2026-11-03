package com.unev.sige.data;

import com.unev.sige.model.ReglasPeriodo;
import com.unev.sige.model.ZonaVisual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Auditoria de operaciones criticas (tabla `auditoria`): quien, que, cuando y por que.
 * Es un registro de solo anadir: esta clase nunca modifica ni borra filas. Las operaciones que cambian
 * datos la escriben DENTRO de su misma transaccion, de modo que no puede haber un cambio sin su rastro
 * ni un rastro de un cambio que se deshizo.
 */
public class AuditoriaDAO {

    private static final Logger LOG = Logger.getLogger(AuditoriaDAO.class.getName());

    /** Una fila de la auditoria lista para mostrar. */
    public record Registro(long id, String fecha, Integer idUsuario, String usuario, String rol, String accion,
                           String entidad, String idEntidad, String detalle, String motivo, String resultado) { }

    /** Filtros combinables; los vacios (null o texto en blanco) no filtran. */
    public record Filtro(String texto, String accion, String resultado, LocalDate desde, LocalDate hasta) {
        public static Filtro vacio() {
            return new Filtro(null, null, null, null, null);
        }
    }

    /** Escribe una fila dentro de la transaccion de quien la llama (no hace commit). */
    static void registrar(Connection conn, Integer idUsuario, String accion, String entidad, String idEntidad,
                          String detalle, String motivo, String resultado) throws SQLException {
        String sql = "INSERT INTO auditoria (id_usuario, rol, accion, entidad, id_entidad, detalle, motivo, resultado) "
                + "VALUES (?, (SELECT rol FROM usuarios WHERE id_usuario = ?), ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (idUsuario == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, idUsuario);
                ps.setInt(2, idUsuario);
            }
            ps.setString(3, accion);
            ps.setString(4, entidad);
            ps.setString(5, idEntidad);
            ps.setString(6, ReglasPeriodo.recortar(detalle == null ? "" : detalle, ReglasPeriodo.DETALLE_MAXIMO));
            ps.setString(7, ReglasPeriodo.recortar(motivo, ReglasPeriodo.MOTIVO_MAXIMO));
            ps.setString(8, resultado);
            ps.executeUpdate();
        }
    }

    /**
     * Anota un intento rechazado (por ejemplo, un profesor que quiso editar notas de un periodo cerrado).
     * Se hace en su propia conexion y sin interrumpir el flujo: si no se pudiera escribir, solo se registra
     * en el log, porque el rechazo al usuario es lo importante.
     */
    public void registrarDenegado(Integer idUsuario, String accion, String entidad, String idEntidad, String detalle) {
        try (Connection conn = ConexionBD.obtener()) {
            registrar(conn, idUsuario, accion, entidad, idEntidad, detalle, null, "DENEGADO");
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "No se pudo anotar el intento denegado en la auditoría.", e);
        }
    }

    /** Acciones distintas que existen en la auditoria (para el filtro de la pantalla). */
    public List<String> acciones() throws SQLException {
        List<String> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT accion FROM auditoria ORDER BY accion");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(rs.getString(1));
            }
        }
        return lista;
    }

    public int contar(Filtro filtro) throws SQLException {
        List<Object> parametros = new ArrayList<>();
        String where = condiciones(filtro, parametros);
        String sql = "SELECT COUNT(*) FROM auditoria a LEFT JOIN usuarios u ON u.id_usuario = a.id_usuario" + where;
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Pagina de resultados, la mas reciente primero. */
    public List<Registro> listar(Filtro filtro, int limite, int desplazamiento) throws SQLException {
        List<Object> parametros = new ArrayList<>();
        String where = condiciones(filtro, parametros);
        String sql = "SELECT a.id_auditoria, DATE_FORMAT(" + ZonaVisual.sql("a.fecha") + ", '%Y-%m-%d %H:%i:%s') AS fecha_texto, a.id_usuario, "
                + "CONCAT(u.nombre, ' ', u.apellido) AS usuario, a.rol, a.accion, a.entidad, a.id_entidad, "
                + "a.detalle, a.motivo, a.resultado "
                + "FROM auditoria a LEFT JOIN usuarios u ON u.id_usuario = a.id_usuario" + where
                + " ORDER BY a.id_auditoria DESC LIMIT ? OFFSET ?";
        parametros.add(Math.max(1, Math.min(limite, 5000)));
        parametros.add(Math.max(0, desplazamiento));
        List<Registro> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            asignar(ps, parametros);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idUsuario = rs.getInt("id_usuario");
                    Integer usuarioId = rs.wasNull() ? null : idUsuario;
                    lista.add(new Registro(rs.getLong("id_auditoria"), rs.getString("fecha_texto"), usuarioId,
                            rs.getString("usuario"), rs.getString("rol"), rs.getString("accion"),
                            rs.getString("entidad"), rs.getString("id_entidad"), rs.getString("detalle"),
                            rs.getString("motivo"), rs.getString("resultado")));
                }
            }
        }
        return lista;
    }

    // ----- Metodos privados -----

    /** Arma el WHERE con marcadores ?; los valores van siempre como parametros, nunca concatenados. */
    private static String condiciones(Filtro f, List<Object> parametros) {
        List<String> partes = new ArrayList<>();
        if (f != null) {
            if (f.texto() != null && !f.texto().isBlank()) {
                String patron = "%" + f.texto().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                partes.add("(a.detalle LIKE ? OR a.motivo LIKE ? OR a.id_entidad LIKE ? OR CONCAT(u.nombre, ' ', u.apellido) LIKE ?)");
                for (int i = 0; i < 4; i++) {
                    parametros.add(patron);
                }
            }
            if (f.accion() != null && !f.accion().isBlank()) {
                partes.add("a.accion = ?");
                parametros.add(f.accion().trim());
            }
            if (f.resultado() != null && !f.resultado().isBlank()) {
                partes.add("a.resultado = ?");
                parametros.add(f.resultado().trim());
            }
            if (f.desde() != null) {
                partes.add(ZonaVisual.sql("a.fecha") + " >= ?");
                parametros.add(f.desde() + " 00:00:00");
            }
            if (f.hasta() != null) {
                partes.add(ZonaVisual.sql("a.fecha") + " < DATE_ADD(?, INTERVAL 1 DAY)");
                parametros.add(f.hasta().toString());
            }
        }
        return partes.isEmpty() ? "" : " WHERE " + String.join(" AND ", partes);
    }

    private static void asignar(PreparedStatement ps, List<Object> parametros) throws SQLException {
        for (int i = 0; i < parametros.size(); i++) {
            ps.setObject(i + 1, parametros.get(i));
        }
    }
}
