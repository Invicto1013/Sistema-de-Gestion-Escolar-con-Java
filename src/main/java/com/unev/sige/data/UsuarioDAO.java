package com.unev.sige.data;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.PoliticaContrasena;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Acceso a MySQL para usuarios (tablas usuarios, estudiantes, profesores y padres).
 * Reemplaza al antiguo UsuarioRepositorio en memoria.
 */
public class UsuarioDAO {

    private static final String SQL_BUSCAR_POR_CORREO = """
            SELECT u.id_usuario, u.nombre, u.apellido, u.correo, u.contrasena, u.rol, u.activo,
                   u.debe_cambiar_contrasena,
                   e.matricula, e.grado, e.promedio_academico,
                   p.codigo_profesor, p.especialidad,
                   pa.id_padre, pa.telefono_contacto, pa.ocupacion
            FROM usuarios u
            LEFT JOIN estudiantes e ON e.id_estudiante = u.id_usuario
            LEFT JOIN profesores p ON p.id_profesor = u.id_usuario
            LEFT JOIN padres pa ON pa.id_usuario = u.id_usuario
            WHERE u.correo = ?
            LIMIT 1
            """;

    /**
     * Busca un usuario por correo. Devuelve la subclase que corresponde a su rol
     * (Estudiante, Profesor, Padre o Usuario para ADMIN).
     */
    public Optional<Usuario> buscarPorCorreo(String correo) throws SQLException {
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_CORREO)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        }
    }

    private static final String SQL_BUSCAR_POR_ID =
            SQL_BUSCAR_POR_CORREO.replace("WHERE u.correo = ?", "WHERE u.id_usuario = ?");

    /** Busca un usuario por su id (la web lo usa para restaurar sesiones guardadas). */
    public Optional<Usuario> buscarPorId(int idUsuario) throws SQLException {
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public boolean existeCorreo(String correo) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE correo = ? LIMIT 1", correo);
    }

    public boolean existeMatricula(String matricula) throws SQLException {
        return existe("SELECT 1 FROM estudiantes WHERE matricula = ? LIMIT 1", matricula);
    }

    public boolean existeCodigoProfesor(String codigo) throws SQLException {
        return existe("SELECT 1 FROM profesores WHERE codigo_profesor = ? LIMIT 1", codigo);
    }

    /**
     * Guarda un usuario nuevo con su contrasena hasheada (BCrypt).
     * Segun la subclase tambien inserta en estudiantes, profesores o padres.
     * Todo ocurre en una transaccion: si algo falla, no se guarda nada.
     * Al terminar, el objeto recibido queda con su id (y idPadre) generado.
     */
    public void registrar(Usuario usuario, String contrasenaPlana) throws SQLException {
        registrarInterno(usuario, contrasenaPlana, false);
    }

    /**
     * Registro público (web o escritorio) cuando la institución exige aprobación: la cuenta se crea
     * inactiva y marcada como pendiente hasta que un administrador la apruebe.
     */
    public void registrarSolicitud(Usuario usuario, String contrasenaPlana) throws SQLException {
        registrarInterno(usuario, contrasenaPlana, true);
    }

    /**
     * Indica si los registros públicos deben ser aprobados por un administrador. Por defecto sí;
     * se desactiva con SIGE_REGISTRO_REQUIERE_APROBACION=false (o registro.requiere.aprobacion=false).
     */
    public static boolean registroRequiereAprobacion() {
        String valor = ConexionBD.configuracion("SIGE_REGISTRO_REQUIERE_APROBACION", "registro.requiere.aprobacion");
        return valor == null || !valor.trim().equalsIgnoreCase("false");
    }

    private void registrarInterno(Usuario usuario, String contrasenaPlana, boolean pendiente) throws SQLException {
        String incumplimiento = PoliticaContrasena.validar(contrasenaPlana);
        if (incumplimiento != null) throw new IllegalArgumentException(incumplimiento);
        String hash = Usuario.hashear(contrasenaPlana);

        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                int id = insertarUsuario(conn, usuario, hash);
                usuario.setId(id);

                if (usuario instanceof Estudiante e) {
                    insertarEstudiante(conn, id, e);
                } else if (usuario instanceof Profesor p) {
                    insertarProfesor(conn, id, p);
                } else if (usuario instanceof Padre pa) {
                    pa.setIdPadre(insertarPadre(conn, id, pa));
                }

                if (pendiente) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE usuarios SET activo = 0, pendiente_aprobacion = 1 WHERE id_usuario = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Solicitud de registro público pendiente de revisión. detalle resume matrícula/grado o teléfono/ocupación. */
    public record Solicitud(int id, String nombre, String apellido, String correo, String rol, String detalle) { }

    /** true si la cuenta está inactiva porque espera la aprobación de un administrador. */
    public boolean estaPendienteAprobacion(int idUsuario) throws SQLException {
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM usuarios WHERE id_usuario = ? AND activo = 0 AND pendiente_aprobacion = 1 LIMIT 1")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarPendientes() throws SQLException {
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM usuarios WHERE activo = 0 AND pendiente_aprobacion = 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public Set<Integer> idsPendientes() throws SQLException {
        Set<Integer> ids = new LinkedHashSet<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id_usuario FROM usuarios WHERE activo = 0 AND pendiente_aprobacion = 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    public List<Solicitud> listarSolicitudes() throws SQLException {
        String sql = "SELECT u.id_usuario, u.nombre, u.apellido, u.correo, u.rol, e.matricula, e.grado, "
                + "pa.telefono_contacto, pa.ocupacion FROM usuarios u "
                + "LEFT JOIN estudiantes e ON e.id_estudiante = u.id_usuario "
                + "LEFT JOIN padres pa ON pa.id_usuario = u.id_usuario "
                + "WHERE u.activo = 0 AND u.pendiente_aprobacion = 1 ORDER BY u.id_usuario";
        List<Solicitud> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String rol = rs.getString("rol");
                String detalle;
                if ("ESTUDIANTE".equals(rol)) {
                    detalle = "Matrícula " + valorODash(rs.getString("matricula")) + " · Grado " + valorODash(rs.getString("grado"));
                } else {
                    detalle = "Teléfono " + valorODash(rs.getString("telefono_contacto"))
                            + " · Ocupación " + valorODash(rs.getString("ocupacion"));
                }
                lista.add(new Solicitud(rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("apellido"),
                        rs.getString("correo"), rol, detalle));
            }
        }
        return lista;
    }

    private static String valorODash(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    /** Activa una cuenta pendiente. Devuelve su correo para el registro de auditoría. */
    public String aprobarSolicitudComoAdmin(int idAdmin, int idUsuario) throws SQLException, PermisoDenegadoException {
        return resolverSolicitud(idAdmin, idUsuario, true);
    }

    /** Elimina una cuenta pendiente (y sus datos de perfil). Devuelve su correo para la auditoría. */
    public String rechazarSolicitudComoAdmin(int idAdmin, int idUsuario) throws SQLException, PermisoDenegadoException {
        return resolverSolicitud(idAdmin, idUsuario, false);
    }

    private String resolverSolicitud(int idAdmin, int idUsuario, boolean aprobar)
            throws SQLException, PermisoDenegadoException {
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                validarAdministrador(conn, idAdmin);
                String correo;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT correo FROM usuarios WHERE id_usuario = ? AND activo = 0 AND pendiente_aprobacion = 1 FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new PermisoDenegadoException("La solicitud ya fue atendida o no existe.");
                        correo = rs.getString(1);
                    }
                }
                String sql = aprobar
                        ? "UPDATE usuarios SET activo = 1, pendiente_aprobacion = 0 WHERE id_usuario = ?"
                        : "DELETE FROM usuarios WHERE id_usuario = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }
                conn.commit();
                return correo;
            } catch (SQLException | PermisoDenegadoException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Registra un profesor solo si el usuario que ejecuta la operación es ADMIN. */
    public void registrarProfesorComoAdmin(Profesor profesor, String contrasenaPlana, int idAdmin)
            throws SQLException, PermisoDenegadoException {
        String incumplimiento = PoliticaContrasena.validar(contrasenaPlana);
        if (incumplimiento != null) throw new PermisoDenegadoException(incumplimiento);
        String hash = Usuario.hashear(contrasenaPlana);
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM usuarios WHERE id_usuario = ? AND rol = 'ADMIN' AND activo = 1 FOR UPDATE")) {
                    ps.setInt(1, idAdmin);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new PermisoDenegadoException(
                                "Solo un administrador puede crear cuentas de profesor.");
                    }
                }
                int id = insertarUsuario(conn, profesor, hash);
                profesor.setId(id);
                insertarProfesor(conn, id, profesor);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios SET debe_cambiar_contrasena = 1 WHERE id_usuario = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Valida la contraseña actual y guarda la nueva con BCrypt, eliminando el indicador inicial. */
    public void cambiarContrasena(int idUsuario, String actual, String nueva)
            throws SQLException, PermisoDenegadoException {
        String incumplimiento = PoliticaContrasena.validar(nueva);
        if (incumplimiento != null) {
            throw new PermisoDenegadoException(incumplimiento);
        }
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                String hashActual;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT contrasena FROM usuarios WHERE id_usuario = ? FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new PermisoDenegadoException("La cuenta ya no existe.");
                        hashActual = rs.getString("contrasena");
                    }
                }
                boolean correcta;
                try {
                    correcta = actual != null && hashActual != null && BCrypt.checkpw(actual, hashActual);
                } catch (IllegalArgumentException e) {
                    correcta = false;
                }
                if (!correcta) throw new PermisoDenegadoException("La contraseña actual es incorrecta.");
                if (actual.equals(nueva)) throw new PermisoDenegadoException(
                        "La nueva contraseña debe ser distinta de la actual.");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios SET contrasena = ?, debe_cambiar_contrasena = 0 WHERE id_usuario = ?")) {
                    ps.setString(1, Usuario.hashear(nueva));
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Lista todos los usuarios con sus datos basicos (id, nombre, correo y rol).
     * Pensado para la pantalla del administrador; no carga los datos propios de cada rol
     * ni el hash de la contrasena.
     */
    public List<Usuario> listarTodos() throws SQLException {
        String sql = "SELECT id_usuario, nombre, apellido, correo, rol, activo FROM usuarios ORDER BY id_usuario";
        List<Usuario> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Rol rol;
                try {
                    rol = Rol.valueOf(rs.getString("rol"));
                } catch (IllegalArgumentException ex) {
                    rol = null; // rol desconocido: la tabla lo muestra como "?"
                }
                Usuario usuario = new Usuario(rs.getInt("id_usuario"), rs.getString("nombre"),
                        rs.getString("apellido"), rs.getString("correo"), null, rol);
                usuario.setActivo(rs.getBoolean("activo"));
                lista.add(usuario);
            }
        }
        return lista;
    }

    /** Búsqueda administrativa parametrizada por texto, rol y estado de la cuenta. */
    public List<Usuario> buscarTodos(String texto, String rol, String estado) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id_usuario,nombre,apellido,correo,rol,activo FROM usuarios WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (nombre LIKE ? OR apellido LIKE ? OR correo LIKE ? OR CONCAT(nombre,' ',apellido) LIKE ?)");
            String pattern = "%" + texto.trim() + "%";
            for (int i=0;i<4;i++) params.add(pattern);
        }
        if (rol != null && !rol.isBlank()) { sql.append(" AND rol = ?"); params.add(rol); }
        if ("activa".equals(estado)) sql.append(" AND activo = 1");
        else if ("inactiva".equals(estado)) sql.append(" AND activo = 0");
        sql.append(" ORDER BY id_usuario");
        List<Usuario> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i=0;i<params.size();i++) ps.setObject(i+1,params.get(i));
            try (ResultSet rs=ps.executeQuery()) {
                while(rs.next()) {
                    Rol role; try { role=Rol.valueOf(rs.getString("rol")); } catch(IllegalArgumentException ex) { role=null; }
                    Usuario user=new Usuario(rs.getInt("id_usuario"),rs.getString("nombre"),rs.getString("apellido"),rs.getString("correo"),null,role);
                    user.setActivo(rs.getBoolean("activo")); lista.add(user);
                }
            }
        }
        return lista;
    }

    /**
     * Lista los profesores (para elegir uno al crear una seccion).
     * Solo carga id, nombre, apellido, correo, codigo y especialidad.
     */
    public List<Profesor> listarProfesores() throws SQLException {
        String sql = """
                SELECT u.id_usuario, u.nombre, u.apellido, u.correo, p.codigo_profesor, p.especialidad
                FROM profesores p
                JOIN usuarios u ON u.id_usuario = p.id_profesor
                ORDER BY u.apellido, u.nombre
                """;
        List<Profesor> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Profesor(rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("apellido"),
                        rs.getString("correo"), null, rs.getString("codigo_profesor"),
                        rs.getString("especialidad")));
            }
        }
        return lista;
    }

    public void cambiarEstadoCuentaComoAdmin(int idAdmin, int idUsuario, boolean activo)
            throws SQLException, PermisoDenegadoException {
        if (idAdmin == idUsuario && !activo) {
            throw new PermisoDenegadoException("No puedes desactivar tu propia cuenta.");
        }
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                validarAdministrador(conn, idAdmin);
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT rol FROM usuarios WHERE id_usuario = ? FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new PermisoDenegadoException("La cuenta seleccionada ya no existe.");
                        if (!activo && "ADMIN".equals(rs.getString("rol"))) {
                            try (PreparedStatement admins = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = 1 AND id_usuario <> ?")) {
                                admins.setInt(1, idUsuario);
                                try (ResultSet cuenta = admins.executeQuery()) {
                                    if (cuenta.next() && cuenta.getInt(1) == 0) {
                                        throw new PermisoDenegadoException("No se puede desactivar el último administrador activo.");
                                    }
                                }
                            }
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios SET activo = ?, pendiente_aprobacion = 0 WHERE id_usuario = ?")) {
                    ps.setBoolean(1, activo);
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void restablecer2FAComoAdmin(int idAdmin, int idUsuario)
            throws SQLException, PermisoDenegadoException {
        try (Connection conn = ConexionBD.obtener()) {
            validarAdministrador(conn, idAdmin);
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE usuarios SET dos_factor_activo = 0, dos_factor_secreto = NULL,
                        dos_factor_intentos = 0, dos_factor_bloqueado_hasta = NULL
                    WHERE id_usuario = ?
                    """)) {
                ps.setInt(1, idUsuario);
                if (ps.executeUpdate() != 1) throw new PermisoDenegadoException("La cuenta seleccionada ya no existe.");
            }
        }
    }

    /** Asigna una clave temporal al usuario y exige que la cambie al iniciar sesión. */
    public void restablecerContrasenaComoAdmin(int idAdmin, int idUsuario, String temporal)
            throws SQLException, PermisoDenegadoException {
        if (idAdmin == idUsuario) throw new PermisoDenegadoException("No puedes restablecer tu propia contraseña desde esta opción.");
        if (temporal == null || temporal.length() < 16 || temporal.length() > 128)
            throw new PermisoDenegadoException("La contraseña temporal no cumple los requisitos de seguridad.");
        String hash = Usuario.hashear(temporal);
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                validarAdministrador(conn, idAdmin);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios SET contrasena = ?, debe_cambiar_contrasena = 1 WHERE id_usuario = ?")) {
                    ps.setString(1, hash);
                    ps.setInt(2, idUsuario);
                    if (ps.executeUpdate() != 1) throw new PermisoDenegadoException("La cuenta seleccionada ya no existe.");
                }
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Cambia el rol y su perfil en una transacción; no permite dejar huérfanos académicos. */
    public void cambiarRolComoAdmin(int idAdmin, int idUsuario, Rol nuevoRol,
            String matricula, String grado, String codigoProfesor, String especialidad,
            String telefono, String ocupacion) throws SQLException, PermisoDenegadoException {
        if (idAdmin == idUsuario) {
            throw new PermisoDenegadoException("No puedes cambiar tu propio rol.");
        }
        if (nuevoRol == null) throw new PermisoDenegadoException("Selecciona el rol nuevo.");
        matricula = limpiar(matricula);
        grado = limpiar(grado);
        codigoProfesor = limpiar(codigoProfesor);
        especialidad = limpiar(especialidad);
        telefono = limpiar(telefono);
        ocupacion = limpiar(ocupacion);
        if (nuevoRol == Rol.ESTUDIANTE && (matricula == null || grado == null))
            throw new PermisoDenegadoException("El perfil de estudiante requiere matrícula y grado.");
        if (nuevoRol == Rol.PROFESOR && (codigoProfesor == null || especialidad == null))
            throw new PermisoDenegadoException("El perfil de profesor requiere código y especialidad.");
        if (matricula != null && matricula.length() > 20 || grado != null && grado.length() > 30
                || codigoProfesor != null && codigoProfesor.length() > 20
                || especialidad != null && especialidad.length() > 100
                || telefono != null && telefono.length() > 30 || ocupacion != null && ocupacion.length() > 100) {
            throw new PermisoDenegadoException("Uno o más datos del perfil exceden el límite permitido.");
        }
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                validarAdministrador(conn, idAdmin);
                String nombre;
                String apellido;
                String correo;
                Rol rolAnterior;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT nombre, apellido, correo, rol FROM usuarios WHERE id_usuario = ? FOR UPDATE")) {
                    ps.setInt(1, idUsuario);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new PermisoDenegadoException("La cuenta seleccionada ya no existe.");
                        nombre = rs.getString("nombre");
                        apellido = rs.getString("apellido");
                        correo = rs.getString("correo");
                        try { rolAnterior = Rol.valueOf(rs.getString("rol")); }
                        catch (IllegalArgumentException ex) { throw new PermisoDenegadoException("El rol actual no es válido."); }
                    }
                }
                if (rolAnterior == nuevoRol) throw new PermisoDenegadoException("La cuenta ya tiene ese rol.");
                if (rolAnterior == Rol.ADMIN) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT COUNT(*) FROM usuarios WHERE rol = 'ADMIN' AND activo = 1 AND id_usuario <> ?")) {
                        ps.setInt(1, idUsuario);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) == 0) {
                                throw new PermisoDenegadoException("No se puede degradar al último administrador activo.");
                            }
                        }
                    }
                }
                retirarPerfilSinDependencias(conn, idUsuario, rolAnterior);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios SET rol = ?, debe_cambiar_contrasena = 1 WHERE id_usuario = ?")) {
                    ps.setString(1, nuevoRol.name());
                    ps.setInt(2, idUsuario);
                    ps.executeUpdate();
                }
                Usuario perfil = switch (nuevoRol) {
                    case ADMIN -> new Usuario(idUsuario, nombre, apellido, correo, null, Rol.ADMIN);
                    case ESTUDIANTE -> new Estudiante(idUsuario, nombre, apellido, correo, null,
                            matricula, grado, 0);
                    case PROFESOR -> new Profesor(idUsuario, nombre, apellido, correo, null,
                            codigoProfesor, especialidad);
                    case PADRE -> new Padre(idUsuario, nombre, apellido, correo, null, 0,
                            telefono, ocupacion);
                };
                if (perfil instanceof Estudiante e) insertarEstudiante(conn, idUsuario, e);
                else if (perfil instanceof Profesor p) insertarProfesor(conn, idUsuario, p);
                else if (perfil instanceof Padre p) insertarPadre(conn, idUsuario, p);
                conn.commit();
            } catch (SQLException | PermisoDenegadoException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void retirarPerfilSinDependencias(Connection conn, int idUsuario, Rol rol)
            throws SQLException, PermisoDenegadoException {
        String dependencia = switch (rol) {
            case ESTUDIANTE -> "SELECT (SELECT COUNT(*) FROM inscripciones WHERE id_estudiante = ?)"
                    + " + (SELECT COUNT(*) FROM estudiante_padre WHERE id_estudiante = ?)";
            case PROFESOR -> "SELECT COUNT(*) FROM secciones WHERE id_profesor = ?";
            case PADRE -> "SELECT COUNT(*) FROM estudiante_padre ep JOIN padres p ON p.id_padre = ep.id_padre"
                    + " WHERE p.id_usuario = ?";
            case ADMIN -> null;
        };
        String tabla = switch (rol) {
            case ESTUDIANTE -> "estudiantes";
            case PROFESOR -> "profesores";
            case PADRE -> "padres";
            case ADMIN -> null;
        };
        if (tabla == null) return;
        try (PreparedStatement ps = conn.prepareStatement(dependencia)) {
            ps.setInt(1, idUsuario);
            if (rol == Rol.ESTUDIANTE) ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new PermisoDenegadoException("No se puede cambiar el rol: el perfil tiene relaciones académicas asociadas.");
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tabla + " WHERE "
                + (rol == Rol.PADRE ? "id_usuario" : rol == Rol.ESTUDIANTE ? "id_estudiante" : "id_profesor") + " = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarAdministrador(Connection conn, int idAdmin)
            throws SQLException, PermisoDenegadoException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM usuarios WHERE id_usuario = ? AND rol = 'ADMIN' AND activo = 1 FOR UPDATE")) {
            ps.setInt(1, idAdmin);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new PermisoDenegadoException("Solo un administrador puede gestionar cuentas.");
            }
        }
    }

    /**
     * Lista los estudiantes (usuario + matricula, grado y promedio), ordenados por apellido.
     * Se usa para elegir a quien inscribir en una seccion.
     */
    public List<Estudiante> listarEstudiantes() throws SQLException {
        String sql = """
                SELECT u.id_usuario, u.nombre, u.apellido, u.correo,
                       e.matricula, e.grado, e.promedio_academico
                FROM estudiantes e
                JOIN usuarios u ON u.id_usuario = e.id_estudiante
                ORDER BY u.apellido, u.nombre
                """;
        List<Estudiante> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Estudiante(rs.getInt("id_usuario"), rs.getString("nombre"),
                        rs.getString("apellido"), rs.getString("correo"), null,
                        rs.getString("matricula"), rs.getString("grado"), rs.getDouble("promedio_academico")));
            }
        }
        return lista;
    }

    // ----- Metodos privados -----

    private boolean existe(String sql, String valor) throws SQLException {
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int insertarUsuario(Connection conn, Usuario u, String hash) throws SQLException {
        String sql = "INSERT INTO usuarios (nombre, apellido, correo, contrasena, rol) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getCorreo());
            ps.setString(4, hash);
            ps.setString(5, u.getRol().name());
            ps.executeUpdate();
            return claveGenerada(ps);
        }
    }

    private void insertarEstudiante(Connection conn, int id, Estudiante e) throws SQLException {
        String sql = "INSERT INTO estudiantes (id_estudiante, matricula, grado) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, e.getMatricula());
            ps.setString(3, e.getGrado());
            ps.executeUpdate();
        }
    }

    private void insertarProfesor(Connection conn, int id, Profesor p) throws SQLException {
        String sql = "INSERT INTO profesores (id_profesor, codigo_profesor, especialidad) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, p.getCodigoProfesor());
            ps.setString(3, p.getEspecialidad());
            ps.executeUpdate();
        }
    }

    private int insertarPadre(Connection conn, int idUsuario, Padre pa) throws SQLException {
        String sql = "INSERT INTO padres (id_usuario, telefono_contacto, ocupacion) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, pa.getTelefonoContacto());
            ps.setString(3, pa.getOcupacion());
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

    private Usuario mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_usuario");
        String nombre = rs.getString("nombre");
        String apellido = rs.getString("apellido");
        String correo = rs.getString("correo");
        String hash = rs.getString("contrasena");

        Rol rol;
        try {
            rol = Rol.valueOf(rs.getString("rol"));
        } catch (IllegalArgumentException ex) {
            throw new SQLException("Rol desconocido en la base de datos: " + rs.getString("rol"), ex);
        }

        Usuario usuario = switch (rol) {
            case ESTUDIANTE -> new Estudiante(id, nombre, apellido, correo, hash,
                    rs.getString("matricula"), rs.getString("grado"), rs.getDouble("promedio_academico"));
            case PROFESOR -> new Profesor(id, nombre, apellido, correo, hash,
                    rs.getString("codigo_profesor"), rs.getString("especialidad"));
            case PADRE -> new Padre(id, nombre, apellido, correo, hash,
                    rs.getInt("id_padre"), rs.getString("telefono_contacto"), rs.getString("ocupacion"));
            case ADMIN -> new Usuario(id, nombre, apellido, correo, hash, Rol.ADMIN);
        };
        usuario.setDebeCambiarContrasena(rs.getBoolean("debe_cambiar_contrasena"));
        usuario.setActivo(rs.getBoolean("activo"));
        return usuario;
    }
}
