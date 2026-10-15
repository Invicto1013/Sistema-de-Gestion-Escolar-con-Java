-- SIGE - Script completo de base de datos MySQL 8 (Tarea No. 4, Release 2.0)
-- Contiene: estructura completa (15 tablas), roles iniciales (columna usuarios.rol:
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
/*!50503 SET NAMES utf8 */;
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
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `correo` (`correo`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'Elian','Castro','admin@sige.edu.do','$2a$10$OFwIWmH37FwAgunNJ6qPYOhvuxZUQ.W7sTNNqXYXi4PzDFWdZNclO','ADMIN',0,1,0,NULL,0,NULL,0,NULL),(2,'Julian','Rodriguez','jrodriguez@sige.edu.do','$2a$10$dDXVYKlJXdaXLUw.cqWuJOPyE3GhPp.rVBEnPYMsjGnX4b9hS87Rq','PROFESOR',0,1,0,NULL,0,NULL,0,NULL),(3,'Carlos','Peguero','peguero@sige.edu.do','$2a$10$WeI15G.zP2P3br4V4tfVzeJETWVXw4A5mIo3nOQZZnNLawhtL2FOq','ESTUDIANTE',0,1,0,NULL,0,NULL,0,NULL),(4,'Mariana','Zapata','mariana@sige.edu.do','$2a$10$AI93b/ZNojwr7TE0y90adekcgJSODEBEh/Ry0Z3..6PSBtvtcITE2','PADRE',0,1,0,NULL,0,NULL,0,NULL);
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

