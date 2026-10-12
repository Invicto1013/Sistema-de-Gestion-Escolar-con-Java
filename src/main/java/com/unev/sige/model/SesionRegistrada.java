package com.unev.sige.model;

/** Datos visibles de una sesión registrada para auditoría administrativa. */
public record SesionRegistrada(
        int idSesion,
        String usuario,
        String correo,
        String inicio,
        String cierre,
        String direccionIP,
        String estado) {
}
