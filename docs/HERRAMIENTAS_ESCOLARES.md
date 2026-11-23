# Herramientas escolares

Módulo que hace visibles, según el rol, las reglas escolares del paquete `com.unev.sige.model` (calculadoras y validadores). Está disponible en la **web** (`/herramientas`) y en el **escritorio** (opción «Herramientas» del menú lateral).

## Qué hace y qué no hace
- Cada herramienta tiene un formulario con valores de ejemplo y muestra el resultado de la regla. Los datos solo se usan para calcular: **no se guardan** en la base de datos.
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
- Las reglas son de apoyo. No sustituyen los reglamentos oficiales ni guardan expedientes; guardar registros requeriría tablas nuevas en el SQL.
