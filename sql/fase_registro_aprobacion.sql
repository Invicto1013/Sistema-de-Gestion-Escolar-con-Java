-- Fase: aprobación de registros públicos (ejecutar una vez en la base sige_db)
SET NAMES utf8mb4;
ALTER TABLE usuarios
  ADD COLUMN pendiente_aprobacion TINYINT(1) NOT NULL DEFAULT 0;
