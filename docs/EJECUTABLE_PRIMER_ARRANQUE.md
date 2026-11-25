# Ejecutable sin credenciales: asistente de primer arranque

**Problema:** `empaquetar.bat` copiaba `config.properties` (con la contraseña de la base) dentro de la entrega, y el programa no abría en otro equipo sin ese archivo. Quien tuviera el `.exe` podía extraer la clave.

**Solución:** el ejecutable ya no lleva credenciales. Cada equipo guarda las suyas fuera del programa.

## Cómo funciona
1. Al abrir SIGE, si no hay configuración (variables de entorno o `config.properties`), aparece el asistente «Configuración inicial».
2. Se escriben dirección (`jdbc:mysql://…`), usuario, contraseña y, opcionalmente, la clave 2FA (AES-256 en Base64), o se importa un `config.properties`.
3. El asistente valida los datos (sin mostrar valores), prueba la conexión y, si funciona, los guarda en `%APPDATA%\SIGE\config.properties` (en Linux/macOS, `~/.config/sige/`). Si falla, explica el problema y deja corregir.
4. Las siguientes veces abre directo en el login.

## Orden de búsqueda (`ConexionBD`)
1. Variables de entorno `SIGE_DB_URL`, `SIGE_DB_USER`, `SIGE_DB_PASSWORD`.
2. `config.properties` en la carpeta de ejecución, junto al `.jar`, o en la carpeta del usuario.
3. Classpath (solo desarrollo).

## Seguridad
- Las claves se escriben en el asistente y no se muestran ni se registran; los mensajes de error no incluyen valores.
- Escritura atómica; en Linux/macOS el archivo queda `rw-------`. En Windows `%APPDATA%` ya es privada del usuario.
- `empaquetar.bat` ya no copia ni exige `config.properties`.
- El archivo del usuario sigue sin subirse a Git. No uses la contraseña de administrador de la base: crea un usuario con los permisos necesarios.

## Soporte
- Cambiar de base o de contraseña: borrar `%APPDATA%\SIGE\config.properties` y abrir SIGE de nuevo.
- Despliegues especiales: la propiedad de sistema `sige.config.dir` reemplaza la carpeta del usuario.
