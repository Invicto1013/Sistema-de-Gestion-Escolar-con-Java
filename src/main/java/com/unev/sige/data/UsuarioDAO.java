package com.unev.sige.data;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Acceso a MySQL para usuarios (tablas usuarios, estudiantes, profesores y padres).
 * Reemplaza al antiguo UsuarioRepositorio en memoria.
 */
public class UsuarioDAO {

    private static final String SQL_BUSCAR_POR_CORREO = """
            SELECT u.id_usuario, u.nombre, u.apellido, u.correo, u.contrasena, u.rol,
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

                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** Registra un profesor solo si el usuario que ejecuta la operación es ADMIN. */
    public void registrarProfesorComoAdmin(Profesor profesor, String contrasenaPlana, int idAdmin)
            throws SQLException, PermisoDenegadoException {
        String hash = Usuario.hashear(contrasenaPlana);
        try (Connection conn = ConexionBD.obtener()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM usuarios WHERE id_usuario = ? AND rol = 'ADMIN' FOR UPDATE")) {
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
        if (nueva == null || nueva.length() < 4) {
            throw new PermisoDenegadoException("La nueva contraseña debe tener al menos 4 caracteres.");
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
        String sql = "SELECT id_usuario, nombre, apellido, correo, rol FROM usuarios ORDER BY id_usuario";
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
                lista.add(new Usuario(rs.getInt("id_usuario"), rs.getString("nombre"),
                        rs.getString("apellido"), rs.getString("correo"), null, rol));
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
        return usuario;
    }
}
