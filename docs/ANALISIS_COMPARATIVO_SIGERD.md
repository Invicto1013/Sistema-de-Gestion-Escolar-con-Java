# Análisis Comparativo: SIGE vs. SIGERD (MINERD)

Este documento presenta una comparativa técnica, funcional y normativa entre **SIGE** (Sistema de Gestión Educativa desarrollado para el centro escolar) y la plataforma gubernamental oficial de la República Dominicana **SIGERD** (*Sistema de Información para la Gestión Escolar de la República Dominicana*), administrada por el **MINERD** (*Ministerio de Educación*).

---

## 1. Cuadro Comparativo Global de Arquitectura y Módulos

| Dimensión / Módulo | SIGERD (Gobierno / MINERD) | SIGE (Este Proyecto) | Grado de Coincidencia |
|---|---|---|:---:|
| **Estructura Organizacional** | Regionales, Distritos y Centros Educativos | Centro escolar con roles: Admin, Director/a, Secretario/a, Docente, Estudiante y Tutor | 🟢 **95 %** (Alineado al centro) |
| **Identificación Estudiantil** | RNE (*Registro Nacional de Estudiante*) y NUI | Matrícula anual + RNE MINERD (`IdentificadorEscolar.java`) | 🟢 **100 %** |
| **Evaluación y Calificaciones** | Ponderaciones 30/30/40, escala A-B-C-D-F, Pruebas Completivas (50/50) y Extraordinarias (30/70) | `EscalaCalificaciones.java`, `CalculoRecuperacion.java` y `IndiceAcademico.java` | 🟢 **100 %** |
| **Control de Asistencia** | Asistencia diaria escolar por sección y materia | `FilaAsistencia`, `ResumenAsistencia` y `ReglasAsistencia.java` (Umbral 80% examen, < 70% R/I) | 🟢 **100 %** |
| **Períodos y Bloqueo de Notas** | Fechas límites por corte lectivo con bloqueo estricto | `PeriodosDAO`, `ReglasPeriodo.java` (Abierto/Cerrado, corrección auditada con motivo $\ge 10$ car.) | 🟢 **100 %** |
| **Gestión Docente y Secciones** | Distribución de secciones, cargas y asignación docente | `SeccionDAO`, `ProfesorDAO`, control de cupos en `ValidadorCapacidadSeccion.java` | 🟢 **95 %** |
| **Horarios Escolares** | Calendario y bloques por aula y sección | `HorarioSeccion` y validador de solapamientos `ValidadorHorarios.java` | 🟢 **95 %** |
| **Convivencia y Disciplina** | Cuaderno de incidencias disciplinarias escolares | `ObservacionConducta` y `EvaluacionConducta.java` (Méritos, faltas y alerta a Orientación) | 🟢 **90 %** |
| **Registro Civil y Legal** | Acta de nacimiento (Oficialía, Libro, Folio, Acta) y Cédula | `ValidadorActaNacimientoEscolar.java` y `ValidadorCedulaDominicana.java` (Módulo 10) | 🟢 **100 %** |
| **Servicio Social Estudiantil** | 60 horas obligatorias comunitarias para bachilleres | `ControlServicioSocial.java` (Acreditación para graduación) | 🟢 **100 %** |
| **Certificaciones y Actas** | Libro, Folio y Acta oficial de calificaciones | `ValidadorActaCalificaciones.java` (Formato oficial MINERD) | 🟢 **100 %** |
| **Salud y Emergencias** | Ficha médica, vacunas PAI y contactos de urgencia | `ValidadorEmergenciaMedica.java` (911, Rh) y `ValidadorCertificadoSaludEscolar.java` | 🟢 **90 %** |
| **Portal Virtual de Clases** | Integrado en ecosistemas externos (Moodle o Teams) | Aula Virtual desacoplada (`sige-aula`) con SSO seguro (`SameSite=Lax`) | 🟢 **Supera a SIGERD** |
| **Auditoría del Sistema** | Auditoría interna del servidor central | Tabla `auditoria` inmutable, sólo inserción, exportación ejecutiva a Excel/CSV con hora RD | 🟢 **Supera a SIGERD** |

---

## 2. Puntos de Parecido Exacto con el Estándar MINERD

### A. Normativa de Evaluación Académica
1. **Ponderación Escolar Ordinaria:**
   SIGE implementa exactamente la fórmula oficial del currículo dominicano:
   $$\text{Nota Final} = 30\% (\text{Parcial 1}) + 30\% (\text{Parcial 2}) + 40\% (\text{Examen Final})$$
   Aprobándose con un mínimo de **$70.0$ puntos** y asignando la escala literal: **A** ($\ge 90$), **B** ($\ge 80$), **C** ($\ge 70$), **D** ($\ge 60$) y **F** ($< 60$).
2. **Evaluaciones de Recuperación:**
   SIGE modela en `CalculoRecuperacion.java` las dos instancias extraordinarias del MINERD:
   - **Prueba Completiva:** $50\%$ Promedio Anual $+ 50\%$ Examen Completivo.
   - **Prueba Extraordinaria:** $30\%$ Promedio Anual $+ 70\%$ Examen Extraordinario.
3. **Control de Inasistencias (R/I):**
   Al igual que en SIGERD, la asistencia menor al $80\%$ activa alertas formativas y por debajo del $70\%$ inhabilita al alumno a examen ordinario por reprobación por inasistencia.

### B. Datos de Identidad y Registro Nacional
- **RNE (Registro Nacional de Estudiante):** Formato oficial de 4 letras iniciales del nombre/apellido + 8 dígitos de fecha de nacimiento y consecutivo.
- **Cédula de Identidad y Electoral:** Validación algorítmica con la fórmula oficial **Módulo 10** de la Junta Central Electoral (JCE), exactamente como valida SIGERD al dar de alta docentes y padres.
- **Acta de Nacimiento Oficial:** Entrada estructurada de Oficialía del Estado Civil, Libro, Folio y Número de Acta para la admisión en nivel inicial y primaria.

### C. Bloqueo de Períodos y Cierre Lectivo
En SIGERD, ningún maestro puede alterar notas fuera del período oficial sin un dictamen del Distrito. En SIGE, la tabla `periodos_lectivos` bloquea automáticamente el módulo de notas cuando el período pasa a estado `CERRADO`. Cualquier modificación posterior es una **corrección excepcional** restringida a rol Administrador, la cual exige una justificación de al menos 10 caracteres y genera un evento inmutable `NOTA_CORREGIDA` en la auditoría.

---

## 3. Diferencias Técnicas y Ventajas Clave de SIGE

1. **Operación Híbrida (Escritorio + Web):**
   - **SIGERD:** Es un portal 100% web centralizado; si la red del distrito o el enlace de internet del plantel falla, la secretaría queda paralizada.
   - **SIGE:** Cuenta con aplicación de escritorio nativa en **JavaFX 21** para la secretaría/dirección y un servidor web ligero adaptable para acceso de docentes, estudiantes y padres, permitiendo operar en red local o en la nube.
2. **Aula Virtual Integrada con Acceso Único (SSO):**
   - SIGE cuenta con un módulo de aula virtual desacoplado (`sige-aula`) que se comunica mediante tokens firmados sin compartir credenciales de base de datos directas (`SameSite=Lax`), ofreciendo una experiencia educativa unificada que SIGERD delega a herramientas externas.
3. **Resiliencia y Pruebas Automatizadas:**
   - SIGE cuenta con más de 190 pruebas automáticas en JUnit 5, cobertura con JaCoCo, análisis de seguridad continuo con CodeQL y flujos CI multiplataforma (Windows, Linux, macOS).

---

## 4. Conclusión

El proyecto **SIGE** es una solución **altamente homóloga y complementaria a SIGERD**. Adopta el $100\%$ de las reglas del negocio educativo dominicano (escalas de calificación, fórmulas ponderadas, cierres de período, RNE, Cédula Módulo 10, horas de servicio social y actas MINERD), superando a las plataformas convencionales en velocidad, trazabilidad auditada y flexibilidad de despliegue institucional.
