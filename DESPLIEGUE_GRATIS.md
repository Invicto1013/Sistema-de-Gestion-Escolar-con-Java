# Publicar SIGE Web gratis (Render + Aiven MySQL)

Datos verificados en octubre de 2026. Los planes gratis cambian seguido; confirma en cada panel antes de crear recursos.

## Qué necesita SIGE
| Requisito | Detalle |
|---|---|
| Servidor | Java 21, contenedor Docker, escucha en el `PORT` que da la plataforma |
| Memoria | ~300 MB con el nuevo `Dockerfile` (cabe en 512 MB) |
| Base de datos | MySQL con la base `sige_db` |
| HTTPS | Obligatorio (cookie `Secure` y PWA) |
| Secretos | `SIGE_DB_URL`, `SIGE_DB_USER`, `SIGE_DB_PASSWORD`, `SIGE_2FA_ENCRYPTION_KEY` |

## Combinación recomendada (sin tarjeta de crédito)
- **App: Render, Web Service gratis.** Docker, HTTPS automático, 512 MB. Se duerme tras 15 min sin visitas y tarda ~1 min en despertar.
- **Base de datos: Aiven for MySQL, plan gratis.** MySQL real, 1 CPU, 1 GB RAM, 1 GB disco, sin límite de tiempo. Si no tiene uso, Aiven puede apagarla (avisa antes; se enciende de nuevo desde su panel).

Render no ofrece MySQL gratis (su Postgres gratis caduca a los 30 días), por eso la base va en Aiven.

Alternativas: TiDB Cloud Starter (compatible con MySQL, 5 GB, sin tarjeta; riesgo menor de diferencias con MySQL), Railway (MySQL con crédito de $1/mes), Oracle Cloud Always Free (siempre encendido y app + MySQL en una sola VM, pero pide tarjeta y configuración manual).

## Pasos

### 1. Exportar tu base local
Tu `sige_db` local tiene el esquema y los datos; el zip no trae el esquema completo (`base de datos.sql` solo tiene un SELECT). En tu PC:
```powershell
mysqldump -u root -p --databases sige_db --routines --triggers > sige_backup.sql
```
No subas ese archivo a GitHub.

### 2. Crear MySQL en Aiven
1. Cuenta en aiven.io → **Create service → MySQL → plan Free**.
2. Espera a que esté *Running* y copia Host, Port, User y Password.
3. Crea la base `sige_db` (pestaña *Databases*).
4. Importa el respaldo desde tu PC:
   ```powershell
   mysql -h HOST -P PUERTO -u avnadmin -p --ssl-mode=REQUIRED sige_db < sige_backup.sql
   ```
   (Si el dump incluye `CREATE DATABASE`/`USE`, quítalo o ajústalo.)

### 3. Subir el código a GitHub
Sube la carpeta `sige` (la que tiene `Dockerfile` y `pom.xml`). `db.properties` ya está en `.gitignore`. Comprueba con `git ls-files | findstr db.properties`: solo debe salir `db.properties.example`.

### 4. Crear el Web Service en Render
1. render.com → **New → Web Service** → tu repositorio → Language: **Docker** → Instance: **Free**.
2. En **Environment** añade:

| Variable | Valor |
|---|---|
| `SIGE_DB_URL` | `jdbc:mysql://HOST:PUERTO/sige_db?sslMode=REQUIRED&serverTimezone=America/Santo_Domingo` |
| `SIGE_DB_USER` | `avnadmin` |
| `SIGE_DB_PASSWORD` | la clave de Aiven |
| `SIGE_2FA_ENCRYPTION_KEY` | el valor de `2fa.encryption-key` de tu `db.properties` local, idéntico |
| `SIGE_WEB_HOST` | `0.0.0.0` |
| `SIGE_WEB_SECURE_COOKIE` | `true` |

3. Deploy. Al terminar, abre la URL `https://….onrender.com`.

### 5. Probar
Inicia sesión con un usuario existente, verifica 2FA y revisa un módulo por rol. En el celular: *Instalar aplicación* / *Añadir a pantalla de inicio*.

## Limitaciones del plan gratis
- La primera visita tras 15 min de inactividad tarda ~1 min.
- Las sesiones están en memoria: al dormirse el servicio hay que volver a iniciar sesión.
- Aiven free no tiene IP fija ni red privada: la base es accesible por Internet con TLS. Usa una contraseña fuerte.
- Render free: 750 horas/mes y 5 GB de tráfico/mes.

## Cambios que se hicieron al proyecto
- `Dockerfile`: ahora compila en una etapa y ejecuta con `java -cp` en una imagen JRE (antes ejecutaba `mvn javafx:run`, que consume demasiada RAM) y borra `db.properties` de la imagen.
- `.dockerignore`: excluye cualquier `db.properties`, también el de `src/main/resources`, que antes se habría copiado a la imagen con tu clave local.

## Actualizar la web publicada con cambios nuevos

1. **Primero la base, después el código.** El código nuevo usa columnas y tablas nuevas; si se despliega antes de migrar la base, la web da errores. Aplica en Aiven, en este orden y una sola vez cada una (son repetibles sin riesgo):
   ```
   mysql -h HOST -P PUERTO -u avnadmin -p --ssl-mode=REQUIRED sige_db < sql\fase_curriculo_grados.sql
   mysql -h HOST -P PUERTO -u avnadmin -p --ssl-mode=REQUIRED sige_db < sql\aula_virtual.sql
   ```
2. Haz `git commit` y `git push` a `main`. Render reconstruye y publica solo si tiene activado el despliegue automático; si no, usa **Manual Deploy → Deploy latest commit**.
3. **Aula virtual en el plan gratuito:** los archivos subidos se guardan en el disco del contenedor (`SIGE_UPLOAD_DIR`, por defecto `data/uploads`). Render gratis no tiene disco persistente: los archivos se pierden cuando el servicio se duerme, se reinicia o se vuelve a desplegar. Los registros de materiales y entregas sí quedan en MySQL.
4. **Correos (opcional):** si defines `SIGE_SMTP_HOST`, `SIGE_SMTP_PORT`, `SIGE_SMTP_USER`, `SIGE_SMTP_PASSWORD` y `SIGE_SMTP_FROM` (y, si aplica, `SIGE_SMTP_AUTH`, `SIGE_SMTP_SSL`, `SIGE_SMTP_STARTTLS`) en Render, SIGE avisa a los usuarios nuevos por correo. Sin ellas, esa función no se usa.
