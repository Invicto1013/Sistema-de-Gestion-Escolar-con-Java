package com.unev.sige.data;

import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;

import java.sql.SQLException;

/**
 * Utilidad para crear el administrador del sistema (el registro publico no permite el rol ADMIN).
 * Se ejecuta una sola vez con 4 argumentos: nombre apellido correo contrasena
 * Ejemplo: Elian Castro admin@sige.edu.do MiClaveSegura
 */
public class CrearAdmin {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Uso: CrearAdmin <nombre> <apellido> <correo> <contrasena>");
            return;
        }

        String nombre = args[0];
        String apellido = args[1];
        String correo = args[2].trim().toLowerCase();
        String contrasena = args[3];

        String incumplimiento = com.unev.sige.model.PoliticaContrasena.validar(contrasena);
        if (incumplimiento != null) {
            System.err.println(incumplimiento);
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();
        try {
            if (dao.existeCorreo(correo)) {
                System.err.println("Ya existe un usuario con el correo " + correo);
                return;
            }
            Usuario admin = new Usuario(0, nombre, apellido, correo, null, Rol.ADMIN);
            dao.registrar(admin, contrasena);
            System.out.println("Administrador creado con id " + admin.getId() + " (" + correo + ")");
        } catch (SQLException e) {
            System.err.println("No se pudo crear el administrador: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
