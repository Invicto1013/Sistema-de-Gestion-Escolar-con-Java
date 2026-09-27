package com.unev.sige.data;

import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio en memoria con usuarios de prueba.
 * Sera reemplazado mas adelante por acceso a la base de datos MySQL.
 */
public class UsuarioRepositorio {

    private static final List<Usuario> usuarios = new ArrayList<>();

    static {
        usuarios.add(new Usuario(1, "admin", "admin123", "Administrador General", "admin@sige.edu.do", Rol.ADMIN));

        usuarios.add(new Profesor(2, "jperez", "prof123", "Juan Perez", "jperez@sige.edu.do",
                "Matematica", 5));

        usuarios.add(new Estudiante(3, "mgarcia", "est123", "Maria Garcia", "mgarcia@sige.edu.do",
                "M-2026-001", LocalDate.of(2012, 4, 10), "8vo", "A"));
    }

    private UsuarioRepositorio() {
    }

    /**
     * Busca un usuario por su nombre de usuario.
     */
    public static Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return usuarios.stream()
                .filter(u -> u.getNombreUsuario().equalsIgnoreCase(nombreUsuario))
                .findFirst();
    }

    /**
     * Agrega un nuevo usuario al repositorio (usado por el formulario de Registro).
     */
    public static void agregar(Usuario usuario) {
        usuarios.add(usuario);
    }

    public static boolean existeNombreUsuario(String nombreUsuario) {
        return buscarPorNombreUsuario(nombreUsuario).isPresent();
    }
    // Ejemplo dentro de UsuarioRepositorio.java
static {
    // Administrador único para el sistema
    usuarios.add(new Usuario(1, "admin", "admin123", "Elian Castro", "admin@sige.edu.do", Rol.ADMIN));
}
}
