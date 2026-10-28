#!/usr/bin/env bash
# Convierte target/site/jacoco/jacoco.csv en una tabla Markdown (resumen del job en GitHub Actions).
set -euo pipefail
csv="${1:-target/site/jacoco/jacoco.csv}"
if [ ! -f "$csv" ]; then echo "No se encontro $csv" >&2; exit 1; fi
awk -F, 'NR>1 {
  im+=$4; ic+=$5; bm+=$6; bc+=$7; lm+=$8; lc+=$9; mm+=$12; mc+=$13
}
function pct(c,m) { return (c+m)==0 ? "n/a" : sprintf("%.1f %%", 100*c/(c+m)) }
END {
  print "## Cobertura de pruebas (JaCoCo)"
  print ""
  print "| Metrica | Cubierto | Total | Porcentaje |"
  print "|---|---:|---:|---:|"
  printf "| Lineas | %d | %d | %s |\n", lc, lc+lm, pct(lc,lm)
  printf "| Ramas | %d | %d | %s |\n", bc, bc+bm, pct(bc,bm)
  printf "| Metodos | %d | %d | %s |\n", mc, mc+mm, pct(mc,mm)
  printf "| Instrucciones | %d | %d | %s |\n", ic, ic+im, pct(ic,im)
}' "$csv"
