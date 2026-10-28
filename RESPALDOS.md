# Respaldos de la base de datos de SIGE

Nunca dependas de una sola copia. Este proyecto hace una copia automatica diaria y tambien
puedes hacer una manual cuando quieras.

## 1. Respaldo automatico (GitHub Actions)

El workflow `.github/workflows/respaldo-bd.yml` corre cada dia (07:17 UTC) y tambien a mano.
Hace un `mysqldump` de la base, lo comprime, lo **cifra con AES-256** y lo guarda 30 dias como
artefacto del workflow. Esta cifrado porque contiene datos personales y hashes de contrasenas.

### Configuracion (una sola vez)

En GitHub: **Settings > Secrets and variables > Actions > New repository secret**. Crea estos 6:

| Secreto | Valor |
|---|---|
| `SIGE_DB_HOST` | Host de Aiven (solo el host, sin `jdbc:` ni puerto) |
| `SIGE_DB_PORT` | Puerto de Aiven |
| `SIGE_DB_USER` | Usuario (por ejemplo `avnadmin`) |
| `SIGE_DB_PASSWORD` | Contrasena actual de la base |
| `SIGE_DB_NAME` | Nombre de la base (por ejemplo `sige_db`) |
| `SIGE_BACKUP_PASSPHRASE` | Una frase larga que inventes: es la clave para abrir las copias |

**Guarda `SIGE_BACKUP_PASSPHRASE` tambien fuera de GitHub** (gestor de contrasenas o papel).
Sin ella las copias no se pueden abrir, y GitHub no puede mostrartela otra vez.

Prueba: pestana **Actions > Respaldo de la base de datos > Run workflow**. Debe terminar en verde
y dejar un artefacto `sige-respaldo-AAAA-MM-DD_HHMM`.

Si cambias la contrasena de la base, actualiza `SIGE_DB_PASSWORD`. Si el workflow falla, GitHub te
avisa por correo.

## 2. Respaldo manual (MySQL Workbench)

1. Conectate a la base (Aiven o local).
2. **Server > Data Export**.
3. Marca la base `sige_db`, elige **Export to Self-Contained File**, marca **Include Create Schema**
   solo si es una base local, y pulsa **Start Export**.
4. Guarda el `.sql` en un lugar seguro (no en el repositorio). Contiene datos personales.

## 3. Restaurar una copia automatica

1. Descarga el artefacto desde la pestana Actions (archivo `.gpg`, viene dentro de un `.zip`).
2. Descifra y descomprime (en PowerShell con Gpg4win instalado, o en Linux/macOS):
   ```
   gpg --output respaldo.sql.gz --decrypt sige_AAAA-MM-DD_HHMM.sql.gz.gpg
   ```
   Te pedira la frase `SIGE_BACKUP_PASSPHRASE`. Luego descomprime `respaldo.sql.gz` (7-Zip en
   Windows, o `gunzip respaldo.sql.gz`).
3. **Prueba primero en una base vacia local**, nunca directamente sobre la de produccion:
   ```
   mysql -u root -p --default-character-set=utf8mb4 -e "CREATE DATABASE sige_restauracion CHARACTER SET utf8mb4"
   mysql -u root -p --default-character-set=utf8mb4 sige_restauracion < respaldo.sql
   ```
   (en cmd, no en PowerShell, porque PowerShell no acepta el operador `<`).
4. Comprueba que hay datos: `SELECT COUNT(*) FROM sige_restauracion.usuarios;`
5. Para restaurar en produccion, importa el mismo archivo en la base de Aiven (idealmente tras
   hacer una copia manual del estado actual).

## 4. Buenas practicas

- Haz una restauracion de prueba al menos una vez: una copia que nunca se probo no es una copia.
- Antes de cambios grandes (nuevos scripts SQL, cambios de esquema) lanza el workflow a mano.
- No subas archivos `.sql` con datos reales al repositorio.
