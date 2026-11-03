SET NAMES utf8mb4;
-- Ejecutar en sige_db una sola vez (es seguro repetirlo).
-- Fase D: periodos lectivos (con cierre), auditoria de operaciones criticas e historial academico.
--
--  periodos_lectivos   Un periodo ABIERTO admite inscripciones y notas; uno CERRADO bloquea ambas.
--                      Solo un administrador cierra o reabre (con motivo) y solo el administrador
--                      puede hacer una correccion excepcional de notas en un periodo cerrado.
--  auditoria           Quien hizo que, cuando y por que (notas, cierres, matricula, secciones, roles).
--                      Es un registro de solo anadir: la aplicacion nunca la modifica ni la borra.
--  historial_academico Resultado de cada estudiante por periodo, guardado al cerrar el periodo.

CREATE TABLE IF NOT EXISTS periodos_lectivos (
    id_periodo INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(20) NOT NULL,
    fecha_inicio DATE NULL,
    fecha_fin DATE NULL,
    estado VARCHAR(10) NOT NULL DEFAULT 'ABIERTO',
    cerrado_por INT NULL,
    fecha_cierre DATETIME NULL,
    UNIQUE KEY uq_periodo_nombre (nombre),
    INDEX idx_periodo_estado (estado),
    CONSTRAINT ck_periodo_estado CHECK (estado IN ('ABIERTO', 'CERRADO')),
    CONSTRAINT ck_periodo_fechas CHECK (fecha_inicio IS NULL OR fecha_fin IS NULL OR fecha_inicio <= fecha_fin),
    CONSTRAINT fk_periodo_cerrado_por FOREIGN KEY (cerrado_por) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Los periodos que ya usan las inscripciones existentes quedan registrados como ABIERTOS.
INSERT IGNORE INTO periodos_lectivos (nombre)
SELECT DISTINCT periodo_lectivo FROM inscripciones WHERE periodo_lectivo <> '';

CREATE TABLE IF NOT EXISTS auditoria (
    id_auditoria BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT NULL,
    rol VARCHAR(20) NULL,
    accion VARCHAR(40) NOT NULL,
    entidad VARCHAR(40) NOT NULL,
    id_entidad VARCHAR(40) NULL,
    detalle VARCHAR(1000) NOT NULL,
    motivo VARCHAR(300) NULL,
    resultado VARCHAR(10) NOT NULL DEFAULT 'OK',
    INDEX idx_auditoria_fecha (fecha),
    INDEX idx_auditoria_usuario (id_usuario, fecha),
    INDEX idx_auditoria_accion (accion, fecha),
    INDEX idx_auditoria_entidad (entidad, id_entidad),
    CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('OK', 'DENEGADO')),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS historial_academico (
    id_historial INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_periodo INT NOT NULL,
    materias_total INT NOT NULL DEFAULT 0,
    materias_aprobadas INT NOT NULL DEFAULT 0,
    materias_reprobadas INT NOT NULL DEFAULT 0,
    promedio DECIMAL(5,2) NOT NULL DEFAULT 0,
    resultado VARCHAR(12) NOT NULL,
    fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_historial_estudiante_periodo (id_estudiante, id_periodo),
    INDEX idx_historial_periodo (id_periodo),
    CONSTRAINT ck_historial_resultado CHECK (resultado IN ('APROBADO', 'REPROBADO', 'INCOMPLETO')),
    CONSTRAINT fk_historial_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_historial_periodo FOREIGN KEY (id_periodo) REFERENCES periodos_lectivos(id_periodo) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
