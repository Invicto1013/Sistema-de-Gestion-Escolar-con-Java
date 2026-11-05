#!/usr/bin/env python3
"""Calcula el porcentaje de avance del proyecto a partir de docs/progreso.json y escribe docs/PROGRESO.md.

Uso:  python docs/progreso.py
Regla: porcentaje = suma(peso x avance) / suma(peso). Cada cambio nuevo se agrega como un elemento con su peso;
terminarlo o verificarlo sube su avance. Solo biblioteca estandar de Python 3.
"""
import json
import pathlib
import sys

AQUI = pathlib.Path(__file__).resolve().parent


def calcular(items):
    total = sum(i["peso"] for i in items)
    hecho = sum(i["peso"] * i["avance"] / 100 for i in items)
    return hecho, total, (100 * hecho / total if total else 0.0)


def barra(porcentaje, ancho=30):
    llenos = round(ancho * porcentaje / 100)
    return "█" * llenos + "░" * (ancho - llenos)


def generar(datos):
    items = datos["items"]
    hecho, total, pct = calcular(items)
    lineas = [
        "# Avance del proyecto SIGE",
        "",
        f"**{pct:.1f} %** `{barra(pct)}`",
        "",
        f"Actualizado el {datos['actualizado']}. Meta: {datos['meta']}.",
        "",
        "El porcentaje es la suma de peso × avance de cada elemento, dividida entre la suma de pesos "
        f"({hecho:.1f} de {total} puntos). Un cambio nuevo se agrega con su peso y sube el porcentaje al terminarse "
        "y al verificarse en producción. Se recalcula con `python docs/progreso.py` desde `docs/progreso.json`.",
        "",
        "## Resumen por área",
        "",
        "| Área | Puntos logrados | Puntos totales | Avance |",
        "| --- | ---: | ---: | ---: |",
    ]
    areas = []
    for i in items:
        if i["area"] not in areas:
            areas.append(i["area"])
    for a in areas:
        sub = [i for i in items if i["area"] == a]
        h, t, p = calcular(sub)
        lineas.append(f"| {a} | {h:.1f} | {t} | {p:.0f} % |")
    lineas += ["", "## Detalle", "", "| Área | Elemento | Peso | Avance |", "| --- | --- | ---: | ---: |"]
    for i in items:
        lineas.append(f"| {i['area']} | {i['nombre']} | {i['peso']} | {i['avance']} % |")
    lineas += ["", "## Historial de avance", "", "| Fecha | Cambio | Avance total |", "| --- | --- | ---: |"]
    for h in datos["historial"]:
        lineas.append(f"| {h['fecha']} | {h['cambio']} | {h['porcentaje']:.1f} % |")
    return "\n".join(lineas) + "\n", pct


def main():
    datos = json.loads((AQUI / "progreso.json").read_text(encoding="utf-8"))
    texto, pct = generar(datos)
    ultimo = datos["historial"][-1]["porcentaje"] if datos["historial"] else None
    if ultimo is not None and abs(ultimo - pct) > 0.05:
        print(f"AVISO: el último punto del historial ({ultimo:.1f} %) no coincide con el cálculo actual ({pct:.1f} %). "
              "Agrega una fila al historial.", file=sys.stderr)
    (AQUI / "PROGRESO.md").write_text(texto, encoding="utf-8")
    print(f"Avance: {pct:.1f} %")


if __name__ == "__main__":
    main()
