SET NAMES utf8mb4;
-- Ejecutar en sige_db una sola vez (es seguro repetirlo).
-- Contenido de los archivos del aula virtual. Se guarda en MySQL porque el disco de los
-- servicios gratuitos (Render) se borra en cada reinicio. archivo_key enlaza con
-- aula_materiales.archivo_key y aula_entregas.archivo_key.
CREATE TABLE IF NOT EXISTS aula_archivos (
    archivo_key CHAR(36) NOT NULL PRIMARY KEY,
    contenido LONGBLOB NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
