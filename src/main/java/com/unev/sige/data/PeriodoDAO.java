package com.unev.sige.data;

import com.unev.sige.model.EstadoPeriodo;
import com.unev.sige.model.Periodo;
import com.unev.sige.model.ReglasPeriodo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Periodos lectivos (tabla `periodos_lectivos`): crear, listar, cerrar y reabrir.
 * Cerrar un periodo es una sola transaccion: bloquea la fila del periodo, guarda el resultado de cada
 * estudiante en `historial_academico`, marca el periodo como CERRADO y deja el rastro en `auditoria`.
 * Mientras esa fila esta bloqueada, nadie puede guardar notas ni inscribir en ese periodo (ver
 * {@link #estadoCompartido}), asi que el cierre no se mezcla con una edicion a medias.
 */
public class PeriodoDAO {

    /** Resultado del cierre (o de su vista previa): cuantos estudiantes quedaron en cada situacion. */
    public record ResumenCierre(int estudiantes, int aprobados, int reprobados, int incompletos) {
        public String texto() {
            return estudiantes + " estudiante(s): " + aprobados + " aprobado(s), " + reprobados
                    + " reprobado(s), " + incompletos + " incompleto(s)";
        }
    }

    private record Fila(int idEstudiante, int total, int aprobadas, int reprobadas, BigDecimal promedio) { }

    private static final String SQL_BASE = """
            SELECT p.id_periodo, p.nombre, p.fecha_inicio, p.fecha_fin, p.estado,
                   CONCAT(u.nombre, ' ', u.apellido) AS cerrado_por,
                   DATE_FORMAT(p.fecha_cierre, '%Y-%m-%d %H:%i') AS fecha_cierre_texto
            FROM periodos_lectivos p
            LEFT JOIN usuarios u ON u.id_usuario = p.cerrado_por
            """;

    public List<Periodo> listar() throws SQLException {
        return consultar(SQL_BASE + " ORDER BY p.nombre DESC");
    }

    /** Periodos que aceptan inscripciones y notas, los mas recientes primero. */
    public List<Periodo> abiertos() throws SQLException {
        return consultar(SQL_BASE + " WHERE p.estado = 'ABIERTO' ORDER BY p.nombre DESC");
    }

    public Optional<Periodo> buscar(int idPeriodo) throws SQLException {
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(SQL_BASE + " WHERE p.id_periodo = ?")) {
            ps.setInt(1, idPeriodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Crea un periodo ABIERTO. Solo administradores; el nombre no puede repetirse. */
    public int crear(String nombre, LocalDate inicio, LocalDate fin, int idAdmin)
            throws SQLException, PermisoDenegadoException {
        String limpio = ReglasPeriodo.validarNombre(nombre);
        ReglasPeriodo.validarFechas(inicio, fin);
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                Autorizacion.exigirAdministrador(conn, idAdmin);
                int id;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO periodos_lectivos (nombre, fecha_inicio, fecha_fin) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, limpio);
                    ps.setDate(2, inicio == null ? null : Date.valueOf(inicio));
                    ps.setDate(3, fin == null ? null : Date.valueOf(fin));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("MySQL no devolvió el id del período.");
                        }
                        id = keys.getInt(1);
                    }
                } catch (SQLIntegrityConstraintViolationException e) {
                    throw new PermisoDenegadoException("Ya existe un período llamado " + limpio + ".");
                }
                AuditoriaDAO.registrar(conn, idAdmin, "PERIODO_CREADO", "periodo", String.valueOf(id),
                        "Período " + limpio + " creado" + rango(inicio, fin) + ".", null, "OK");
                conn.commit();
                return id;
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Vista previa del cierre: el mismo calculo, sin guardar nada. */
    public ResumenCierre vistaPrevia(int idPeriodo) throws SQLException, PermisoDenegadoException {
        try (Connection conn = ConexionBD.obtener()) {
            String nombre = nombreDe(conn, idPeriodo);
            return resumir(agregar(conn, nombre, null));
        }
    }

    /**
     * Cierra el periodo: guarda el historial de cada estudiante y bloquea notas e inscripciones.
     *
     * @throws PermisoDenegadoException si no es administrador activo o el periodo ya estaba cerrado
     */
    public ResumenCierre cerrar(int idPeriodo, int idAdmin) throws SQLException, PermisoDenegadoException {
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                Autorizacion.exigirAdministrador(conn, idAdmin);
                Object[] fila = bloquear(conn, idPeriodo);
                String nombre = (String) fila[0];
                if (EstadoPeriodo.desde((String) fila[1]) == EstadoPeriodo.CERRADO) {
                    throw new PermisoDenegadoException("El período " + nombre + " ya está cerrado.");
                }
                List<Fila> filas = agregar(conn, nombre, null);
                for (Fila f : filas) {
                    guardarHistorial(conn, idPeriodo, f);
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE periodos_lectivos SET estado = 'CERRADO', cerrado_por = ?, "
                                + "fecha_cierre = CURRENT_TIMESTAMP WHERE id_periodo = ?")) {
                    ps.setInt(1, idAdmin);
                    ps.setInt(2, idPeriodo);
                    ps.executeUpdate();
                }
                ResumenCierre resumen = resumir(filas);
                AuditoriaDAO.registrar(conn, idAdmin, "PERIODO_CERRADO", "periodo", String.valueOf(idPeriodo),
                        "Período " + nombre + " cerrado: " + resumen.texto() + ".", null, "OK");
                conn.commit();
                return resumen;
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Reabre un periodo cerrado. Solo administradores y con motivo, que queda en la auditoria. */
    public void reabrir(int idPeriodo, int idAdmin, String motivo) throws SQLException, PermisoDenegadoException {
        String razon = ReglasPeriodo.validarMotivo(motivo);
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                Autorizacion.exigirAdministrador(conn, idAdmin);
                Object[] fila = bloquear(conn, idPeriodo);
                String nombre = (String) fila[0];
                if (EstadoPeriodo.desde((String) fila[1]) != EstadoPeriodo.CERRADO) {
                    throw new PermisoDenegadoException("El período " + nombre + " ya está abierto.");
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE periodos_lectivos SET estado = 'ABIERTO', cerrado_por = NULL, fecha_cierre = NULL "
                                + "WHERE id_periodo = ?")) {
                    ps.setInt(1, idPeriodo);
                    ps.executeUpdate();
                }
                AuditoriaDAO.registrar(conn, idAdmin, "PERIODO_REABIERTO", "periodo", String.valueOf(idPeriodo),
                        "Período " + nombre + " reabierto.", razon, "OK");
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ----- Uso compartido con CalificacionDAO e InscripcionDAO -----

    /**
     * Estado del periodo con un bloqueo de lectura compartido (FOR SHARE) hasta el final de la transaccion:
     * si alguien esta cerrando ese periodo, espera a que termine; y mientras una edicion sigue abierta, el
     * cierre espera. Un periodo que no esta en la tabla (datos antiguos) cuenta como abierto.
     */
    static Optional<EstadoPeriodo> estadoCompartido(Connection conn, String nombrePeriodo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT estado FROM periodos_lectivos WHERE nombre = ? FOR SHARE")) {
            ps.setString(1, nombrePeriodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(EstadoPeriodo.desde(rs.getString(1))) : Optional.empty();
            }
        }
    }

    /** Vuelve a guardar el resultado de un estudiante en un periodo cerrado (tras una correccion excepcional). */
    static void actualizarHistorial(Connection conn, String nombrePeriodo, int idEstudiante) throws SQLException {
        int idPeriodo;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id_periodo FROM periodos_lectivos WHERE nombre = ?")) {
            ps.setString(1, nombrePeriodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                idPeriodo = rs.getInt(1);
            }
        }
        for (Fila f : agregar(conn, nombrePeriodo, idEstudiante)) {
            guardarHistorial(conn, idPeriodo, f);
        }
    }

    // ----- Metodos privados -----

    private Object[] bloquear(Connection conn, int idPeriodo) throws SQLException, PermisoDenegadoException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nombre, estado FROM periodos_lectivos WHERE id_periodo = ? FOR UPDATE")) {
            ps.setInt(1, idPeriodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PermisoDenegadoException("El período ya no existe.");
                }
                return new Object[] {rs.getString(1), rs.getString(2)};
            }
        }
    }

    private String nombreDe(Connection conn, int idPeriodo) throws SQLException, PermisoDenegadoException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM periodos_lectivos WHERE id_periodo = ?")) {
            ps.setInt(1, idPeriodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PermisoDenegadoException("El período ya no existe.");
                }
                return rs.getString(1);
            }
        }
    }

    /** Totales por estudiante de las materias del periodo (todos, o solo el estudiante indicado). */
    private static List<Fila> agregar(Connection conn, String nombrePeriodo, Integer idEstudiante) throws SQLException {
        String sql = """
                SELECT i.id_estudiante,
                       COUNT(d.id_detalle_inscripcion) AS total,
                       COALESCE(SUM(d.estado_materia = 'Aprobada'), 0) AS aprobadas,
                       COALESCE(SUM(d.estado_materia = 'Reprobada'), 0) AS reprobadas,
                       COALESCE(AVG(CASE WHEN d.estado_materia IN ('Aprobada', 'Reprobada')
                                         THEN c.nota_final END), 0) AS promedio
                FROM inscripciones i
                JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
                LEFT JOIN calificaciones c ON c.id_detalle_inscripcion = d.id_detalle_inscripcion
                WHERE i.periodo_lectivo = ?""" + (idEstudiante == null ? "" : " AND i.id_estudiante = ?")
                + " GROUP BY i.id_estudiante ORDER BY i.id_estudiante";
        List<Fila> filas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombrePeriodo);
            if (idEstudiante != null) {
                ps.setInt(2, idEstudiante);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Fila(rs.getInt("id_estudiante"), rs.getInt("total"), rs.getInt("aprobadas"),
                            rs.getInt("reprobadas"),
                            BigDecimal.valueOf(rs.getDouble("promedio")).setScale(2, RoundingMode.HALF_UP)));
                }
            }
        }
        return filas;
    }

    private static void guardarHistorial(Connection conn, int idPeriodo, Fila f) throws SQLException {
        String sql = "INSERT INTO historial_academico (id_estudiante, id_periodo, materias_total, materias_aprobadas, "
                + "materias_reprobadas, promedio, resultado) VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE materias_total = VALUES(materias_total), "
                + "materias_aprobadas = VALUES(materias_aprobadas), materias_reprobadas = VALUES(materias_reprobadas), "
                + "promedio = VALUES(promedio), resultado = VALUES(resultado), fecha_registro = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, f.idEstudiante());
            ps.setInt(2, idPeriodo);
            ps.setInt(3, f.total());
            ps.setInt(4, f.aprobadas());
            ps.setInt(5, f.reprobadas());
            ps.setBigDecimal(6, f.promedio());
            ps.setString(7, ReglasPeriodo.resultado(f.total(), f.aprobadas(), f.reprobadas()));
            ps.executeUpdate();
        }
    }

    private static ResumenCierre resumir(List<Fila> filas) {
        int aprobados = 0;
        int reprobados = 0;
        int incompletos = 0;
        for (Fila f : filas) {
            switch (ReglasPeriodo.resultado(f.total(), f.aprobadas(), f.reprobadas())) {
                case "APROBADO" -> aprobados++;
                case "REPROBADO" -> reprobados++;
                default -> incompletos++;
            }
        }
        return new ResumenCierre(filas.size(), aprobados, reprobados, incompletos);
    }

    private static String rango(LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) {
            return "";
        }
        return " (" + (inicio == null ? "?" : inicio) + " a " + (fin == null ? "?" : fin) + ")";
    }

    private List<Periodo> consultar(String sql) throws SQLException {
        List<Periodo> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private static Periodo mapear(ResultSet rs) throws SQLException {
        Date inicio = rs.getDate("fecha_inicio");
        Date fin = rs.getDate("fecha_fin");
        return new Periodo(rs.getInt("id_periodo"), rs.getString("nombre"),
                inicio == null ? null : inicio.toLocalDate(), fin == null ? null : fin.toLocalDate(),
                EstadoPeriodo.desde(rs.getString("estado")), rs.getString("cerrado_por"),
                rs.getString("fecha_cierre_texto"));
    }
}
