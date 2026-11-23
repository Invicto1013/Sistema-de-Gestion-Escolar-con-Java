# Levanta la MySQL de prueba, corre las pruebas de integracion y la apaga. Uso (desde la raiz del repo):
#   powershell -ExecutionPolicy Bypass -File deploy\db-test\ejecutar-integracion.ps1
$ErrorActionPreference = 'Stop'
$compose = 'deploy/db-test/docker-compose.yml'
docker compose -f $compose up -d --wait
try {
    mvn -P integration "-Djacoco.skip=true" test
    $codigo = $LASTEXITCODE
} finally {
    docker compose -f $compose down -v
}
exit $codigo
