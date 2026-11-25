# Historial de cambios (change-log)

| Versión | Fecha | Cambios principales |
|---|---|---|
| v1.1.0 | — | Panel principal animado, estaciones del año, modos de visión y accesibilidad. |
| v1.2.0 | — | Mejoras de interfaz y documentación (ver `docs/NOVEDADES_v1.2.0.md`). |
| v1.3.0 | — | Extensiones escolares: anuncios, horarios, conducta, boleta oficial y carnet con QR; seguridad por rol; JaCoCo 0.8.15 (ver `docs/NOVEDADES_v1.3.0.md`). |
| v1.4.0 | 2026-11-13 | Roles director/a y secretario/a; creación de personal por el administrador; avisos por correo (Brevo/SMTP/manual); icono ❓ «Acerca de»; menú de escritorio desplazable; migración a TiDB Cloud. |
| Aula v1.1.0 | pendiente de publicar | Dominó para dos jugadores en línea; icono ❓ «Acerca de» (repositorio `sige-aula`). |

Completar las fechas vacías con `git log --date=short --format="%ad %h %s"` y `git tag`.
TiDB no es MySQL al 100 %.

- 2026-11-18: Registro del error real en `saveGrade` (INC-009).
- 2026-11-18: Producción: `sessionVariables=tidb_enable_noop_functions=ON` en `SIGE_DB_URL` (INC-009).
- 2026-11-19: Respaldo: `mysqldump` sin `--single-transaction` (TiDB no admite SAVEPOINT).
- 2026-11-19: Producción: bloqueo de IP de claves API desactivado en Brevo (INC-010).
- 2026-11-19: Nueva función: eliminar usuario (solo ADMIN).
- 2026-11-21: Modelo de evaluación académica: cálculo de índice escolar, condición del alumno y honores (`IndiceAcademico.java`).
- 2026-11-21: Reglas escolares de asistencia: control de derecho a examen ordinario y alertas por inasistencia (`ReglasAsistencia.java`).
- 2026-11-21: Evaluación disciplinaria escolar: ponderación de méritos, faltas y alertas de orientación (`EvaluacionConducta.java`).
- 2026-11-21: Validador de horarios escolares: prevención de choque o cruce de horarios en secciones (`ValidadorHorarios.java`).
- 2026-11-21: Malla curricular: validación de prerrequisitos de asignaturas y límite de carga académica (`ValidadorCurricular.java`).
- 2026-11-21: Identificadores escolares: validación de matrículas, códigos de asignatura y RNE MINERD (`IdentificadorEscolar.java`).
- 2026-11-21: Recuperación académica: cálculo oficial MINERD de pruebas completivas y extraordinarias (`CalculoRecuperacion.java`).
- 2026-11-21: Capacidad escolar: gestión de aforo, disponibilidad de cupos y prevención de sobrecupo (`ValidadorCapacidadSeccion.java`).
- 2026-11-21: Admisión por edad: validación de edad cronológica y detección de sobreedad (`ValidadorEdadEstudiante.java`).
- 2026-11-21: Identidad nacional: validación con algoritmo oficial Módulo 10 de Cédula Dominicana (`ValidadorCedulaDominicana.java`).
- 2026-11-21: Contacto escolar: validación y máscara de teléfonos de emergencia dominicanos 809/829/849 (`ValidadorTelefonoDominicano.java`).
- 2026-11-21: Certificación oficial: validación de Libro, Folio y Acta MINERD para récord de notas (`ValidadorActaCalificaciones.java`).
- 2026-11-21: Correos escolares: validación de dominios educativos y sugerencia de usuarios (`ValidadorCorreoInstitucional.java`).
- 2026-11-21: Registro de centro: validación de código de centro educativo MINERD y distrito escolar (`ValidadorCentroEducativo.java`).
- 2026-11-21: Biblioteca escolar: algoritmo de checksum para ISBN-10 e ISBN-13 en libros de texto (`ValidadorBibliotecaEscolar.java`).
- 2026-11-21: Becas escolares: reglas de asignación por índice académico y descuentos en matrícula (`ValidadorBecaAcademica.java`).
- 2026-11-21: Servicio social escolar: certificación de 60 horas comunitarias obligatorias MINERD (`ControlServicioSocial.java`).
- 2026-11-21: Emergencias médicas: validación de 911, contactos y grupos sanguíneos en fichas (`ValidadorEmergenciaMedica.java`).
- 2026-11-21: Salud escolar: validación de exequátur médico y esquema de vacunación estudiantil (`ValidadorCertificadoSaludEscolar.java`).
- 2026-11-21: Registro civil escolar: validación de actas de nacimiento JCE para admisión (`ValidadorActaNacimientoEscolar.java`).
- 2026-11-21: Identidad y diferenciación de producto: adopción del Código Único de Estudiante autónomo (CUE `SIGE-YYYY-XXXXX`), actas y suficiencia académica propia (`docs/IDENTIDAD_Y_DIFERENCIACION_SIGE.md`).
- 2026-11-23: Verificación documental: códigos de integridad `DOC-XXXX-XXXX-XXXX` y URLs institucionales (`ValidadorFirmaDigitalDocumento.java`).
- 2026-11-23: Gestión de profesorado: cálculo y prevención de sobrecarga docente y límites de secciones (`CalculadorCargaDocente.java`).
- 2026-11-23: Bienestar escolar: validación reglamentaria de excusas y reprogramación de exámenes (`ValidadorExcusaEscolar.java`).
- 2026-11-23: Analítica de retención: índice predictivo de riesgo de deserción y abandono escolar (`CalculadorRiesgoDesercion.java`).
- 2026-11-23: Credenciales QR: carnets estudiantiles inteligentes con payload de seguridad (`ValidadorCarnetInteligente.java`).
- 2026-11-23: Finanzas escolares: cálculo de mensualidades, descuentos familiares y control de mora (`CalculadorFinanzasEscolares.java`).
- 2026-11-23: Traslados de alumnos: gestión de transferencias escolares y paz y salvo financiero (`ValidadorTransferenciaEscolar.java`).
- 2026-11-23: Formación integral: inscripción y límites de clubes extracurriculares (`ValidadorClubExtracurricular.java`).
- 2026-11-23: Transporte escolar: asignación y validación de rutas y paradas escolares (`ValidadorTransporteEscolar.java`).
- 2026-11-23: Comedor escolar: gestión de planes nutricionales y prevención de alérgenos (`ValidadorComedorEscolar.java`).
- 2026-11-23: Préstamo de tecnología: control de tablets, laptops y mora en devoluciones (`ValidadorPrestamoDispositivo.java`).
- 2026-11-23: Calidad docente: evaluación multidimensional del profesorado (`CalculadorEvaluacionDocente.java`).
- 2026-11-23: Infraestructura: casilleros estudiantiles con nomenclatura por pabellón (`ValidadorCasilleroEscolar.java`).
- 2026-11-23: Democracia escolar: sistema de votación para el consejo estudiantil (`ValidadorVotoEstudiantil.java`).
- 2026-11-23: Seguridad de salida: entrega de alumnos mediante PIN de terceros autorizados (`ValidadorRetiroEstudiante.java`).
- 2026-11-23: Idoneidad académica: validación de títulos universitarios de educadores (`ValidadorTituloDocente.java`).
- 2026-11-23: Salas de cómputo: inventario y validación de MAC en laboratorios STEM (`ValidadorLaboratorioInformatica.java`).
- 2026-11-23: Diagnóstico pedagógico: pruebas de entrada y refuerzo académico (`CalculadorPruebaDiagnostica.java`).
- 2026-11-23: Admisión escolar: control y completitud de expedientes de nuevo ingreso (`ValidadorAdmisionNuevoIngreso.java`).
- 2026-11-23: Feria científica: baremos de evaluación de proyectos y prototipos (`CalculadorFeriaCientifica.java`).
- 2026-11-23: Banco de libros: control de préstamos, conservación física y reposición (`GestorBancoLibrosTexto.java`).
- 2026-11-23: Familia y escuela: citaciones formales a tutores por la dirección (`ValidadorCitacionPadres.java`).
- 2026-11-23: Salidas escolares: validación de seguro y autorizaciones de excursiones (`ValidadorExcursionEscolar.java`).
- 2026-11-23: Deporte escolar: baremos de rendimiento y aptitud física (`CalculadorAptitudFisica.java`).
- 2026-11-23: Salud escolar: historial de enfermería y primeros auxilios (`ValidadorEnfermeriaEscolar.java`).
- 2026-11-23: Divulgación estudiantil: gestión del periódico y publicaciones escolares (`GestorPublicacionEscolar.java`).
- 2026-11-23: Escuela de padres: seguimiento de asistencia a talleres familiares (`GestorEscuelaParaPadres.java`).
- 2026-11-23: Comunidad Alumni: registro y seguimiento a egresados de bachillerato (`ValidadorRedEgresados.java`).













- 2026-11-23: Mejoras del protocolo de pruebas: perfil Maven `integration` + MySQL desechable (`deploy/db-test/`), umbral de cobertura en CI (`verificar-umbral.sh`, paquete `com.unev.sige.model`) y contratos de cabeceras/cookie (`SeguridadWeb`, `SeguridadWebTest`, `ContratosHttpTest`).
- 2026-11-23: Herramientas escolares: las 36 reglas del paquete `model` quedan visibles por rol en la web (`/herramientas`) y en el escritorio (menú «Herramientas»), sin guardar datos (`CatalogoHerramientas`, `HerramientasWeb`, `HerramientasPanel`). Corrige además la detección de alérgenos compuestos del comedor.
- 2026-11-24: Registros de herramientas escolares: tablas `herramienta_registros` y `herramienta_registro_datos` (`sql/fase_herramientas_escolares.sql`), botón «Calcular y guardar», «Mis registros» y descarga en PDF (uno o el listado) en web y escritorio; los campos sensibles no se guardan, límite de 200 por usuario y borrado de registros al eliminar la cuenta.
- 2026-11-24: Herramientas escolares: submenú plegable por grupos, orden, iconos, diseño y animaciones (web y escritorio).
- 2026-11-24: Iconos en el menú web para Herramientas (🧰), Mis registros (💾) y Reporte (📑).
- 2026-11-24: Planes de servicio (Esencial/Estándar/Institucional): tabla suscripcion_centro, web /planes y panel de escritorio, solo ADMIN cambia.
- 2026-11-24: Carnet de identidad del personal (profesor, director/a, secretario/a): web /carnet-personal y /personal-carnets, panel de escritorio, color por rol, plantilla compartida con el carnet estudiantil (ver docs/CARNET_PERSONAL.md).
- 2026-11-24: Ejecutable sin credenciales: asistente de primer arranque, configuración por equipo en %APPDATA%\SIGE, empaquetar.bat ya no copia config.properties (ver docs/EJECUTABLE_PRIMER_ARRANQUE.md).
