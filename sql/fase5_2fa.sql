-- SIGE Fase 5: bitácora de intentos de acceso y autenticación 2FA.
-- Ejecutar en MySQL Workbench después de aplicar fase4_gestion_usuarios.sql.
USE sige_db;

CREATE TABLE IF NOT EXISTS bitacora_accesos (
    id_acceso BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario INT NULL,
    correo_ingresado VARCHAR(190) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resultado VARCHAR(16) NOT NULL,
    detalle VARCHAR(120) NULL,
    PRIMARY KEY (id_acceso),
    INDEX idx_bitacora_accesos_usuario_fecha (id_usuario, fecha_hora),
    INDEX idx_bitacora_accesos_correo_fecha (correo_ingresado, fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
