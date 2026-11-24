-- Fase: planes de servicio (suscripción del centro). Ejecutar en sige_db después de un respaldo.
USE sige_db;
SET NAMES utf8mb4;

-- Una fila por cambio de plan; el plan vigente es la fila más reciente.
CREATE TABLE IF NOT EXISTS suscripcion_centro (
    id_suscripcion INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo_plan VARCHAR(20) NOT NULL,          -- ESENCIAL, ESTANDAR o INSTITUCIONAL
    cambiado_por INT NULL,                     -- id del administrador (sin FK: no bloquea el borrado de usuarios)
    cambiado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_suscripcion_fecha (cambiado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO suscripcion_centro (codigo_plan, cambiado_por)
SELECT 'ESTANDAR', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM suscripcion_centro);
