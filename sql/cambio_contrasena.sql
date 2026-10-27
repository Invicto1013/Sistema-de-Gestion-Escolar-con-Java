-- Codificación: guarda este archivo como UTF-8 (sin BOM) y ejecútalo con la conexión en utf8mb4.
SET NAMES utf8mb4;
-- Agrega el indicador de cambio obligatorio para la contraseña inicial del profesor.
-- Ejecutar en MySQL Workbench antes de iniciar la versión actualizada de SIGE.
USE sige_db;

SET @columna_flag_existe = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'usuarios'
      AND column_name = 'debe_cambiar_contrasena'
);

SET @sql_migracion = IF(
    @columna_flag_existe = 0,
    'ALTER TABLE usuarios ADD COLUMN debe_cambiar_contrasena TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt_migracion FROM @sql_migracion;
EXECUTE stmt_migracion;
DEALLOCATE PREPARE stmt_migracion;

-- En la primera ejecución, las cuentas existentes de profesor deberán cambiar su clave inicial.
SET @sql_marcar_profesores = IF(
    @columna_flag_existe = 0,
    'UPDATE usuarios SET debe_cambiar_contrasena = 1 WHERE rol = ''PROFESOR''',
    'SELECT 1'
);
PREPARE stmt_marcar FROM @sql_marcar_profesores;
EXECUTE stmt_marcar;
DEALLOCATE PREPARE stmt_marcar;
