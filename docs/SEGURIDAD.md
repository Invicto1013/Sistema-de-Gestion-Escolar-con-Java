# Revisión de seguridad de SIGE y del Aula virtual

Revisión del código y de las pruebas automáticas, hecha el 4 de noviembre de 2026 sobre los 20 puntos más comunes de fallos en aplicaciones web. "Cumple" significa que hay una medida concreta en el código; las pruebas pendientes de ejecutar en producción se marcan como acción.

| # | Punto | Estado | Evidencia |
|---|---|---|---|
| 1 | `.env` o credenciales en GitHub | Cumple | `config.properties` está en `.gitignore`; las claves van en variables de entorno de Render. Ya no se publican contraseñas de demostración en el script SQL |
| 2 | API keys en el frontend | Cumple | El navegador no recibe claves: la base de datos y el secreto SSO solo existen en el servidor |
| 3 | RLS desactivado | No aplica | MySQL sin RLS; el Aula usa el usuario `aula_app` con permisos mínimos (comprobado con errores 1142) |
| 4 | Permisos solo en el frontend | Cumple | Cada DAO valida el rol en el servidor (`Autorizacion`); el padre solo ve a sus hijos por la unión `estudiante_padre` |
| 5 | Endpoints sin rate limiting | Cumple | Cuenta bloqueada 15 minutos tras 5 contraseñas incorrectas, límite de 20 fallos por IP cada 10 minutos, 300 POST por minuto por IP y segundo factor bloqueado 15 minutos tras varios intentos |
| 6 | SQL concatenando strings | Cumple | Todo con `PreparedStatement`; 40 sentencias nuevas de la Fase D se analizaron con un parser de MySQL |
| 7 | Cero validación de inputs | Cumple | `LimitesCampos` en la web, reglas de negocio en `ReglasPeriodo`, validación de motivo, nombre y fechas |
| 8 | HTML del usuario sin filtrar | Cumple | Escape de HTML en toda salida (`esc`) y CSP `default-src 'self'` |
| 9 | Contraseñas en texto plano | Cumple | BCrypt |
| 10 | Tokens en localStorage | Cumple | Sesión en cookie `HttpOnly; SameSite=Lax` (Lax permite que al volver desde el Aula la sesión siga abierta; los POST llevan token CSRF); `Secure` con `SIGE_WEB_SECURE_COOKIE=true` |
| 11 | Panel de admin sin login | Cumple | Todas las rutas de administración comprueban `Rol.ADMIN` en el servidor; los intentos denegados se auditan |
| 12 | CORS abierto | Cumple | No se emiten cabeceras CORS; `Cross-Origin-Resource-Policy: same-origin` |
| 13 | Email sin verificar | Parcial | El registro pasa por aprobación del administrador (`pendiente_aprobacion`), pero no se envía un enlace de verificación al correo |
| 14 | IDs predecibles sin control | Cumple | Cada consulta con un ID comprueba pertenencia (profesor-sección, padre-hijo, estudiante-materia). Falta ejecutarlo como prueba manual (S1 y S2 abajo) |
| 15 | Body entero en cada update | Cumple | Los formularios se leen campo por campo; no se mapea el cuerpo completo a entidades |
| 16 | Webhooks sin firma | No aplica | No hay webhooks. El acceso único al Aula usa un token firmado con HMAC-SHA256, de un solo uso y caduca en 180 s |
| 17 | Stacktraces en producción | Cumple | El usuario solo ve mensajes controlados; los detalles técnicos van al log del servidor |
| 18 | Dependencias sin actualizar | Cumple | Dependabot semanal en ambos repositorios y análisis CodeQL semanal |
| 19 | Claves débiles o filtradas | Acción | Rotar las contraseñas de las cuentas de demostración (ver abajo) y usar un `AULA_SSO_SECRET` de 48 bytes |
| 20 | Archivos subidos sin validar | Cumple | Lista de extensiones permitidas y comprobación de la firma real del archivo, tanto en SIGE como en el Aula; descarga siempre como adjunto con `nosniff`; límite de 10 MB |

## Cabeceras de seguridad activas
`Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`, `Cross-Origin-Opener-Policy`, `Cross-Origin-Resource-Policy`, `Cache-Control: no-store` y `Strict-Transport-Security` cuando la conexión es HTTPS.

## Acción pendiente: cuentas de demostración
Las cuentas `admin@sige.edu.do`, `jrodriguez@sige.edu.do`, `peguero@sige.edu.do` y `mariana@sige.edu.do` se crearon con contraseñas que estuvieron escritas en el repositorio público. Aunque se quiten del archivo, siguen en el historial de Git, así que hay que cambiarlas:

```sql
SELECT id_usuario, correo, rol, activo FROM usuarios
WHERE correo IN ('admin@sige.edu.do','jrodriguez@sige.edu.do','peguero@sige.edu.do','mariana@sige.edu.do');
```

Para cada cuenta que siga existiendo: entrar como administrador, *Usuarios → Restablecer contraseña* (o desactivar la cuenta si ya no se usa) y entregar la nueva contraseña solo a quien evalúe el proyecto.

## Pruebas de seguridad para ejecutar en producción
| # | Prueba | Resultado esperado |
|---|---|---|
| S1 | Entrar como profesor y cambiar en la URL `calificaciones?seccion=` por la sección de otro profesor | Se rechaza o no muestra datos |
| S2 | Entrar como padre y cambiar el estudiante en `/boleta` por uno no vinculado | Se rechaza o muestra solo los hijos propios |
| S3 | Sin sesión, abrir `/auditoria`, `/periodos` y `/auditoria.csv` | Redirige al inicio de sesión o muestra "solo administrador" |
| S4 | Como profesor, abrir `/auditoria` | Mensaje de solo administrador |
| S5 | Fallar la contraseña 5 veces seguidas con una cuenta de prueba | La cuenta se bloquea 15 minutos |
| S6 | En el Aula, subir `virus.exe` y un `falso.pdf` que no sea PDF | Mensaje "Tipo de archivo no permitido" |
| S7 | Abrir dos veces el mismo enlace `/sso` del Aula | La segunda vez se rechaza |
| S8 | Enviar un formulario sin `_csrf` (o con uno viejo) | "La solicitud expiró" |
| S9 | Escribir `<script>alert(1)</script>` como título de una publicación | Se muestra como texto, no se ejecuta |
