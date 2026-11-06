# Créditos, licencias y originalidad

Documento de transparencia del proyecto. Sirve para demostrar qué es propio, qué es de terceros y cómo se verificó que no hay copia.

## 1. Componentes de terceros (dependencias)

| Componente | Uso | Licencia |
|---|---|---|
| OpenJFX (JavaFX) | Interfaz de escritorio | GPL v2 + Classpath Exception |
| MySQL Connector/J | Conexión a MySQL | GPL v2 con excepción FOSS |
| HikariCP | Pool de conexiones | Apache 2.0 |
| jBCrypt | Hash de contraseñas | ISC |
| SLF4J | Registro de eventos | MIT |
| Eclipse Angus (Jakarta Mail) | Correo | EPL 2.0 / EDL (BSD-3) |
| ZXing | Códigos QR (verificación en dos pasos) | Apache 2.0 |
| JUnit 5, JaCoCo | Pruebas y cobertura | EPL 2.0 |

Las licencias indicadas son las habituales de cada biblioteca; confírmalas en el sitio de cada una antes de publicar una versión final.
Aula virtual usa HikariCP, MySQL Connector/J, SLF4J y JUnit 5 con las mismas licencias.

## 2. Recursos no escritos como código

- **Paleta para daltonismo:** colores de la paleta Okabe-Ito (Okabe e Ito), pensada para ser distinguible en daltonismo.
- **Logo e ilustraciones (`logo.svg`, `login-hero.svg`, `login-hero.gif`, `dashboard-hero.svg`):** dibujos vectoriales creados para este proyecto, sin material de terceros.
- **Iconos de menú:** caracteres emoji de las fuentes del sistema; no se distribuyen imágenes.
- **Música:** el proyecto no incluye música.

## 3. Herramientas de desarrollo

El proyecto se desarrolló con asistencia de una herramienta de IA (Claude, de Anthropic) para escribir y revisar código y documentación. Las decisiones de diseño, las pruebas y la verificación en producción las realizó el autor. Ajusta esta sección a la política de tu institución sobre el uso de IA.

## 4. Verificación de similitud con otros proyectos

**Método:** se descargaron proyectos públicos de GitHub sobre gestión escolar en Java y se comparó su código con `src/main/java` de SIGE (109 archivos, unos 123 000 símbolos). Se quitaron comentarios y se compararon secuencias de 25 símbolos consecutivos, en dos variantes: literal (mismos nombres) y estructural (nombres de variables ignorados).

| Proyecto comparado | Coincidencia literal | Coincidencia estructural | Mayor coincidencia en un archivo |
|---|---|---|---|
| abou-dev/Projet-Java-School-Management-System | 0,04 % | 0,48 % | 5,3 % |
| Leemo-ui/java-studentManagement | 0,01 % | 0,45 % | 5,1 % |
| BK-KAVIYA/FOT_TECMIS | 0,02 % | 0,20 % | 5,4 % |
| hamletabr/Grading-System | 0,06 % | 0,14 % | 7,2 % |
| hasnatrasool163/-javafx-school-management-system | 0,00 % | 0,00 % | 0,0 % |

**Lectura:** los valores cercanos a cero son lo esperado entre proyectos independientes; las coincidencias pequeñas corresponden a código común del lenguaje (importaciones, bloques estándar de JDBC y JavaFX). No se encontró copia.

**Límites:** solo se compararon los repositorios que pudieron descargarse (otros dos de la lista no estaban disponibles), y esto no sustituye una revisión de una herramienta oficial de la institución. Fecha de la verificación: 2026-11-06.

## 5. Nombre del proyecto

«SIGE» es una sigla usada por otros sistemas educativos (por ejemplo en Chile y El Salvador, y en proyectos de SourceForge). Este proyecto es independiente de ellos y no tiene relación con ninguno.
