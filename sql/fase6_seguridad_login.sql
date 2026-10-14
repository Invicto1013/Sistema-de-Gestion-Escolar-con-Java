-- Bloqueo temporal por contraseñas incorrectas (5 intentos -> 15 minutos).
-- Ejecutar una sola vez en MySQL Workbench antes de iniciar esta versión de SIGE.
-- Es seguro repetirlo: solo agrega las columnas que falten.
USE sige_db;

SET @existe = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'login_intentos');
SET @sql_migracion = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN login_intentos INT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt_migracion FROM @sql_migracion;
EXECUTE stmt_migracion;
DEALLOCATE PREPARE stmt_migracion;

SET @existe = (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'login_bloqueado_hasta');
SET @sql_migracion = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN login_bloqueado_hasta DATETIME NULL',
    'SELECT 1');
PREPARE stmt_migracion FROM @sql_migracion;
EXECUTE stmt_migracion;
DEALLOCATE PREPARE stmt_migracion;
