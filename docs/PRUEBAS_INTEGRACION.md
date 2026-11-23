# Pruebas de integración aisladas

Las pruebas `*IntegracionTest` solo corren con `SIGE_IT_DB=true` y una base **local** (`localhost`/`127.0.0.1`). Nunca tocan TiDB, Aiven ni producción.

## Ejecutarlas (requiere Docker)
```powershell
powershell -ExecutionPolicy Bypass -File deploy\db-test\ejecutar-integracion.ps1
```
Levanta `mysql:8.4` en `127.0.0.1:3307` con `sql/sige_completo.sql`, ejecuta `mvn -P integration test` y destruye la base. En Linux/macOS: `deploy/db-test/ejecutar-integracion.sh`.

Si ya tienes una MySQL local: `mvn -P integration "-Dit.db.url=jdbc:mysql://127.0.0.1:3306/sige_test?useSSL=false&allowPublicKeyRetrieval=true" "-Dit.db.password=TU_CLAVE" test`.

`mvn test` normal sigue omitiendo esas pruebas (15 *Skipped*).

## Cobertura mínima en CI
`cobertura.yml` ejecuta `.github/scripts/verificar-umbral.sh`: falla si la cobertura de líneas de `com.unev.sige.model` baja del umbral (`UMBRAL_MODELO`, inicial 60). Ajústalo tras ver el primer resultado real.

## Contratos HTTP
`SeguridadWebTest` verifica cabeceras (CSP, X-Frame-Options, nosniff, HSTS con HTTPS) y la cookie (`HttpOnly`, `SameSite=Lax`). `ContratosHttpTest` lo comprueba contra el servidor real en otra JVM y se omite si no arranca.
