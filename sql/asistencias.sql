-- Tabla de asistencias para SIGE.
-- Ejecutar una sola vez en MySQL Workbench sobre la base sige_db.
-- Un registro por materia inscrita (detalle_inscripciones) y por dia.

USE sige_db;

CREATE TABLE IF NOT EXISTS `asistencias` (
  `id_asistencia` int NOT NULL AUTO_INCREMENT,
  `id_detalle_inscripcion` int NOT NULL,
  `fecha` date NOT NULL,
  `presente` tinyint(1) NOT NULL DEFAULT '1',
  `observacion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_asistencia`),
  UNIQUE KEY `uq_asistencia_dia` (`id_detalle_inscripcion`, `fecha`),
  CONSTRAINT `asistencias_ibfk_1` FOREIGN KEY (`id_detalle_inscripcion`)
    REFERENCES `detalle_inscripciones` (`id_detalle_inscripcion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
