#!/usr/bin/env bash
# Falla (exit 1) si la cobertura de LINEAS de un paquete queda por debajo del umbral.
# Uso: verificar-umbral.sh [paquete] [umbral%] [csv]
#   paquete  por defecto com.unev.sige.model
#   umbral   por defecto $UMBRAL_MODELO o 60
set -euo pipefail
paquete="${1:-com.unev.sige.model}"
umbral="${2:-${UMBRAL_MODELO:-60}}"
csv="${3:-target/site/jacoco/jacoco.csv}"
if [ ! -f "$csv" ]; then echo "No se encontro $csv" >&2; exit 1; fi
awk -F, -v pkg="$paquete" -v min="$umbral" '
NR>1 && $2==pkg { lm+=$8; lc+=$9; n++ }
END {
  if (n==0) { printf "El paquete %s no aparece en el informe.\n", pkg > "/dev/stderr"; exit 2 }
  total = lm+lc
  pct = total==0 ? 100 : 100*lc/total
  printf "Cobertura de lineas de %s: %.1f %% (%d/%d). Umbral: %s %%\n", pkg, pct, lc, total, min
  if (pct+0 < min+0) { print "UMBRAL NO ALCANZADO" > "/dev/stderr"; exit 1 }
  print "Umbral alcanzado."
}' "$csv"
