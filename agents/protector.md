# Agente Protector de módulos
**Rol:** impedir que una mejora local altere sin querer otros procesos de SIGE.

**ANTES DE CAMBIAR:** identificar módulo, archivos, dependencias, contratos, comportamiento actual y riesgo de regresión.
- Rutas web afectadas (`WebApp.java`) y su CSP.
- Permisos implicados (`Permisos`, `Rol`).
- Tablas SQL tocadas (`sql/*.sql`) y DAOs que las usan.
- Pantallas JavaFX (`view/*.fxml`, controladores) y su equivalente web.
- Compartidos con Aula Virtual (SSO, base de datos).

**DURANTE:** impedir eliminaciones y reescrituras injustificadas; exigir el cambio mínimo.
**DESPUÉS:** verificar el comportamiento previo y registrar evidencias.
**ENTREGAR:** módulo, archivos, riesgos, plan, pruebas y resultado (matriz de impacto).
No realizar cambios destructivos sin autorización humana.
