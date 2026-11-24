package com.unev.sige.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Eliminación definitiva de una cuenta y de todos sus datos, solo para administradores.
 *
 * Se borra explícitamente, tabla por tabla y en una sola transacción, sin depender de ON DELETE CASCADE:
 * en TiDB las claves foráneas pueden no aplicarse, y varias tablas del aula (aula_materiales, aula_entregas)
 * referencian a usuarios sin cascada. Si algo falla, no se borra nada.
 *
 * La auditoría se conserva: las filas antiguas quedan sin vínculo a la cuenta (id_usuario NULL) y se
 * añade una fila CUENTA_ELIMINADA hecha por el administrador, sin nombre ni correo del eliminado.
 */
public class EliminacionUsuarioDAO {

    /** Resultado de una eliminación: cuenta, rol y total de filas borradas (incluida la cuenta). */
    public record Resultado(int idUsuario, String rol, int filasBorradas) { }

    /** El correo escrito debe coincidir con el de la cuenta (sin distinguir mayúsculas ni espacios en los extremos). */
    public static boolean correoCoincide(String correoReal, String escrito) {
        return correoReal != null && escrito != null && !escrito.isBlank()
                && correoReal.trim().equalsIgnoreCase(escrito.trim());
    }

    public Resultado eliminarComoAdmin(int idAdmin, int idUsuario, String correoEscrito)
            throws SQLException, PermisoDenegadoException {
        if (idAdmin == idUsuario) {
            throw new PermisoDenegadoException("No puedes eliminar tu propia cuenta.");
        }
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                Autorizacion.exigirAdministrador(conn, idAdmin);
                String rol;
                String correo;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT rol, correo FROM usuarios WHERE id_usuario = ? FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new PermisoDenegadoException("La cuenta seleccionada ya no existe.");
                        rol = rs.getString(1);
                        correo = rs.getString(2);
                    }
                }
                if (!correoCoincide(correo, correoEscrito)) {
                    throw new PermisoDenegadoException("El correo escrito no coincide con el de la cuenta. No se eliminó nada.");
                }
                // Quien ejecuta es un administrador activo distinto de la cuenta, así que siempre queda al menos uno:
                // eliminar al "último administrador" no es posible por construcción.

                int filas = 0;

                // 1. Archivos del aula (antes de borrar las filas que los referencian).
                filas += borrar(conn, "DELETE FROM aula_archivos WHERE archivo_key IN "
                        + "(SELECT archivo_key FROM aula_entregas WHERE id_estudiante = ?)", idUsuario);
                filas += borrar(conn, "DELETE FROM aula_archivos WHERE archivo_key IN "
                        + "(SELECT archivo_key FROM aula_materiales WHERE id_profesor = ? AND archivo_key IS NOT NULL)", idUsuario);
                filas += borrar(conn, "DELETE FROM aula_archivos WHERE archivo_key IN "
                        + "(SELECT e.archivo_key FROM aula_entregas e JOIN aula_materiales m ON m.id_material = e.id_material "
                        + "WHERE m.id_profesor = ?)", idUsuario);

                // 2. Aula: entregas propias, entregas recibidas en sus materiales y los materiales.
                filas += borrar(conn, "DELETE FROM aula_entregas WHERE id_estudiante = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM aula_entregas WHERE id_material IN "
                        + "(SELECT id_material FROM aula_materiales WHERE id_profesor = ?)", idUsuario);
                filas += borrar(conn, "DELETE FROM aula_materiales WHERE id_profesor = ?", idUsuario);

                // 3. Conducta y anuncios.
                filas += borrar(conn, "DELETE FROM observaciones_conducta WHERE id_profesor = ? OR id_estudiante = ?", idUsuario, idUsuario);
                filas += borrar(conn, "DELETE FROM anuncios_escolares WHERE id_autor = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM herramienta_registro_datos WHERE id_registro IN "
                        + "(SELECT id_registro FROM herramienta_registros WHERE id_usuario = ?)", idUsuario);
                filas += borrar(conn, "DELETE FROM herramienta_registros WHERE id_usuario = ?", idUsuario);

                // 4. Estudiante: asistencia, notas, inscripciones, historial y vínculos con padres.
                String detallesDelEstudiante = "(SELECT d.id_detalle_inscripcion FROM detalle_inscripciones d "
                        + "JOIN inscripciones i ON i.id_inscripcion = d.id_inscripcion WHERE i.id_estudiante = ?)";
                filas += borrar(conn, "DELETE FROM asistencias WHERE id_detalle_inscripcion IN " + detallesDelEstudiante, idUsuario);
                filas += borrar(conn, "DELETE FROM calificaciones WHERE id_detalle_inscripcion IN " + detallesDelEstudiante, idUsuario);
                filas += borrar(conn, "DELETE FROM detalle_inscripciones WHERE id_inscripcion IN "
                        + "(SELECT id_inscripcion FROM inscripciones WHERE id_estudiante = ?)", idUsuario);
                filas += borrar(conn, "DELETE FROM inscripciones WHERE id_estudiante = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM historial_academico WHERE id_estudiante = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM records_academicos WHERE id_estudiante = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM reportes_academicos WHERE id_estudiante = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM estudiante_padre WHERE id_estudiante = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM estudiantes WHERE id_estudiante = ?", idUsuario);

                // 5. Padre: sus vínculos y su ficha.
                filas += borrar(conn, "DELETE FROM estudiante_padre WHERE id_padre IN "
                        + "(SELECT id_padre FROM padres WHERE id_usuario = ?)", idUsuario);
                filas += borrar(conn, "DELETE FROM padres WHERE id_usuario = ?", idUsuario);

                // 6. Profesor: sus secciones quedan sin profesor (no se borran) y se elimina su ficha.
                borrar(conn, "UPDATE secciones SET id_profesor = NULL WHERE id_profesor = ?", idUsuario);
                filas += borrar(conn, "DELETE FROM profesores WHERE id_profesor = ?", idUsuario);

                // 7. Sesiones y accesos (el correo también está en los intentos fallidos).
                filas += borrar(conn, "DELETE FROM sesiones_web WHERE id_usuario = ? OR id_pendiente = ?", idUsuario, idUsuario);
                filas += borrar(conn, "DELETE FROM controlador_sesiones WHERE id_usuario = ?", idUsuario);
                filas += borrarPorCorreo(conn, "DELETE FROM bitacora_accesos WHERE id_usuario = ? OR LOWER(correo_ingresado) = LOWER(?)",
                        idUsuario, correo);

                // 8. Auditoría: se conserva, pero sin vínculo a la cuenta ni a su correo.
                borrar(conn, "UPDATE periodos_lectivos SET cerrado_por = NULL WHERE cerrado_por = ?", idUsuario);
                borrar(conn, "UPDATE auditoria SET id_usuario = NULL WHERE id_usuario = ?", idUsuario);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE auditoria SET detalle = REPLACE(detalle, ?, '[cuenta eliminada]') WHERE detalle LIKE ?")) {
                    ps.setString(1, correo);
                    ps.setString(2, "%" + correo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
                    ps.executeUpdate();
                }

                // 9. Rastro de la eliminación (solo id y rol, sin datos personales) y la cuenta.
                int total = filas + 1;
                AuditoriaDAO.registrar(conn, idAdmin, "CUENTA_ELIMINADA", "usuario", String.valueOf(idUsuario),
                        "Cuenta " + idUsuario + " (" + rol + ") eliminada con sus datos: " + total + " filas.", null, "OK");
                int borrada = borrar(conn, "DELETE FROM usuarios WHERE id_usuario = ?", idUsuario);
                if (borrada != 1) throw new SQLException("No se pudo eliminar la cuenta " + idUsuario + ".");

                conn.commit();
                return new Resultado(idUsuario, rol, total);
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int borrar(Connection conn, String sql, int... ids) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.length; i++) ps.setInt(i + 1, ids[i]);
            return ps.executeUpdate();
        } catch (SQLException e) {
            if (tablaInexistente(e)) return 0; // instalaciones sin el módulo (por ejemplo, sin aula virtual)
            throw e;
        }
    }

    private static int borrarPorCorreo(Connection conn, String sql, int id, String correo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, correo);
            return ps.executeUpdate();
        } catch (SQLException e) {
            if (tablaInexistente(e)) return 0;
            throw e;
        }
    }

    private static boolean tablaInexistente(SQLException e) {
        return "42S02".equals(e.getSQLState()) || e.getErrorCode() == 1146;
    }
}
