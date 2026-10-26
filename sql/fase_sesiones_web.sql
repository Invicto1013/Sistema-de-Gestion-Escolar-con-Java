SET NAMES utf8mb4;
-- Ejecutar en sige_db una sola vez (es seguro repetirlo).
-- Sesiones de la web guardadas en MySQL para que sobrevivan a reinicios del servidor (Render se duerme
-- y se redespliega). Se guarda el hash SHA-256 del token de la cookie, nunca el token ni secretos 2FA.
-- id_usuario: sesion iniciada. id_pendiente: contrasena verificada, falta 2FA o cambio de clave.
-- id_login: fila de controlador_sesiones (historial); si un administrador la cierra, la sesion deja de valer.
CREATE TABLE IF NOT EXISTS sesiones_web (
    token_hash CHAR(64) NOT NULL PRIMARY KEY,
    csrf VARCHAR(64) NOT NULL,
    id_usuario INT NULL,
    id_pendiente INT NULL,
    mfa_activo TINYINT(1) NOT NULL DEFAULT 0,
    id_login INT NULL,
    ultimo_acceso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sesiones_web_usuario (id_usuario),
    INDEX idx_sesiones_web_pendiente (id_pendiente),
    INDEX idx_sesiones_web_login (id_login),
    INDEX idx_sesiones_web_acceso (ultimo_acceso),
    CONSTRAINT fk_sesion_web_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_sesion_web_pendiente FOREIGN KEY (id_pendiente) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
