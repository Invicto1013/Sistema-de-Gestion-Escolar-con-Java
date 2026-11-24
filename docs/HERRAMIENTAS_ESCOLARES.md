# Herramientas escolares

Módulo que hace visibles, según el rol, las reglas escolares del paquete `com.unev.sige.model` (calculadoras y validadores). Está disponible en la **web** (`/herramientas`) y en el **escritorio** (opción «Herramientas» del menú lateral).

## Qué hace y qué no hace
- Cada herramienta tiene un formulario con valores de ejemplo y muestra el resultado de la regla.
- «Calcular» solo consulta. **«Calcular y guardar»** guarda el cálculo (datos ingresados y resultado) para verlo después y descargarlo en PDF; ver «Registros guardados».
- El administrador ve todas; los demás roles ven solo las de la tabla.
- Si un dato no se puede interpretar (número, fecha, formato), se muestra un mensaje en lugar de un error.
- Web y escritorio comparten el mismo catálogo (`CatalogoHerramientas`), así que no pueden diferir.

## Herramientas por rol
| Herramienta | Grupo | Director/a | Secretario/a | Profesor | Estudiante | Padre / Tutor |
|---|---|:-:|:-:|:-:|:-:|:-:|
| Aptitud física | Académico | ✓ |  | ✓ |  |  |
| Estadísticas de un grupo | Académico | ✓ |  | ✓ |  |  |
| Prueba diagnóstica | Académico | ✓ |  | ✓ |  |  |
| Riesgo de deserción | Académico | ✓ |  | ✓ |  |  |
| Créditos académicos | Académico | ✓ | ✓ |  | ✓ |  |
| Proyecto integrador | Académico | ✓ |  | ✓ | ✓ |  |
| Plan de refuerzo académico | Académico | ✓ |  | ✓ | ✓ |  |
| Feria científica | Académico | ✓ |  | ✓ |  |  |
| Días hábiles del calendario | Académico | ✓ | ✓ | ✓ | ✓ | ✓ |
| Carga docente | Docentes | ✓ | ✓ |  |  |  |
| Evaluación docente | Docentes | ✓ |  |  |  |  |
| Título del docente | Docentes | ✓ | ✓ |  |  |  |
| Reunión de consejo | Docentes | ✓ | ✓ | ✓ |  |  |
| Excusa de inasistencia | Bienestar y seguridad | ✓ | ✓ |  | ✓ | ✓ |
| Comedor y alérgenos | Bienestar y seguridad |  | ✓ |  | ✓ | ✓ |
| Retiro de estudiante | Bienestar y seguridad | ✓ | ✓ |  |  | ✓ |
| Permiso de permanencia | Bienestar y seguridad | ✓ | ✓ | ✓ |  | ✓ |
| Transporte escolar | Bienestar y seguridad | ✓ | ✓ |  |  | ✓ |
| Código de atención de enfermería | Bienestar y seguridad | ✓ | ✓ | ✓ |  |  |
| Código de excursión | Bienestar y seguridad | ✓ | ✓ | ✓ |  |  |
| Código de citación a padres | Bienestar y seguridad | ✓ | ✓ |  |  |  |
| Mensualidad y mora | Administración | ✓ | ✓ |  |  | ✓ |
| Inventario escolar | Administración | ✓ | ✓ |  |  |  |
| Banco de libros | Administración |  | ✓ | ✓ |  |  |
| Préstamo de dispositivos | Administración |  | ✓ | ✓ | ✓ |  |
| Transferencia escolar | Administración | ✓ | ✓ |  |  | ✓ |
| Código de solicitud de admisión | Administración | ✓ | ✓ |  |  |  |
| Escuela de padres | Familia y comunidad | ✓ | ✓ |  |  | ✓ |
| Clubes extracurriculares | Familia y comunidad |  |  | ✓ | ✓ | ✓ |
| Voto estudiantil | Familia y comunidad | ✓ |  | ✓ | ✓ |  |
| Código de egresado | Familia y comunidad | ✓ | ✓ |  |  |  |
| Código de publicación estudiantil | Familia y comunidad |  | ✓ | ✓ | ✓ |  |
| Carnet digital con QR | Documentos y códigos | ✓ | ✓ |  | ✓ | ✓ |
| Casillero escolar | Documentos y códigos |  | ✓ |  | ✓ |  |
| Documento certificado | Documentos y códigos | ✓ | ✓ |  |  |  |
| Equipo de laboratorio (MAC) | Documentos y códigos | ✓ |  | ✓ |  |  |

## Notas
- Código: `HerramientaEscolar` (modelo del formulario), `CatalogoHerramientas` (las 36 entradas), `HerramientasWeb` (HTML) y `HerramientasPanel` (JavaFX).
- Pruebas: `CatalogoHerramientasTest`, `HerramientasWebTest` y `ContratosHttpTest` (la web exige sesión).
- Para agregar una herramienta: crear la regla en `model`, añadir una entrada en `CatalogoHerramientas` con sus roles y una prueba.
- Las reglas son de apoyo. No sustituyen los reglamentos oficiales ni son expedientes: el PDF indica «solo consulta, sin valor oficial».

## Registros guardados y PDF
- **Dónde:** web `/herramientas-registros` (enlace «Mis registros» en cada herramienta) y escritorio, opción «Mis registros».
- **Quién ve qué:** cada usuario ve los suyos; **Administrador y Director** ven los de todos. Eliminar: el dueño o el Administrador (si lo borra otra persona queda en la auditoría `HERRAMIENTA_REGISTRO_ELIMINADO`).
- **PDF:** de un registro (datos + resultado) o del listado completo, desde la web y el escritorio.
- **No se guardan** campos sensibles: en «Retiro de estudiante» no se guardan la cédula ni los PIN (se muestra «(no se guarda)»).
- **Límite:** 200 registros por usuario; al llegar se pide eliminar alguno.
- **Al eliminar una cuenta** (Admin → Usuarios) se borran también sus registros.

### Tablas nuevas (`sql/fase_herramientas_escolares.sql`, ya incluidas en `sql/sige_completo.sql`)
| Tabla | Contenido |
|---|---|
| `herramienta_registros` | Un renglón por cálculo guardado: usuario, herramienta, título, resultado y fecha. |
| `herramienta_registro_datos` | Los campos del formulario de cada registro (orden, etiqueta, valor). |

### Cómo activarlo en una base que ya existe (TiDB Cloud / MySQL)
1. Haz un respaldo (workflow «Respaldo de la base de datos»).
2. Ejecuta el contenido de `sql/fase_herramientas_escolares.sql` en la consola SQL de TiDB (o `mysql ... < sql/fase_herramientas_escolares.sql`). Es idempotente: se puede repetir.
3. Despliega la nueva versión. **Si despliegas antes del paso 2**, las herramientas siguen funcionando; solo «guardar» y «Mis registros» mostrarán el aviso «El guardado aún no está activado».
