package com.unev.sige.model;

/**
 * Matriz de permisos por rol. Se usa en la web, en el ejecutable y en las comprobaciones del servidor, para que
 * una misma regla no quede repetida (ni olvidada) en varios sitios.
 *
 * <ul>
 *   <li>ADMIN: todo.</li>
 *   <li>DIRECTOR/A: supervisa (informes, boletas, carnets, asistencia, auditoría), publica anuncios y gestiona conducta.</li>
 *   <li>SECRETARIO/A: administración diaria (solicitudes de registro, vínculos padre-estudiante, horarios), informes y anuncios.</li>
 * </ul>
 */
public final class Permisos {
    private Permisos() { }

    private static boolean es(Rol r, Rol... roles) {
        if (r == null) return false;
        for (Rol x : roles) if (x == r) return true;
        return false;
    }

    /** Consulta informes de cualquier sección o estudiante: asistencia, boletas, carnets y reporte de inscripciones. */
    public static boolean veInformes(Rol r) { return es(r, Rol.ADMIN, Rol.DIRECTOR, Rol.SECRETARIA); }
    public static boolean publicaAnuncios(Rol r) { return es(r, Rol.ADMIN, Rol.DIRECTOR, Rol.SECRETARIA); }
    public static boolean gestionaHorarios(Rol r) { return es(r, Rol.ADMIN, Rol.SECRETARIA); }
    public static boolean atiendeSolicitudes(Rol r) { return es(r, Rol.ADMIN, Rol.SECRETARIA); }
    public static boolean gestionaVinculos(Rol r) { return es(r, Rol.ADMIN, Rol.SECRETARIA); }
    public static boolean veAuditoria(Rol r) { return es(r, Rol.ADMIN, Rol.DIRECTOR); }
    public static boolean registraConducta(Rol r) { return es(r, Rol.ADMIN, Rol.DIRECTOR, Rol.PROFESOR); }
    public static boolean veConductaCompleta(Rol r) { return es(r, Rol.ADMIN, Rol.DIRECTOR); }
    public static boolean eliminaConductaAjena(Rol r) { return es(r, Rol.ADMIN, Rol.DIRECTOR); }
    /** Roles que puede crear un administrador desde «Crear personal». */
    public static boolean esPersonalCreable(Rol r) { return es(r, Rol.DIRECTOR, Rol.SECRETARIA); }
}
