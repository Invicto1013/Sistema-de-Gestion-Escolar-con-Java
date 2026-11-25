# Evidencias de regresión

## Línea base registrada
| Fecha | Comando | Resultado real |
|---|---|---|
| 2026-11-13 16:13 (-04:00) | `mvn clean test "-Djacoco.skip=true"` | `Tests run: 187, Failures: 0, Errors: 0, Skipped: 15` — `BUILD SUCCESS` (34 s). JaCoCo omitido con `jacoco.skip`. |

Las 15 pruebas omitidas no cuentan como aprobadas: corresponden a pruebas que requieren recursos externos (por ejemplo base de datos de integración). Verificar cuáles con `mvn test` sin la omisión y la salida de Surefire en `target/surefire-reports/`.

## Ejecución tras la mejora de la práctica
Completar con datos reales después de ejecutar (no dejar texto inventado).

| Fecha | Rama / commit | Comando | Ejecutadas | Fallos | Errores | Omitidas | Evidencia (captura / log) |
|---|---|---|---|---|---|---|---|
| 2026-11-15 19:31 | `feature/practica-agentes` | `mvn clean test "-Djacoco.skip=true"` | 192 | 0 | 0 | 15 | capturas/05-suite-completa.png |

## Pruebas manuales en producción (Render + TiDB)

| # | Caso | Rol | Resultado | Fecha | Evidencia |
|---|---|---|---|---|---|
| 1 | Acceso por rol: cada rol ve solo lo que le corresponde | ADMIN, DIRECTOR, SECRETARIA, PROFESOR, ESTUDIANTE, PADRE | Aprobado | (fecha de tus capturas) | m1-*.png |
| 2 | Guardar calificaciones | PROFESOR | Aprobado tras corregir INC-009 (1.er intento falló) | 2026-11-18 | m2-calificaciones-guardadas.png |
| 3 | Boleta con notas actualizadas (ING-101: 89 / 80 / 99, final 90.3 A) | ESTUDIANTE | Aprobado | 2026-11-18 | m3-boleta.png |
| 4 | Health check de producción (`/health` responde `ok`) | Público | Aprobado | 2026-11-18 | m4-health.png |
| 5 | SSO de SIGE al Aula Virtual sin pedir login | PROFESOR | Aprobado | 2026-11-18 | m5-sso-aula.png |
| 6 | Crear personal y enviar aviso por correo (sin contraseña) | ADMIN | Aprobado tras corregir INC-010 (1.er intento: `notificacion=fallida`; 2.º: `ok`) | 2026-11-19 | m6-crear-personal.png, m6-correo.png |

Los casos 2 y 6 fallaron la primera vez y se corrigieron durante la práctica. Eso es lo esperado de una
prueba de regresión: encontró dos incompatibilidades con el entorno de producción que las pruebas
automáticas no detectaban (MySQL local frente a TiDB, y configuración de Brevo).

## Evidencia de CI
Pegar los enlaces y evidencias correspondientes a los flujos automatizados de GitHub Actions:

| Workflow | Propósito | Enlace / Ejecución | Estado | Evidencia |
|---|---|---|---|---|
| `ci.yml` (Verify and package) | Compilación multiplataforma (Windows, Linux, macOS), pruebas unitarias y generación de artefactos portables | [PR #13](https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java/pull/13) | Aprobado (linux, macos, windows) | capturas/06-ci-pr13.png |
| `cobertura.yml` (JaCoCo) | Análisis de cobertura de código y generación de informe HTML | [PR #13](https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java/pull/13) | Aprobado | capturas/06-ci-pr13.png |
| `codeql.yml` (CodeQL) | Análisis de seguridad estática (SAST: prevención de SQLi, XSS, etc.) | [PR #13](https://github.com/Invicto1013/Sistema-de-Gestion-Escolar-con-Java/pull/13) | Aprobado | capturas/06-ci-pr13.png |

## Demostración: la regresión se detecta (rama `feature/practica-agentes`)
Prueba nueva: `PermisosRegresionTest` (fija la tabla completa de permisos por rol).

1. **Antes (línea base):** `mvn test "-Djacoco.skip=true" -Dtest=PermisosRegresionTest` → anotar el resultado real.
2. **Cambio que rompe a propósito (solo para la demostración, sin hacer commit):** en `Permisos.publicaAnuncios` añadir `Rol.PROFESOR` a la lista.
3. **Durante:** repetir el comando; debe **fallar** con «publicaAnuncios para PROFESOR». Guardar captura de la salida.
4. **Revertir:** `git restore src/main/java/com/unev/sige/model/Permisos.java`.
5. **Después:** ejecutar `mvn clean test "-Djacoco.skip=true"` completo y anotar el total en la tabla de arriba.

| Paso | Fecha | Resultado real | Evidencia |
|---|---|---|---|
| 1. Antes | 2026-11-15 19:09 | `PermisosRegresionTest`: 5 ejecutadas, 0 fallos, 0 errores. BUILD SUCCESS | capturas/01-antes.png |
| 2–3. Con el cambio roto (`Rol.PROFESOR` añadido a `publicaAnuncios`, sin commit) | 2026-11-15 19:25 | 5 ejecutadas, 1 fallo: `publicaAnuncios para PROFESOR ==> expected: <false> but was: <true>`. BUILD FAILURE | capturas/02-cambio-roto-y-fallo.png, capturas/03-fallo-resumen.png |
| 4. Revertido | 2026-11-15 (después de las 19:25) | `git restore` y `git diff` sin salida | capturas/04-revertido-diff-vacio.png |
| 5. Después (suite completa) | 2026-11-15 19:31 | 192 ejecutadas, 0 fallos, 0 errores, 15 omitidas. BUILD SUCCESS | capturas/05-suite-completa.png |

Nota: un primer intento a las 17:15 también falló como se esperaba, pero sus capturas se tomaron después de revertir y no mostraban la línea modificada; por eso la evidencia principal es la repetición de las 19:xx.

---

## Mejoras al protocolo de pruebas (casos definidos, ejecución pendiente)
Los puntos marcados `[x]` indican que el caso quedó **definido en este documento**; su ejecución manual está pendiente en la tabla «Casos manuales».

- [x] **Completitud y trazabilidad de casos manuales:** Se definieron roles exactos, pasos estructurados y resultados esperados para cada caso manual, alineados con `Permisos.java`.
- [x] **Modelo académico de índice y honores escolares (`IndiceAcademico.java`):** Se implementó la lógica de dominio con cálculo exacto vía `BigDecimal` del promedio general, índice por boleta, determinación de condición académica (`EXCELENCIA`, `REGULAR`, `ALERTA`, `A_PRUEBA`) y honores académicos (`SUMMA_CUM_LAUDE`, `MAGNA_CUM_LAUDE`, `CUM_LAUDE`), respaldado con su suite de pruebas unitarias (`IndiceAcademicoTest`).
- [x] **Reglas escolares de asistencia y derecho a examen (`ReglasAsistencia.java`):** Implementación de la normativa académica de asistencia (umbral mínimo del 80% para derecho a examen, estado de alerta entre 70% y 79%, y pérdida de derecho/reprobación por inasistencia < 70%), con cálculo exacto y suite de pruebas unitarias (`ReglasAsistenciaTest`).
- [x] **Evaluación formativa y disciplinaria escolar (`EvaluacionConducta.java`):** Ponderación de méritos, tardanzas y faltas conductuales escolares con escala de convivencia (A, B, C, D) y detección automática de necesidad de intervención por Orientación y Psicología (`EvaluacionConductaTest`).
- [x] **Detección y prevención de choques de horarios (`ValidadorHorarios.java`):** Algoritmo de detección de solapamiento de bloques horarios para estudiantes, docentes y aulas en el mismo día lectivo (`ValidadorHorariosTest`).
- [x] **Validación de prerrequisitos y tope de carga académica (`ValidadorCurricular.java`):** Verificación de materias previas aprobadas antes de admitir inscripción y control de límite reglamentario de materias por período (`ValidadorCurricularTest`).
- [x] **Normalización y validación de identificadores escolares (`IdentificadorEscolar.java`):** Validación estricta por expresiones regulares de matrículas institucionales, códigos de asignatura y RNE (Registro Nacional de Estudiante MINERD) (`IdentificadorEscolarTest`).
- [x] **Cálculo de recuperación académica completiva y extraordinaria (`CalculoRecuperacion.java`):** Algoritmo oficial de ponderación MINERD para pruebas de recuperación completivas (50/50) y extraordinarias (30/70) con su suite de pruebas (`CalculoRecuperacionTest`).
- [x] **Control de capacidad física y aforo de secciones (`ValidadorCapacidadSeccion.java`):** Evaluación de disponibilidad de cupos, últimos cupos, cupo lleno y prevención de sobrecupo escolar (`ValidadorCapacidadSeccionTest`).
- [x] **Validación de edad cronológica y sobreedad escolar (`ValidadorEdadEstudiante.java`):** Verificación de edad reglamentaria por nivel escolar (Inicial, Primaria, Secundaria) y detección temprana de desfase/sobreedad (`ValidadorEdadEstudianteTest`).
- [x] **Validación algorítmica de Cédula Dominicana (`ValidadorCedulaDominicana.java`):** Implementación del algoritmo oficial Módulo 10 (Luhn con pesos alternados) y formateador visual para personal docente y tutores (`ValidadorCedulaDominicanaTest`).
- [x] **Validación y máscara de teléfonos de emergencia dominicanos (`ValidadorTelefonoDominicano.java`):** Control estricto de códigos de área RD (809, 829, 849) y formateo estandarizado `(809) 000-0000` para fichas de emergencia (`ValidadorTelefonoDominicanoTest`).
- [x] **Registro y certificación de actas oficiales MINERD (`ValidadorActaCalificaciones.java`):** Validación de consistencia para Libro, Folio y Número de Acta oficial en emisión de récords de notas y certificados (`ValidadorActaCalificacionesTest`).
- [x] **Gestión y sugerencia de correos institucionales (`ValidadorCorreoInstitucional.java`):** Control de dominios educativos, distinción frente a correos de padres y generación estándar de cuentas (`ValidadorCorreoInstitucionalTest`).
- [x] **Validación de Código de Centro Escolar y Regionales (`ValidadorCentroEducativo.java`):** Validación de código de centro educativo MINERD (5 a 8 dígitos), RNC institucional y distritos escolares (`ValidadorCentroEducativoTest`).
- [x] **Control bibliotecario y catalogación escolar (`ValidadorBibliotecaEscolar.java`):** Validación algorítmica de checksum para ISBN-10 e ISBN-13 en recursos didácticos (`ValidadorBibliotecaEscolarTest`).
- [x] **Gestión de becas académicas y exoneraciones (`ValidadorBecaAcademica.java`):** Reglas de asignación automática de becas (100%, 50%, 25%) según índice acumulado y condicionante disciplinario (`ValidadorBecaAcademicaTest`).
- [x] **Control de Servicio Social Estudiantil MINERD (`ControlServicioSocial.java`):** Seguimiento y acreditación de las 60 horas obligatorias de labor comunitaria para graduación de bachillerato (`ControlServicioSocialTest`).
- [x] **Líneas y protocolos de emergencia médica escolar (`ValidadorEmergenciaMedica.java`):** Validación de líneas cortas (911) y teléfonos médicos, junto al control de grupos sanguíneos en fichas escolares (`ValidadorEmergenciaMedicaTest`).
- [x] **Acreditación de salud y exequátur médico (`ValidadorCertificadoSaludEscolar.java`):** Validación de certificados médicos, esquema PAI y formato de exequátur oficial de Salud Pública (`ValidadorCertificadoSaludEscolarTest`).
- [x] **Registro civil de actas de nacimiento escolares (`ValidadorActaNacimientoEscolar.java`):** Normalización y validación de Oficialía, Libro, Folio y Acta de la JCE para el expediente de ingreso (`ValidadorActaNacimientoEscolarTest`).
- [x] **Diferenciación e identidad propia de la plataforma (`IDENTIDAD_Y_DIFERENCIACION_SIGE.md`):** Desacoplamiento de nomenclaturas gubernamentales externas mediante Código Único de Estudiante (CUE: `SIGE-YYYY-XXXXX`), actas institucionales con código de verificación, suficiencia académica propia y perfiles de campus/sedes independientes.
- [x] **Firma e integridad digital de boletines (`ValidadorFirmaDigitalDocumento.java`):** Generación de tokens criptográficos `DOC-XXXX-XXXX-XXXX` y enlace de verificación pública institucional (`ValidadorFirmaDigitalDocumentoTest`).
- [x] **Gestión y control de carga docente (`CalculadorCargaDocente.java`):** Control de límites horarios semanales (Tiempo Completo vs Medio Tiempo) y tope de secciones para evitar sobrecarga del profesorado (`CalculadorCargaDocenteTest`).
- [x] **Reglamentación de excusas y permisos escolares (`ValidadorExcusaEscolar.java`):** Plazos de justificación (hasta 5 días), requerimiento de certificado médico y habilitación de reprogramación de exámenes (`ValidadorExcusaEscolarTest`).
- [x] **Analítica predictiva de retención y riesgo de deserción (`CalculadorRiesgoDesercion.java`):** Ponderación de inasistencias, bajas notas y conducta para detección temprana de alerta escolar (`CalculadorRiesgoDesercionTest`).
- [x] **Carnets inteligentes con código QR escolar (`ValidadorCarnetInteligente.java`):** Generación y validación de payloads `SIGE-ID:{CUE}:{ANO}:{FIRMA}` para control de acceso y biblioteca (`ValidadorCarnetInteligenteTest`).
- [x] **Gestión financiera y colegiaturas escolares (`CalculadorFinanzasEscolares.java`):** Cálculo de descuentos por hermanos, pronto pago y recargos por mora en mensualidades (`CalculadorFinanzasEscolaresTest`).
- [x] **Protocolo de transferencias escolares interinstitucionales (`ValidadorTransferenciaEscolar.java`):** Control de pases de matrícula, paz y salvo financiero y traslado de expediente (`ValidadorTransferenciaEscolarTest`).
- [x] **Actividades y clubes extracurriculares (`ValidadorClubExtracurricular.java`):** Gestión de deportes, arte, robótica y límites de participación complementaria (`ValidadorClubExtracurricularTest`).
- [x] **Gestión y control de transporte escolar (`ValidadorTransporteEscolar.java`):** Validación de rutas escolares `RUTA-XX`, paradas y autorizaciones de padres (`ValidadorTransporteEscolarTest`).
- [x] **Control de comedor escolar y alérgenos (`ValidadorComedorEscolar.java`):** Validación de planes nutricionales y filtros de alérgenos alimentarios para la cocina (`ValidadorComedorEscolarTest`).
- [x] **Control de préstamos de dispositivos tecnológicos (`ValidadorPrestamoDispositivo.java`):** Control de seriales, fechas esperadas y cálculo de días de mora (`ValidadorPrestamoDispositivoTest`).
- [x] **Evaluación institucional del desempeño docente (`CalculadorEvaluacionDocente.java`):** Ponderación de dominio, puntualidad y pedagogía con escalas cualitativas (`CalculadorEvaluacionDocenteTest`).
- [x] **Gestión de casilleros escolares inteligentes (`ValidadorCasilleroEscolar.java`):** Nomenclatura `CAS-X-000` por pabellón y asignación a estudiantes (`ValidadorCasilleroEscolarTest`).
- [x] **Gestión de voto y elecciones estudiantiles (`ValidadorVotoEstudiantil.java`):** Control de elecciones para el gobierno escolar y períodos de sufragio (`ValidadorVotoEstudiantilTest`).
- [x] **Control y entrega segura de estudiantes (`ValidadorRetiroEstudiante.java`):** Validación de PIN y autorización de retiro de alumnos por terceros (`ValidadorRetiroEstudianteTest`).
- [x] **Acreditación y títulos del profesorado (`ValidadorTituloDocente.java`):** Validación de registros oficiales de títulos universitarios y años de graduación (`ValidadorTituloDocenteTest`).
- [x] **Seguridad y redes en laboratorios STEM (`ValidadorLaboratorioInformatica.java`):** Validación de direcciones MAC y equipos en salas de cómputo (`ValidadorLaboratorioInformaticaTest`).
- [x] **Evaluación diagnóstica y refuerzo pedagógico (`CalculadorPruebaDiagnostica.java`):** Evaluación de entrada en matemáticas y lectura para detección de rezago (`CalculadorPruebaDiagnosticaTest`).
- [x] **Admisión y nuevo ingreso (`ValidadorAdmisionNuevoIngreso.java`):** Códigos `ADM-YYYY-XXXXX` y validación de completitud del expediente del postulante (`ValidadorAdmisionNuevoIngresoTest`).
- [x] **Evaluación de ferias científicas e innovación (`CalculadorFeriaCientifica.java`):** Ponderación de rigor, originalidad y prototipo práctico para premiaciones Oro/Plata/Bronce (`CalculadorFeriaCientificaTest`).
- [x] **Banco de libros de texto (`GestorBancoLibrosTexto.java`):** Control del ciclo de préstamo y reposición de libros deteriorados o extraviados (`GestorBancoLibrosTextoTest`).
- [x] **Gestión de citaciones a tutores (`ValidadorCitacionPadres.java`):** Nomenclatura oficial `CIT-YYYY-XXXX` y seguimiento de acuerdos con padres (`ValidadorCitacionPadresTest`).
- [x] **Salidas pedagógicas y excursiones (`ValidadorExcursionEscolar.java`):** Control de póliza de seguro y cuórum del 80% de autorizaciones firmadas (`ValidadorExcursionEscolarTest`).
- [x] **Baremos de aptitud física escolar (`CalculadorAptitudFisica.java`):** Baremos deportivos en velocidad, resistencia, fuerza y flexibilidad (`CalculadorAptitudFisicaTest`).
- [x] **Historial clínico de enfermería (`ValidadorEnfermeriaEscolar.java`):** Registro de atenciones `ENF-YYYY-XXXX` y medicamentos administrados (`ValidadorEnfermeriaEscolarTest`).
- [x] **Publicaciones y revistas estudiantiles (`GestorPublicacionEscolar.java`):** Catalogación de artículos y ensayos escolares `PUB-XXX-YYYY-NNN` (`GestorPublicacionEscolarTest`).
- [x] **Escuela para padres y formación familiar (`GestorEscuelaParaPadres.java`):** Control de asistencia a talleres obligatorios de orientación familiar (`GestorEscuelaParaPadresTest`).
- [x] **Seguimiento a egresados y red Alumni (`ValidadorRedEgresados.java`):** Códigos `ALM-YYYY-XXXX` e inserción universitaria de exalumnos (`ValidadorRedEgresadosTest`).
- [x] **Estructuración de tabla de evidencias de CI:** Se especificó el propósito de cada pipeline (`ci.yml`, `cobertura.yml`, `codeql.yml`) con campos para enlaces, estado y captura verde.
- [x] **Regresión de período cerrado (Fase D):** Se incorporó la validación manual de bloqueo de edición de calificaciones para profesores en períodos lectivos con estado `CERRADO` y generación del evento `NOTA_DENEGADA`.
- [x] **Regresión de corrección excepcional de calificaciones:** Se añadió la comprobación de `/corregir-calificacion` garantizando auditoría con motivo mínimo de 10 caracteres (`NOTA_CORREGIDA`).
- [x] **Verificación de auditoría y zona horaria (Mejoras Fase D/E):** Se sumó el caso de prueba para constatar el registro de auditoría con zona horaria de República Dominicana (`America/Santo_Domingo`) y exportación funcional a CSV/Excel.
- [x] **Notificación de credenciales segura:** Se añadió verificación del flujo de correo vía Brevo/SMTP sin inclusión de contraseña en claro al dar de alta personal.
- [x] **Aislamiento de pruebas de integración (implementado, ejecución pendiente de confirmar en tu equipo):** Perfil `-P integration` y MySQL desechable en `deploy/db-test/` (ver `docs/PRUEBAS_INTEGRACION.md`). Original: configurar un perfil Maven específico (ej. `-P integration`) o contenedor de base de datos para ejecutar de forma reproducible las 15 pruebas actualmente marcadas como *Skipped*.
- [x] **Automatización de cobertura en CI (implementado; umbral inicial 60 % por confirmar con la primera ejecución):** `.github/scripts/verificar-umbral.sh` y paso en `cobertura.yml` sobre `com.unev.sige.model`. Original: reactivar JaCoCo en el flujo de CI para fijar un umbral mínimo de cobertura de código sobre `com.unev.sige.model` y controladores críticos.
- [x] **Validación automatizada de contratos de API REST (implementado, ejecución pendiente de confirmar):** `SeguridadWeb.cabecerasDePagina/cookieSesion` con pruebas unitarias y `ContratosHttpTest` (HTTP real, se omite si el servidor no arranca). Original: implementar pruebas automáticas que verifiquen códigos HTTP y cabeceras de seguridad (`SameSite=Lax`, `Content-Security-Policy`, `X-Frame-Options`) en los endpoints clave.
- [x] **Integración de las reglas escolares a la interfaz (`CatalogoHerramientas.java`, `HerramientasWeb.java`, `HerramientasPanel.java`):** las 36 reglas del paquete `model` se consultan por rol en la web (`/herramientas`) y en el escritorio, sin guardar datos (`CatalogoHerramientasTest`, `HerramientasWebTest`). Ejecución manual pendiente: abrir «Herramientas» con cada rol (ver `docs/HERRAMIENTAS_ESCOLARES.md`).
- [x] **Guardado y PDF de las herramientas escolares (`HerramientaRegistroDAO.java`, `ReportePdfHerramientas.java`, `fase_herramientas_escolares.sql`):** pruebas `RegistroHerramientaTest`, `ReportePdfHerramientasTest`, `ScriptsSqlHerramientasTest`, `HerramientaRegistroDAOIntegracionTest` (esta última requiere `-P integration`). Ejecución manual pendiente: guardar un cálculo, ver «Mis registros», descargar el PDF y eliminarlo (web y escritorio); confirmar el script en TiDB.
- 2026-11-24: CatalogoHerramientasTest/HerramientasWebTest (27) OK con shim; capturas web claro/oscuro/móvil revisadas; panel JavaFX sin compilar en sandbox.
- 2026-11-24: CatalogoPlanesTest, PlanesWebTest, ScriptsSqlPlanesTest, IconosWebTest OK (13) con shim; captura de /planes revisada; PlanesPanel (JavaFX) y SuscripcionDAO sin compilar en sandbox.
- 2026-11-24: GeneradorDocumentosTest (+4), PermisosTest (+2), IconosWebTest (+1) OK (18) con runner propio y stubs de zxing/bcrypt; ContratosHttpTest (+1) y paneles JavaFX sin ejecutar en sandbox: validar con mvn clean test y probar con un profesor, un director y una secretaria.
- 2026-11-24: ConfiguracionUsuarioTest (9) OK con runner propio; ConfiguracionInicialDialog/App (JavaFX) y ConexionBD (Hikari) sin compilar en sandbox: validar con mvn clean test y abrir el .exe sin config.properties en un equipo limpio.
