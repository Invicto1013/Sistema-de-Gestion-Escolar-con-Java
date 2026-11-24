SET NAMES utf8mb4;
-- ============================================================================
-- Fase F: Registros de las herramientas escolares
-- Guarda lo que cada usuario calcula en «Herramientas» (datos del formulario y
-- resultado) para consultarlo después y descargarlo en PDF.
-- Idempotente: se puede ejecutar varias veces sin perder datos.
-- Nota TiDB: las claves foráneas pueden no aplicarse; el programa borra los
-- datos hijos de forma explícita (ver HerramientaRegistroDAO y EliminacionUsuarioDAO).
-- ============================================================================

-- 1. Cabecera del registro (uno por cálculo guardado)
CREATE TABLE IF NOT EXISTS herramienta_registros (
    id_registro INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    herramienta VARCHAR(40) NOT NULL,   -- identificador del catálogo, p. ej. riesgo-desercion
    titulo VARCHAR(120) NOT NULL,
    resultado TEXT NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_herr_reg_usuario (id_usuario, creado_en),
    INDEX idx_herr_reg_herramienta (herramienta),
    CONSTRAINT fk_herr_reg_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Datos de entrada de cada registro (un renglón por campo del formulario)
CREATE TABLE IF NOT EXISTS herramienta_registro_datos (
    id_dato INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_registro INT NOT NULL,
    orden INT NOT NULL,
    campo VARCHAR(40) NOT NULL,
    etiqueta VARCHAR(120) NOT NULL,
    valor TEXT NOT NULL,
    INDEX idx_herr_dato_registro (id_registro, orden),
    CONSTRAINT fk_herr_dato_registro FOREIGN KEY (id_registro) REFERENCES herramienta_registros(id_registro) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
