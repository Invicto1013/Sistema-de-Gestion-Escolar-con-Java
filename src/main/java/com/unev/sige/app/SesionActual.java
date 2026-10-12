package com.unev.sige.app;

import com.unev.sige.model.ControladorSesion;
import com.unev.sige.model.Usuario;

/**
 * Guarda quien tiene la sesion abierta mientras la aplicacion esta en ejecucion.
 * Las pantallas (por ejemplo el dashboard) leen de aqui al usuario autenticado.
 */
public final class SesionActual {

    private static Usuario usuario;
    private static ControladorSesion sesion; // puede ser null si no se pudo registrar en la BD

    private SesionActual() {
    }

    public static void iniciar(Usuario usuarioAutenticado, ControladorSesion sesionRegistrada) {
        usuario = usuarioAutenticado;
        sesion = sesionRegistrada;
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    public static ControladorSesion getSesion() {
        return sesion;
    }

    public static boolean haySesion() {
        return usuario != null;
    }

    /**
     * Olvida al usuario actual (no toca la base de datos; para eso esta SesionDAO.cerrar).
     */
    public static void cerrar() {
        usuario = null;
        sesion = null;
    }
}
