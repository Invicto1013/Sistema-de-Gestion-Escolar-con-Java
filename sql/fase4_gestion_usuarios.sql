-- SIGE Release 2.0, Fase 4: activación de cuentas y estado de 2FA.
-- Selecciona primero sige_db en Workbench y ejecuta este script una sola vez;
-- las comprobaciones permiten volver a ejecutarlo sin duplicar las columnas.
USE sige_db;

SET @tabla_usuarios = DATABASE();

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = @tabla_usuarios AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'activo');
SET @ddl = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1',
    'SELECT 1');
PREPARE sentencia FROM @ddl; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = @tabla_usuarios AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'dos_factor_activo');
SET @ddl = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN dos_factor_activo TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE sentencia FROM @ddl; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = @tabla_usuarios AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'dos_factor_secreto');
SET @ddl = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN dos_factor_secreto VARCHAR(128) NULL',
    'SELECT 1');
PREPARE sentencia FROM @ddl; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = @tabla_usuarios AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'dos_factor_intentos');
SET @ddl = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN dos_factor_intentos SMALLINT UNSIGNED NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE sentencia FROM @ddl; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;

SET @existe = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = @tabla_usuarios AND TABLE_NAME = 'usuarios' AND COLUMN_NAME = 'dos_factor_bloqueado_hasta');
SET @ddl = IF(@existe = 0,
    'ALTER TABLE usuarios ADD COLUMN dos_factor_bloqueado_hasta DATETIME NULL',
    'SELECT 1');
PREPARE sentencia FROM @ddl; EXECUTE sentencia; DEALLOCATE PREPARE sentencia;

UPDATE usuarios SET activo = 1 WHERE activo IS NULL;
