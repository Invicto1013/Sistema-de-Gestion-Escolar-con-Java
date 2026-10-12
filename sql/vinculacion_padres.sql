-- Tablas para vincular cuentas de padres/tutores con estudiantes.
-- Ejecutar una vez en MySQL Workbench sobre sige_db.
-- Las tablas usuarios y estudiantes deben existir previamente.
USE sige_db;

CREATE TABLE IF NOT EXISTS padres (
    id_padre INT NOT NULL AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    telefono_contacto VARCHAR(20) DEFAULT NULL,
    ocupacion VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id_padre),
    UNIQUE KEY uq_padres_usuario (id_usuario),
    CONSTRAINT fk_padres_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS estudiante_padre (
    id_estudiante INT NOT NULL,
    id_padre INT NOT NULL,
    parentesco VARCHAR(50) NOT NULL DEFAULT 'PADRE/MADRE/TUTOR',
    PRIMARY KEY (id_estudiante, id_padre),
    KEY idx_estudiante_padre_padre (id_padre),
    CONSTRAINT fk_estudiante_padre_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES estudiantes (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_estudiante_padre_padre FOREIGN KEY (id_padre)
        REFERENCES padres (id_padre) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
