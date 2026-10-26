SET NAMES utf8mb4;
-- Catalogo curricular dominicano para Primaria y Secundaria.
-- Ejecutar en la base sige_db antes de usar /curriculo.
-- La base nacional es comun; horas y ofertas particulares se configuraran en fases posteriores.

CREATE TABLE IF NOT EXISTS curriculo_grados (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nivel VARCHAR(30) NOT NULL,
    ciclo VARCHAR(40) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    orden INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS areas_curriculares (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS curriculo_grado_area (
    id_grado INT NOT NULL,
    id_area INT NOT NULL,
    horas_semanales DECIMAL(4,1) NULL,
    observacion VARCHAR(255) NULL,
    PRIMARY KEY (id_grado, id_area),
    CONSTRAINT fk_curriculo_area_grado FOREIGN KEY (id_grado) REFERENCES curriculo_grados(id) ON DELETE CASCADE,
    CONSTRAINT fk_curriculo_area_area FOREIGN KEY (id_area) REFERENCES areas_curriculares(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO curriculo_grados (codigo,nivel,ciclo,nombre,orden) VALUES
('P1','Primaria','Primer ciclo','1.º de Primaria',1),('P2','Primaria','Primer ciclo','2.º de Primaria',2),
('P3','Primaria','Primer ciclo','3.º de Primaria',3),('P4','Primaria','Segundo ciclo','4.º de Primaria',4),
('P5','Primaria','Segundo ciclo','5.º de Primaria',5),('P6','Primaria','Segundo ciclo','6.º de Primaria',6),
('S1','Secundaria','Primer ciclo','1.º de Secundaria',7),('S2','Secundaria','Primer ciclo','2.º de Secundaria',8),
('S3','Secundaria','Primer ciclo','3.º de Secundaria',9),('S4','Secundaria','Segundo ciclo','4.º de Secundaria',10),
('S5','Secundaria','Segundo ciclo','5.º de Secundaria',11),('S6','Secundaria','Segundo ciclo','6.º de Secundaria',12);

INSERT IGNORE INTO areas_curriculares (codigo,nombre) VALUES
('LENGUA','Lengua Española'),('MATEMATICA','Matemática'),('SOCIALES','Ciencias Sociales'),
('NATURALES','Ciencias de la Naturaleza'),('INGLES','Inglés'),('FRANCES','Francés'),
('FHR','Formación Integral Humana y Religiosa'),('ED_FISICA','Educación Física'),
('ARTISTICA','Educación Artística'),('OPTATIVAS','Optativas de modalidad');

-- Primaria: Inglés aparece en el segundo ciclo (4.º a 6.º).
INSERT IGNORE INTO curriculo_grado_area (id_grado,id_area)
SELECT g.id,a.id FROM curriculo_grados g CROSS JOIN areas_curriculares a
WHERE g.nivel='Primaria' AND a.codigo IN ('LENGUA','MATEMATICA','SOCIALES','NATURALES','FHR','ED_FISICA','ARTISTICA');
INSERT IGNORE INTO curriculo_grado_area (id_grado,id_area)
SELECT g.id,a.id FROM curriculo_grados g CROSS JOIN areas_curriculares a
WHERE g.codigo IN ('P4','P5','P6') AND a.codigo='INGLES';
-- Secundaria: areas comunes; optativas detalladas por modalidad/plan del centro.
INSERT IGNORE INTO curriculo_grado_area (id_grado,id_area)
SELECT g.id,a.id FROM curriculo_grados g CROSS JOIN areas_curriculares a
WHERE g.nivel='Secundaria' AND a.codigo IN ('LENGUA','MATEMATICA','SOCIALES','NATURALES','INGLES','FRANCES','FHR','ED_FISICA','ARTISTICA');
INSERT IGNORE INTO curriculo_grado_area (id_grado,id_area)
SELECT g.id,a.id FROM curriculo_grados g CROSS JOIN areas_curriculares a
WHERE g.codigo IN ('S4','S5','S6') AND a.codigo='OPTATIVAS';

-- Enlace entre el catálogo de asignaturas ya existente y las áreas/grados del currículo.
CREATE TABLE IF NOT EXISTS asignatura_grado_area (
    id_asignatura INT NOT NULL,
    id_grado INT NOT NULL,
    id_area INT NOT NULL,
    horas_semanales DECIMAL(4,1) NULL,
    PRIMARY KEY (id_asignatura,id_grado),
    CONSTRAINT fk_aga_asignatura FOREIGN KEY (id_asignatura) REFERENCES asignaturas(id_asignatura) ON DELETE CASCADE,
    CONSTRAINT fk_aga_grado FOREIGN KEY (id_grado) REFERENCES curriculo_grados(id) ON DELETE CASCADE,
    CONSTRAINT fk_aga_area FOREIGN KEY (id_area) REFERENCES areas_curriculares(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Compatibilidad: seccion sin grado para los datos historicos hasta que el administrador los clasifique.
SET @existe_grado_seccion = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='secciones' AND COLUMN_NAME='id_grado');
SET @ddl_grado_seccion = IF(@existe_grado_seccion=0,
    'ALTER TABLE secciones ADD COLUMN id_grado INT NULL', 'SELECT 1');
PREPARE stmt_grado_seccion FROM @ddl_grado_seccion; EXECUTE stmt_grado_seccion; DEALLOCATE PREPARE stmt_grado_seccion;
SET @existe_fk_grado_seccion = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='secciones' AND CONSTRAINT_NAME='fk_secciones_curriculo_grado');
SET @ddl_fk_grado_seccion = IF(@existe_fk_grado_seccion=0,
    'ALTER TABLE secciones ADD CONSTRAINT fk_secciones_curriculo_grado FOREIGN KEY (id_grado) REFERENCES curriculo_grados(id) ON DELETE RESTRICT', 'SELECT 1');
PREPARE stmt_fk_grado_seccion FROM @ddl_fk_grado_seccion; EXECUTE stmt_fk_grado_seccion; DEALLOCATE PREPARE stmt_fk_grado_seccion;

-- Asocia automáticamente asignaturas existentes reconocibles por nombre al área correspondiente.
INSERT IGNORE INTO asignatura_grado_area (id_asignatura,id_grado,id_area)
SELECT a.id_asignatura,g.id,ar.id FROM asignaturas a
JOIN areas_curriculares ar ON
    (ar.codigo='LENGUA' AND (LOWER(a.nombre) LIKE '%español%' OR LOWER(a.nombre) LIKE '%lengua%')) OR
    (ar.codigo='MATEMATICA' AND LOWER(a.nombre) LIKE '%matem%') OR
    (ar.codigo='SOCIALES' AND LOWER(a.nombre) LIKE '%social%') OR
    (ar.codigo='NATURALES' AND LOWER(a.nombre) LIKE '%natur%') OR
    (ar.codigo='INGLES' AND LOWER(a.nombre) LIKE '%ingl%') OR
    (ar.codigo='FRANCES' AND LOWER(a.nombre) LIKE '%franc%') OR
    (ar.codigo='FHR' AND (LOWER(a.nombre) LIKE '%relig%' OR LOWER(a.nombre) LIKE '%integral humana%')) OR
    (ar.codigo='ED_FISICA' AND LOWER(a.nombre) LIKE '%física%') OR
    (ar.codigo='ARTISTICA' AND LOWER(a.nombre) LIKE '%art%')
JOIN curriculo_grado_area cga ON cga.id_area=ar.id
JOIN curriculo_grados g ON g.id=cga.id_grado;