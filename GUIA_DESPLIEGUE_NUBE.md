# Desplegar SIGE Web en otros dispositivos

Esta guía prepara SIGE para alojarlo en un servicio compatible con Docker. La aplicación se publica por HTTPS y los teléfonos, tabletas y computadoras acceden desde un navegador; Android y iPhone pueden instalarla como PWA desde el navegador.

## Antes de desplegar

1. Haz una copia de seguridad de la base de datos actual con `mysqldump` y guárdala fuera del repositorio.
2. Prepara una instancia MySQL accesible **solo de forma privada** desde el servicio de SIGE. No publiques el puerto 3306.
3. Restaura la copia de `sige_db` en ese servidor. Este proyecto no contiene un volcado completo de los datos y el contenedor no crea ni migra automáticamente la base.
4. Conserva la clave AES ya usada por SIGE en `2fa.encryption-key`. Si se cambia, los secretos 2FA existentes no podrán descifrarse. No la subas a GitHub ni la incluyas en Docker.

## Variables privadas del servicio

Configura estas variables en el panel del proveedor, como secretos protegidos:

| Variable | Valor |
| --- | --- |
| `SIGE_WEB_HOST` | `0.0.0.0` |
| `SIGE_WEB_SECURE_COOKIE` | `true` |
| `SIGE_DB_URL` | URL JDBC privada de la instancia MySQL restaurada, con TLS habilitado |
| `SIGE_DB_USER` | Usuario de base de datos con permisos mínimos necesarios |
| `SIGE_DB_PASSWORD` | Contraseña de ese usuario |
| `SIGE_2FA_ENCRYPTION_KEY` | La clave Base64 existente de 32 bytes, exactamente igual a la original |

No configures `PORT` manualmente si el proveedor lo asigna. SIGE ahora utiliza ese valor automáticamente. `SIGE_WEB_PORT` puede conservarse para una instalación propia, pero no debe sobrescribir el puerto asignado por la plataforma.

## Ejemplo de despliegue en Render

1. Guarda los cambios en GitHub desde tu equipo y confirma que el repositorio no contenga `db.properties`, copias SQL con datos reales ni claves.
2. En Render crea un **Web Service** desde el repositorio y elige el entorno **Docker**. Si el repositorio tiene SIGE dentro de una carpeta, indica esa carpeta como raíz del servicio.
3. Añade las variables anteriores en **Environment**. La conexión MySQL debe usar el nombre privado de host que indique tu proveedor.
4. Espera a que finalice la construcción y abre el dominio HTTPS asignado. Comprueba el inicio de sesión, la verificación 2FA y las funciones de cada rol.
5. En el móvil, abre ese dominio en Chrome o Safari y elige **Instalar aplicación** o **Añadir a pantalla de inicio**.

Un servicio gratuito puede suspenderse cuando no se usa y tardar en reactivarse. Para disponibilidad continua se necesita un plan que mantenga el servicio activo. El costo y los límites dependen del proveedor; revisa el panel antes de crear recursos.

## Probar Docker localmente (opcional)

Desde la carpeta que contiene este `Dockerfile`:

```powershell
docker build -t sige-web .
docker run --rm -p 8080:8080 `
  -e PORT=8080 `
  -e SIGE_WEB_HOST=0.0.0.0 `
  -e SIGE_DB_URL="jdbc:mysql://host.docker.internal:3306/sige_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
  -e SIGE_DB_USER="TU_USUARIO" `
  -e SIGE_DB_PASSWORD="TU_CONTRASENA" `
  -e SIGE_2FA_ENCRYPTION_KEY="TU_CLAVE_BASE64_EXISTENTE" `
  sige-web
```

Ese ejemplo de URL es solo para MySQL local de desarrollo. Para producción usa TLS y la URL privada recomendada por el proveedor. No pegues contraseñas reales en capturas, chats ni comandos que vayas a guardar en el historial de PowerShell.

## Instalación y límites

La instalación PWA crea un acceso directo; SIGE y MySQL siguen ejecutándose en el servidor. El modo sin conexión no permite consultar datos académicos. Para desplegar actualizaciones, se actualiza el repositorio y se vuelve a construir el servicio. Esta guía no crea servicios, cuentas ni recursos de pago automáticamente.
