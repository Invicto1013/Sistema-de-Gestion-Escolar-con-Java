-- Aula virtual: materiales de clase, tareas y entregas de estudiantes.
-- Ejecutar en sige_db antes de utilizar /aula.
-- Se admite cualquier formato como archivo adjunto; el portal los descarga como adjuntos.

CREATE TABLE IF NOT EXISTS aula_materiales (
    id_material INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_seccion INT NOT NULL,
    id_profesor INT NOT NULL,
    tipo ENUM('CLASE','TAREA') NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descripcion TEXT NULL,
    fecha_limite DATE NULL,
    fecha_publicacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archivo_key CHAR(36) NULL,
    archivo_nombre VARCHAR(180) NULL,
    archivo_tipo VARCHAR(255) NULL,
    archivo_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT fk_aula_material_seccion FOREIGN KEY (id_seccion) REFERENCES secciones(id_seccion) ON DELETE CASCADE,
    CONSTRAINT fk_aula_material_profesor FOREIGN KEY (id_profesor) REFERENCES usuarios(id_usuario),
    INDEX idx_aula_material_seccion (id_seccion, fecha_publicacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS aula_entregas (
    id_entrega INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_material INT NOT NULL,
    id_estudiante INT NOT NULL,
    archivo_key CHAR(36) NOT NULL,
    archivo_nombre VARCHAR(180) NOT NULL,
    archivo_tipo VARCHAR(255) NULL,
    archivo_bytes BIGINT UNSIGNED NOT NULL,
    fecha_entrega TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aula_entrega_material FOREIGN KEY (id_material) REFERENCES aula_materiales(id_material) ON DELETE CASCADE,
    CONSTRAINT fk_aula_entrega_estudiante FOREIGN KEY (id_estudiante) REFERENCES usuarios(id_usuario),
    CONSTRAINT uq_aula_entrega_estudiante UNIQUE (id_material, id_estudiante),
    INDEX idx_aula_entrega_material (id_material, fecha_entrega)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
