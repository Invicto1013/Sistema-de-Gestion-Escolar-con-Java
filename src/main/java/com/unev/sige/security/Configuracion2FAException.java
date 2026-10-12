package com.unev.sige.security;

/** Error de configuración local del cifrado de secretos TOTP. */
public class Configuracion2FAException extends Exception {
    public Configuracion2FAException(String mensaje) { super(mensaje); }
    public Configuracion2FAException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
