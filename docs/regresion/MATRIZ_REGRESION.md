# Matriz de regresión — SIGE

Cada fila relaciona una función existente con las pruebas automáticas que la protegen y, cuando hace falta, una verificación manual.
**El resultado real se anota en `EVIDENCIAS.md` después de ejecutar; esta matriz no declara resultados.**

| # | Función | Pruebas automáticas (`src/test/java/...`) | Verificación manual tras un cambio | Crítica |
|---|---|---|---|---|
| 1 | Inicio de sesión y contraseñas | `PoliticaContrasenaTest`, `LimiteIntentosTest`, `TOTPTest`, `SesionWebDAOTest` | Entrar con un usuario por rol; 5 intentos fallidos bloquean | Sí |
| 2 | Roles y permisos | `PermisosTest`, `PermisosRegresionTest`, `UsuarioTest`, `SeguridadWebTest` | Cada rol ve solo su menú; un padre no abre datos de otro hijo | Sí |
| 3 | Calificaciones y escala | `CalificacionTest`, `EscalaCalificacionesTest`, `ReglasPeriodoTest` | Registrar una nota y ver el promedio en la boleta | Sí |
| 4 | Asistencia | `ResumenAsistenciaTest` | Marcar asistencia y comprobar el resumen | No |
| 5 | Boleta, carnet y exportaciones | `GeneradorDocumentosTest`, `PdfTablaTest`, `XlsxTablaTest` | Abrir `/boleta-oficial` y `/carnet`; imprimir/PDF legible | Sí |
| 6 | Anuncios, horarios y conducta | `ExtensionesEscolaresModelTest` | Crear y eliminar un anuncio (pantalla de confirmación) | No |
| 7 | Entrada única (SSO) al Aula | `AulaSsoTest` | Desde SIGE pulsar «Aula virtual» y entrar sin pedir clave | Sí |
| 8 | Conexión y esquema de base de datos | `ConexionBDConfiguracionTest`, `ConexionBDTest`, `ScriptsSqlUtf8Test`, `ConexionBDIntegracionTest`, `UsuarioDAOIntegracionTest`, `SesionWebDAOIntegracionTest` | `/health?db=1` responde correcto | Sí |
| 9 | Avisos por correo de perfiles nuevos | `CorreoNotificacionesTest` | Crear personal con un correo personal y recibir el aviso sin contraseña | No |
| 10 | Interfaz: temas, visión, estaciones, movimiento | `TemaWebTest`, `ModoVisionTest`, `VisionWebTest`, `EstacionWebTest`, `EstacionAppTest`, `MovimientoWebTest`, `AccesibilidadTest`, `IconosWebTest`, `ZonaVisualTest` | Cambiar tema y visión en web y en el ejecutable | No |
| 11 | «Acerca de» | `AcercaWebTest` | Abrir `/acerca` y el ❓ del escritorio | No |
| 12 | Listados, límites y salud | `FiltrosTest`, `PaginadorTest`, `LimitesCamposTest`, `CacheSaludTest`, `RegistroLogTest` | Filtrar y paginar una lista larga | No |

## Casos manuales mínimos antes de cada despliegue
1. Entrar como administrador y crear un usuario de prueba (y borrarlo).
2. Entrar como profesor, registrar una calificación de prueba.
3. Entrar como estudiante, ver su boleta.
4. Abrir `/health`.
5. Entrar al Aula desde SIGE.

## Comando
```powershell
mvn clean test "-Djacoco.skip=true"
```
Anotar: pruebas ejecutadas, fallos, errores y omitidas (las de integración se omiten sin base de datos de prueba).
