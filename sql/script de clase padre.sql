USE sige_db;

-- 1. Crear la tabla de Padres
CREATE TABLE IF NOT EXISTS padres (
    id_padre INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    telefono_contacto VARCHAR(20),
    ocupacion VARCHAR(100),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 2. Tabla de relación Muchos a Muchos entre Padres y Estudiantes (Tutoría)
CREATE TABLE IF NOT EXISTS estudiante_padre (
    id_estudiante INT NOT NULL,
    id_padre INT NOT NULL,
    parentesco VARCHAR(50) DEFAULT 'PADRE/MADRE/TUTOR',
    PRIMARY KEY (id_estudiante, id_padre),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_padre) REFERENCES padres(id_padre) ON DELETE CASCADE
);