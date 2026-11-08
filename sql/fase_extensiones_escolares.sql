SET NAMES utf8mb4;
-- ============================================================================
-- Fase E: Extensiones Escolares Avanzadas
-- 1. Tablón de Anuncios y Circulares Institucionales
-- 2. Horarios Escolares y Distribución de Bloques de Clase
-- 3. Cuaderno de Observaciones y Conducta Estudiantil
-- ============================================================================

-- 1. Tablón de anuncios y comunicados
CREATE TABLE IF NOT EXISTS anuncios_escolares (
    id_anuncio INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    contenido TEXT NOT NULL,
    id_autor INT NOT NULL,
    destinatarios VARCHAR(30) NOT NULL DEFAULT 'TODOS', -- TODOS, PROFESORES, ESTUDIANTES, PADRES
    prioridad VARCHAR(15) NOT NULL DEFAULT 'NORMAL',   -- NORMAL, IMPORTANTE, URGENTE
    fecha_publicacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    INDEX idx_anuncios_dest (destinatarios, activo),
    INDEX idx_anuncios_fecha (fecha_publicacion),
    CONSTRAINT fk_anuncio_autor FOREIGN KEY (id_autor) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Horarios por sección (bloques semanales de clase)
CREATE TABLE IF NOT EXISTS horarios_secciones (
    id_horario INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_seccion INT NOT NULL,
    dia_semana VARCHAR(12) NOT NULL, -- LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    aula VARCHAR(50) NULL,
    INDEX idx_horario_seccion (id_seccion),
    INDEX idx_horario_dia (dia_semana, hora_inicio),
    CONSTRAINT ck_horario_dia CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO')),
    CONSTRAINT ck_horario_horas CHECK (hora_inicio < hora_fin),
    CONSTRAINT fk_horario_seccion FOREIGN KEY (id_seccion) REFERENCES secciones(id_seccion) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Cuaderno de observaciones y conducta
CREATE TABLE IF NOT EXISTS observaciones_conducta (
    id_observacion INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_profesor INT NOT NULL,
    id_seccion INT NULL,
    tipo VARCHAR(25) NOT NULL, -- MERITO, CONDUCTA, TARDANZA, TAREA, NOTA_INFORMATIVA
    titulo VARCHAR(100) NOT NULL,
    descripcion TEXT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_obs_estudiante (id_estudiante, fecha),
    INDEX idx_obs_profesor (id_profesor),
    INDEX idx_obs_tipo (tipo),
    CONSTRAINT ck_obs_tipo CHECK (tipo IN ('MERITO', 'CONDUCTA', 'TARDANZA', 'TAREA', 'NOTA_INFORMATIVA')),
    CONSTRAINT fk_obs_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_obs_profesor FOREIGN KEY (id_profesor) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_obs_seccion FOREIGN KEY (id_seccion) REFERENCES secciones(id_seccion) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
