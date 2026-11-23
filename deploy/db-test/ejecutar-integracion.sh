#!/usr/bin/env bash
# Igual que el .ps1 para Linux/macOS: levanta la MySQL de prueba, corre las pruebas y la apaga.
set -uo pipefail
compose=deploy/db-test/docker-compose.yml
docker compose -f "$compose" up -d --wait || exit 1
mvn -P integration -Djacoco.skip=true test
codigo=$?
docker compose -f "$compose" down -v
exit $codigo
