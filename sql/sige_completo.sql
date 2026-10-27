-- Codificación: guarda este archivo como UTF-8 (sin BOM) y ejecútalo con la conexión en utf8mb4.
SET NAMES utf8mb4;
-- SIGE - Script completo de base de datos MySQL 8 (Tarea No. 4, Release 2.0)
-- Contiene: estructura completa (23 tablas, incluye curriculo por grados y aula virtual), roles iniciales (columna usuarios.rol:
-- ADMIN, PROFESOR, ESTUDIANTE, PADRE), un usuario de prueba por rol y datos de ejemplo.
--
-- Uso: crea una base vacia (por ejemplo sige_db), seleccionala e importa este archivo
-- (MySQL Workbench > Server > Data Import, o phpMyAdmin > Importar).
--
-- Usuarios de prueba (contrasenas de DEMOSTRACION; cambialas en uso real):
--   admin@sige.edu.do        Admin2026        ADMIN
--   jrodriguez@sige.edu.do   Profesor2026     PROFESOR
--   peguero@sige.edu.do      Estudiante2026   ESTUDIANTE
--   mariana@sige.edu.do      Padre2026        PADRE
-- El 2FA queda sin activar: se configura (QR + TOTP) en el primer inicio de sesion de cada usuario.
-- Las contrasenas se guardan con BCrypt; no hay secretos 2FA ni claves reales en este script.

--

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `asignaturas`
--

DROP TABLE IF EXISTS `asignaturas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asignaturas` (
  `id_asignatura` int NOT NULL AUTO_INCREMENT,
  `codigo_asignatura` varchar(10) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `creditos` int NOT NULL,
  `descripcion` text,
  PRIMARY KEY (`id_asignatura`),
  UNIQUE KEY `codigo_asignatura` (`codigo_asignatura`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asignaturas`
--

/*!40000 ALTER TABLE `asignaturas` DISABLE KEYS */;
INSERT INTO `asignaturas` VALUES (1,'MAT-101','Matematicas',3,NULL);
/*!40000 ALTER TABLE `asignaturas` ENABLE KEYS */;

--
-- Table structure for table `asistencias`
--

DROP TABLE IF EXISTS `asistencias`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asistencias` (
  `id_asistencia` int NOT NULL AUTO_INCREMENT,
  `id_detalle_inscripcion` int NOT NULL,
  `fecha` date NOT NULL,
  `presente` tinyint(1) NOT NULL DEFAULT '1',
  `observacion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_asistencia`),
  UNIQUE KEY `uq_asistencia_dia` (`id_detalle_inscripcion`,`fecha`),
  CONSTRAINT `asistencias_ibfk_1` FOREIGN KEY (`id_detalle_inscripcion`) REFERENCES `detalle_inscripciones` (`id_detalle_inscripcion`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asistencias`
--

/*!40000 ALTER TABLE `asistencias` DISABLE KEYS */;
INSERT INTO `asistencias` VALUES (1,2,'2026-10-12',1,NULL);
/*!40000 ALTER TABLE `asistencias` ENABLE KEYS */;

--
-- Table structure for table `bitacora_accesos`
--

DROP TABLE IF EXISTS `bitacora_accesos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bitacora_accesos` (
  `id_acceso` bigint unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` int DEFAULT NULL,
  `correo_ingresado` varchar(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fecha_hora` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resultado` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detalle` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_acceso`),
  KEY `idx_bitacora_accesos_usuario_fecha` (`id_usuario`,`fecha_hora`),
  KEY `idx_bitacora_accesos_correo_fecha` (`correo_ingresado`,`fecha_hora`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bitacora_accesos`
--

/*!40000 ALTER TABLE `bitacora_accesos` DISABLE KEYS */;
/*!40000 ALTER TABLE `bitacora_accesos` ENABLE KEYS */;

--
-- Table structure for table `calificaciones`
--

DROP TABLE IF EXISTS `calificaciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calificaciones` (
  `id_calificacion` int NOT NULL AUTO_INCREMENT,
  `nota_parcial1` double DEFAULT '0',
  `nota_parcial2` double DEFAULT '0',
  `nota_examen_final` double DEFAULT '0',
  `nota_final` double DEFAULT '0',
  `calificacion_literal` varchar(2) DEFAULT '',
  `id_detalle_inscripcion` int NOT NULL,
  PRIMARY KEY (`id_calificacion`),
  UNIQUE KEY `id_detalle_inscripcion` (`id_detalle_inscripcion`),
  CONSTRAINT `calificaciones_ibfk_1` FOREIGN KEY (`id_detalle_inscripcion`) REFERENCES `detalle_inscripciones` (`id_detalle_inscripcion`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `calificaciones`
--

/*!40000 ALTER TABLE `calificaciones` DISABLE KEYS */;
INSERT INTO `calificaciones` VALUES (2,85,93,100,93.4,'A',2);
/*!40000 ALTER TABLE `calificaciones` ENABLE KEYS */;

--
-- Table structure for table `controlador_sesiones`
--

DROP TABLE IF EXISTS `controlador_sesiones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `controlador_sesiones` (
  `id_sesion` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `fecha_inicio` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_cierre` datetime DEFAULT NULL,
  `estado_activo` tinyint(1) NOT NULL DEFAULT '1',
  `direccion_ip` varchar(45) NOT NULL DEFAULT '127.0.0.1',
  PRIMARY KEY (`id_sesion`),
  KEY `id_usuario` (`id_usuario`),
  CONSTRAINT `controlador_sesiones_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `controlador_sesiones`
--

/*!40000 ALTER TABLE `controlador_sesiones` DISABLE KEYS */;
/*!40000 ALTER TABLE `controlador_sesiones` ENABLE KEYS */;

--
-- Table structure for table `detalle_inscripciones`
--

DROP TABLE IF EXISTS `detalle_inscripciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `detalle_inscripciones` (
  `id_detalle_inscripcion` int NOT NULL AUTO_INCREMENT,
  `estado_materia` varchar(20) NOT NULL DEFAULT 'Inscrita',
  `id_inscripcion` int NOT NULL,
  `id_seccion` int NOT NULL,
  PRIMARY KEY (`id_detalle_inscripcion`),
  KEY `id_inscripcion` (`id_inscripcion`),
  KEY `id_seccion` (`id_seccion`),
  CONSTRAINT `detalle_inscripciones_ibfk_1` FOREIGN KEY (`id_inscripcion`) REFERENCES `inscripciones` (`id_inscripcion`) ON DELETE CASCADE,
  CONSTRAINT `detalle_inscripciones_ibfk_2` FOREIGN KEY (`id_seccion`) REFERENCES `secciones` (`id_seccion`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `detalle_inscripciones`
--

/*!40000 ALTER TABLE `detalle_inscripciones` DISABLE KEYS */;
INSERT INTO `detalle_inscripciones` VALUES (2,'Aprobada',1,2);
/*!40000 ALTER TABLE `detalle_inscripciones` ENABLE KEYS */;

--
-- Table structure for table `estudiante_padre`
--

DROP TABLE IF EXISTS `estudiante_padre`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estudiante_padre` (
  `id_estudiante` int NOT NULL,
  `id_padre` int NOT NULL,
  `parentesco` varchar(50) DEFAULT 'PADRE/MADRE/TUTOR',
  PRIMARY KEY (`id_estudiante`,`id_padre`),
  KEY `id_padre` (`id_padre`),
  CONSTRAINT `estudiante_padre_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE,
  CONSTRAINT `estudiante_padre_ibfk_2` FOREIGN KEY (`id_padre`) REFERENCES `padres` (`id_padre`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estudiante_padre`
--

/*!40000 ALTER TABLE `estudiante_padre` DISABLE KEYS */;
INSERT INTO `estudiante_padre` VALUES (3,1,'PADRE/MADRE/TUTOR');
/*!40000 ALTER TABLE `estudiante_padre` ENABLE KEYS */;

--
-- Table structure for table `estudiantes`
--

DROP TABLE IF EXISTS `estudiantes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estudiantes` (
  `id_estudiante` int NOT NULL,
  `matricula` varchar(20) NOT NULL,
  `grado` varchar(50) NOT NULL,
  `promedio_academico` double DEFAULT '0',
  PRIMARY KEY (`id_estudiante`),
  UNIQUE KEY `matricula` (`matricula`),
  CONSTRAINT `estudiantes_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estudiantes`
--

/*!40000 ALTER TABLE `estudiantes` DISABLE KEYS */;
INSERT INTO `estudiantes` VALUES (3,'M-2026-001','8vo',93.4);
/*!40000 ALTER TABLE `estudiantes` ENABLE KEYS */;

--
-- Table structure for table `inscripciones`
--

DROP TABLE IF EXISTS `inscripciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inscripciones` (
  `id_inscripcion` int NOT NULL AUTO_INCREMENT,
  `fecha_inscripcion` date NOT NULL,
  `periodo_lectivo` varchar(20) NOT NULL,
  `estado` varchar(20) NOT NULL DEFAULT 'Activa',
  `id_estudiante` int NOT NULL,
  PRIMARY KEY (`id_inscripcion`),
  KEY `id_estudiante` (`id_estudiante`),
  CONSTRAINT `inscripciones_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inscripciones`
--

/*!40000 ALTER TABLE `inscripciones` DISABLE KEYS */;
INSERT INTO `inscripciones` VALUES (1,'2026-10-12','2026-1','Activa',3);
/*!40000 ALTER TABLE `inscripciones` ENABLE KEYS */;

--
-- Table structure for table `padres`
--

DROP TABLE IF EXISTS `padres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `padres` (
  `id_padre` int NOT NULL AUTO_INCREMENT,
  `id_usuario` int NOT NULL,
  `telefono_contacto` varchar(20) DEFAULT NULL,
  `ocupacion` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_padre`),
  KEY `id_usuario` (`id_usuario`),
  CONSTRAINT `padres_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `padres`
--

/*!40000 ALTER TABLE `padres` DISABLE KEYS */;
INSERT INTO `padres` VALUES (1,4,'849-777-5048','Maestro');
/*!40000 ALTER TABLE `padres` ENABLE KEYS */;

--
-- Table structure for table `profesores`
--

DROP TABLE IF EXISTS `profesores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profesores` (
  `id_profesor` int NOT NULL,
  `codigo_profesor` varchar(20) NOT NULL,
  `especialidad` varchar(50) NOT NULL,
  PRIMARY KEY (`id_profesor`),
  UNIQUE KEY `codigo_profesor` (`codigo_profesor`),
  CONSTRAINT `profesores_ibfk_1` FOREIGN KEY (`id_profesor`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `profesores`
--

/*!40000 ALTER TABLE `profesores` DISABLE KEYS */;
INSERT INTO `profesores` VALUES (2,'P-001','Matematicas');
/*!40000 ALTER TABLE `profesores` ENABLE KEYS */;

--
-- Table structure for table `records_academicos`
--

DROP TABLE IF EXISTS `records_academicos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `records_academicos` (
  `id_record` int NOT NULL AUTO_INCREMENT,
  `asignaturas_aprobadas` int DEFAULT '0',
  `indice_acumulado` double DEFAULT '0',
  `id_estudiante` int NOT NULL,
  PRIMARY KEY (`id_record`),
  UNIQUE KEY `id_estudiante` (`id_estudiante`),
  CONSTRAINT `records_academicos_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `records_academicos`
--

/*!40000 ALTER TABLE `records_academicos` DISABLE KEYS */;
/*!40000 ALTER TABLE `records_academicos` ENABLE KEYS */;

--
-- Table structure for table `reportes_academicos`
--

DROP TABLE IF EXISTS `reportes_academicos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reportes_academicos` (
  `id_reporte` int NOT NULL AUTO_INCREMENT,
  `id_estudiante` int NOT NULL,
  `tipo_reporte` varchar(50) NOT NULL,
  `fecha_generacion` date NOT NULL,
  `contenido_detalle` text NOT NULL,
  PRIMARY KEY (`id_reporte`),
  KEY `id_estudiante` (`id_estudiante`),
  CONSTRAINT `reportes_academicos_ibfk_1` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiantes` (`id_estudiante`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reportes_academicos`
--

/*!40000 ALTER TABLE `reportes_academicos` DISABLE KEYS */;
/*!40000 ALTER TABLE `reportes_academicos` ENABLE KEYS */;

--
-- Table structure for table `secciones`
--

DROP TABLE IF EXISTS `secciones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `secciones` (
  `id_seccion` int NOT NULL AUTO_INCREMENT,
  `aula` varchar(20) NOT NULL,
  `horario` varchar(50) NOT NULL,
  `cupo_maximo` int NOT NULL,
  `id_asignatura` int NOT NULL,
  `id_profesor` int DEFAULT NULL,
  PRIMARY KEY (`id_seccion`),
  KEY `id_asignatura` (`id_asignatura`),
  KEY `id_profesor` (`id_profesor`),
  CONSTRAINT `secciones_ibfk_1` FOREIGN KEY (`id_asignatura`) REFERENCES `asignaturas` (`id_asignatura`) ON DELETE CASCADE,
  CONSTRAINT `secciones_ibfk_2` FOREIGN KEY (`id_profesor`) REFERENCES `profesores` (`id_profesor`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `secciones`
--

/*!40000 ALTER TABLE `secciones` DISABLE KEYS */;
INSERT INTO `secciones` VALUES (2,'A-101','Lun-Mie-8:00-9:30',30,1,2);
/*!40000 ALTER TABLE `secciones` ENABLE KEYS */;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `correo` varchar(100) NOT NULL,
  `contrasena` varchar(255) NOT NULL,
  `rol` varchar(20) NOT NULL,
  `debe_cambiar_contrasena` tinyint(1) NOT NULL DEFAULT '0',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `dos_factor_activo` tinyint(1) NOT NULL DEFAULT '0',
  `dos_factor_secreto` varchar(128) DEFAULT NULL,
  `dos_factor_intentos` smallint unsigned NOT NULL DEFAULT '0',
  `dos_factor_bloqueado_hasta` datetime DEFAULT NULL,
  `login_intentos` int NOT NULL DEFAULT '0',
  `login_bloqueado_hasta` datetime DEFAULT NULL,
  `pendiente_aprobacion` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `correo` (`correo`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'Elian','Castro','admin@sige.edu.do','$2a$10$OFwIWmH37FwAgunNJ6qPYOhvuxZUQ.W7sTNNqXYXi4PzDFWdZNclO','ADMIN',0,1,0,NULL,0,NULL,0,NULL,0),(2,'Julian','Rodriguez','jrodriguez@sige.edu.do','$2a$10$dDXVYKlJXdaXLUw.cqWuJOPyE3GhPp.rVBEnPYMsjGnX4b9hS87Rq','PROFESOR',0,1,0,NULL,0,NULL,0,NULL,0),(3,'Carlos','Peguero','peguero@sige.edu.do','$2a$10$WeI15G.zP2P3br4V4tfVzeJETWVXw4A5mIo3nOQZZnNLawhtL2FOq','ESTUDIANTE',0,1,0,NULL,0,NULL,0,NULL,0),(4,'Mariana','Zapata','mariana@sige.edu.do','$2a$10$AI93b/ZNojwr7TE0y90adekcgJSODEBEh/Ry0Z3..6PSBtvtcITE2','PADRE',0,1,0,NULL,0,NULL,0,NULL,0);
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;

--
-- Dumping events for database 'sige_db'
--

--
-- Dumping routines for database 'sige_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- ===== fase_curriculo_grados.sql =====
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

-- ===== aula_virtual.sql =====
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

-- Contenido de los archivos del aula virtual. Se guarda en MySQL porque el disco de los
-- servicios gratuitos (Render) se borra en cada reinicio. archivo_key enlaza con
-- aula_materiales.archivo_key y aula_entregas.archivo_key.
CREATE TABLE IF NOT EXISTS aula_archivos (
    archivo_key CHAR(36) NOT NULL PRIMARY KEY,
    contenido LONGBLOB NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sesiones de la web guardadas en MySQL para que sobrevivan a reinicios del servidor (Render se duerme
-- y se redespliega). Se guarda el hash SHA-256 del token de la cookie, nunca el token ni secretos 2FA.
-- id_usuario: sesion iniciada. id_pendiente: contrasena verificada, falta 2FA o cambio de clave.
-- id_login: fila de controlador_sesiones (historial); si un administrador la cierra, la sesion deja de valer.
CREATE TABLE IF NOT EXISTS sesiones_web (
    token_hash CHAR(64) NOT NULL PRIMARY KEY,
    csrf VARCHAR(64) NOT NULL,
    id_usuario INT NULL,
    id_pendiente INT NULL,
    mfa_activo TINYINT(1) NOT NULL DEFAULT 0,
    id_login INT NULL,
    ultimo_acceso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sesiones_web_usuario (id_usuario),
    INDEX idx_sesiones_web_pendiente (id_pendiente),
    INDEX idx_sesiones_web_login (id_login),
    INDEX idx_sesiones_web_acceso (ultimo_acceso),
    CONSTRAINT fk_sesion_web_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_sesion_web_pendiente FOREIGN KEY (id_pendiente) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
