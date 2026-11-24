package com.unev.sige.model;

import static com.unev.sige.model.HerramientaEscolar.Campo.ent;
import static com.unev.sige.model.HerramientaEscolar.Campo.fecha;
import static com.unev.sige.model.HerramientaEscolar.Campo.hora;
import static com.unev.sige.model.HerramientaEscolar.Campo.lista;
import static com.unev.sige.model.HerramientaEscolar.Campo.num;
import static com.unev.sige.model.HerramientaEscolar.Campo.op;
import static com.unev.sige.model.HerramientaEscolar.Campo.sn;
import static com.unev.sige.model.HerramientaEscolar.Campo.texto;
import static com.unev.sige.model.HerramientaEscolar.roles;
import static com.unev.sige.model.Rol.DIRECTOR;
import static com.unev.sige.model.Rol.ESTUDIANTE;
import static com.unev.sige.model.Rol.PADRE;
import static com.unev.sige.model.Rol.PROFESOR;
import static com.unev.sige.model.Rol.SECRETARIA;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Catálogo de herramientas escolares visibles según el rol. Cada entrada conecta una regla del paquete
 * {@code model} (calculadora o validador) con un formulario. El administrador ve todas.
 */
public final class CatalogoHerramientas {

    /** Orden fijo en que se muestran los grupos. */
    public static final List<String> GRUPOS = List.of("Académico", "Docentes", "Bienestar y seguridad",
            "Administración", "Familia y comunidad", "Documentos y códigos");

    private static final String ACADEMICO = "Académico";
    private static final String DOCENTES = "Docentes";
    private static final String BIENESTAR = "Bienestar y seguridad";
    private static final String ADMINISTRACION = "Administración";
    private static final String FAMILIA = "Familia y comunidad";
    private static final String DIGITAL = "Documentos y códigos";

    private static final List<HerramientaEscolar> TODAS = construir();

    private CatalogoHerramientas() { }

    public static List<HerramientaEscolar> todas() { return TODAS; }

    public static List<HerramientaEscolar> paraRol(Rol rol) {
        return TODAS.stream().filter(h -> h.visiblePara(rol)).toList();
    }

    /** Campos que se usan para calcular pero NUNCA se guardan en la base de datos (datos personales o secretos). */
    private static final java.util.Map<String, Set<String>> NO_SE_GUARDAN =
            java.util.Map.of("retiro-estudiante", Set.of("cedula", "pin", "pinIngresado"));

    public static boolean noSeGuarda(String herramientaId, String campo) {
        return NO_SE_GUARDAN.getOrDefault(herramientaId, Set.of()).contains(campo);
    }

    /** Las herramientas del rol agrupadas en el orden de {@link #GRUPOS} y, dentro de cada grupo, por título (alfabético en español). */
    public static java.util.Map<String, List<HerramientaEscolar>> porGrupo(Rol rol) {
        java.text.Collator orden = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es"));
        java.util.Map<String, List<HerramientaEscolar>> r = new java.util.LinkedHashMap<>();
        for (String g : GRUPOS) {
            List<HerramientaEscolar> delGrupo = paraRol(rol).stream().filter(h -> h.grupo().equals(g))
                    .sorted((x, y) -> orden.compare(x.titulo(), y.titulo())).toList();
            if (!delGrupo.isEmpty()) r.put(g, delGrupo);
        }
        return r;
    }

    /** Identificador sin tildes ni espacios de un grupo (para anclas y para recordar qué grupo está abierto). */
    public static String slug(String grupo) {
        String s = java.text.Normalizer.normalize(grupo == null ? "" : grupo, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    public static Optional<HerramientaEscolar> buscar(String id) {
        return TODAS.stream().filter(h -> h.id().equals(id)).findFirst();
    }

    private static String si(boolean b) { return b ? "Sí" : "No"; }

    private static String hoy(int dias) { return LocalDate.now().plusDays(dias).toString(); }

    private static HerramientaEscolar herramienta(String id, String grupo, String titulo, String desc, Set<Rol> r,
            List<HerramientaEscolar.Campo> campos, java.util.function.Function<HerramientaEscolar.Entrada, String> f) {
        return new HerramientaEscolar(id, grupo, titulo, desc, r, campos, f);
    }

    private static HerramientaEscolar codigo(String id, String grupo, String titulo, String desc, String ejemplo,
            String formato, Predicate<String> valida, Set<Rol> r) {
        return herramienta(id, grupo, titulo, desc, r, List.of(texto("codigo", "Código", ejemplo)),
                e -> "Código «" + e.txt("codigo") + "»: " + (valida.test(e.txt("codigo")) ? "VÁLIDO" : "NO VÁLIDO")
                        + "\nFormato esperado: " + formato + " (ejemplo " + ejemplo + ")");
    }

    private static List<HerramientaEscolar> construir() {
        List<HerramientaEscolar> l = new ArrayList<>();

        // ---------- Académico ----------
        l.add(herramienta("aptitud-fisica", ACADEMICO, "Aptitud física", "Califica el desempeño deportivo con cuatro pruebas.",
                roles(PROFESOR, DIRECTOR),
                List.of(num("velocidad", "Velocidad 50 m (segundos)", "8.5"), ent("abdominales", "Abdominales en 1 minuto", "35"),
                        num("flexibilidad", "Flexibilidad (cm)", "25"), num("resistencia", "Resistencia Cooper (metros)", "2400")),
                e -> "Desempeño: " + CalculadorAptitudFisica.calificarDesempeno(new CalculadorAptitudFisica.EvaluacionFisica(
                        e.dbl("velocidad"), e.ent("abdominales"), e.dbl("flexibilidad"), e.dbl("resistencia")))));

        l.add(herramienta("promedio-grupal", ACADEMICO, "Estadísticas de un grupo", "Promedio, mediana, aprobación y distribución de notas.",
                roles(PROFESOR, DIRECTOR),
                List.of(lista("notas", "Notas (0-100, separadas por coma)", "85, 70, 92, 55, 78, 64, 88")),
                e -> {
                    List<BigDecimal> notas = e.items("notas").stream().map(s -> {
                        try { return new BigDecimal(s.replace(',', '.')); }
                        catch (NumberFormatException x) { throw new IllegalArgumentException("Nota no válida: " + s); }
                    }).toList();
                    CalculadorPromedioGrupal.EstadisticasGrupo g = CalculadorPromedioGrupal.calcular(notas);
                    return "Estudiantes: " + g.totalEstudiantes() + "\nPromedio: " + g.promedio() + "   Mediana: " + g.mediana()
                            + "\nMáxima: " + g.notaMaxima() + "   Mínima: " + g.notaMinima()
                            + "\nAprobados: " + g.aprobados() + "   Reprobados: " + g.reprobados()
                            + "\nTasa de aprobación: " + g.tasaAprobacion() + " %"
                            + "\nDistribución: " + CalculadorPromedioGrupal.distribucionPorRango(notas);
                }));

        l.add(herramienta("prueba-diagnostica", ACADEMICO, "Prueba diagnóstica", "Nivel de entrada en matemáticas y lectura.",
                roles(PROFESOR, DIRECTOR),
                List.of(num("mat", "Puntaje de matemáticas (0-100)", "75"), num("lec", "Puntaje de lectura (0-100)", "68")),
                e -> {
                    CalculadorPruebaDiagnostica.ResultadoDiagnostico r = CalculadorPruebaDiagnostica.evaluar(e.dbl("mat"), e.dbl("lec"));
                    return "Promedio diagnóstico: " + r.promedioDiagnostico() + "\nNivel: " + r.nivel()
                            + "\nRequiere refuerzo pedagógico: " + si(r.requiereRefuerzoPedagogico());
                }));

        l.add(herramienta("riesgo-desercion", ACADEMICO, "Riesgo de deserción", "Índice de riesgo según inasistencias, notas y conducta.",
                roles(PROFESOR, DIRECTOR),
                List.of(num("inasistencia", "Inasistencias (%)", "25"), num("promedio", "Promedio de calificaciones", "70"),
                        ent("faltas", "Faltas de conducta", "1")),
                e -> {
                    CalculadorRiesgoDesercion.DiagnosticoRiesgo d = CalculadorRiesgoDesercion.evaluar(
                            e.dbl("inasistencia"), e.dbl("promedio"), e.ent("faltas"));
                    return "Índice de riesgo: " + d.indiceRiesgoPonderado() + " / 100\nNivel: " + d.nivel().getDescripcion()
                            + "\nIntervención inmediata: " + si(d.requiereIntervencionInmediata()) + "\nMotivo: " + d.motivoPrincipal();
                }));

        l.add(herramienta("creditos", ACADEMICO, "Créditos académicos", "Créditos aprobados, promedio ponderado y elegibilidad de graduación.",
                roles(DIRECTOR, SECRETARIA, ESTUDIANTE),
                List.of(lista("asignaturas", "Una por línea: código;nombre;créditos;nota",
                        "MAT-101;Matemática;4;88\nLEN-101;Lengua Española;4;72\nCIE-101;Ciencias;3;65")),
                e -> {
                    List<CalculadorCreditosAcademicos.AsignaturaCursada> lista = new ArrayList<>();
                    for (String linea : e.lineas("asignaturas")) {
                        String[] p = linea.split(";");
                        if (p.length != 4) throw new IllegalArgumentException("Formato de línea no válido: " + linea);
                        try {
                            lista.add(new CalculadorCreditosAcademicos.AsignaturaCursada(p[0].trim(), p[1].trim(),
                                    Integer.parseInt(p[2].trim()), new BigDecimal(p[3].trim().replace(',', '.'))));
                        } catch (NumberFormatException x) {
                            throw new IllegalArgumentException("Créditos o nota no numéricos en: " + linea);
                        }
                    }
                    CalculadorCreditosAcademicos.ResumenCreditos r = CalculadorCreditosAcademicos.calcular(lista);
                    return "Créditos aprobados: " + r.creditosAprobados() + " de " + r.creditosRequeridos()
                            + "\nCréditos pendientes: " + r.creditosPendientes() + "\nPromedio acumulado: " + r.promedioAcumulado()
                            + "\nElegible para graduación: " + si(r.eligibleGraduacion());
                }));

        l.add(herramienta("proyecto-integrador", ACADEMICO, "Proyecto integrador", "Valida el proyecto y calcula la nota con la rúbrica ponderada.",
                roles(PROFESOR, DIRECTOR, ESTUDIANTE),
                List.of(texto("codigo", "Código (PROY-…)", "PROY-001"), texto("titulo", "Título (mín. 10 caracteres)", "Huerto escolar sostenible"),
                        lista("integrantes", "Integrantes (códigos, 1 a 5)", "E-101, E-102, E-103"),
                        num("n1", "Planteamiento del problema (20 %)", "85"), num("n2", "Marco teórico (20 %)", "80"),
                        num("n3", "Metodología (20 %)", "78"), num("n4", "Resultados y análisis (25 %)", "82"),
                        num("n5", "Presentación oral (15 %)", "90")),
                e -> {
                    ValidadorProyectoIntegrador.RubricaEvaluacion rub = new ValidadorProyectoIntegrador.RubricaEvaluacion(
                            e.dec("n1"), e.dec("n2"), e.dec("n3"), e.dec("n4"), e.dec("n5"));
                    ValidadorProyectoIntegrador.ResultadoValidacion r = ValidadorProyectoIntegrador.validar(
                            new ValidadorProyectoIntegrador.ProyectoIntegrador(e.txt("codigo"), e.txt("titulo"), e.items("integrantes"), rub));
                    return (r.valido() ? "Proyecto válido" : "Proyecto no válido") + ": " + r.mensaje()
                            + (r.valido() ? "\nNota final: " + r.notaFinal() + "\nAprobado: " + si(ValidadorProyectoIntegrador.aprobado(rub)) : "");
                }));

        l.add(herramienta("plan-refuerzo", ACADEMICO, "Plan de refuerzo académico", "Valida el plan y mide la mejora de la nota.",
                roles(PROFESOR, DIRECTOR, ESTUDIANTE),
                List.of(texto("codigo", "Código (REF-…)", "REF-001"), texto("estudiante", "Código del estudiante", "E-101"),
                        texto("asignatura", "Asignatura", "Matemática"), num("inicial", "Nota inicial", "55"),
                        ent("sesiones", "Sesiones planificadas (3 a 30)", "10"), fecha("inicio", "Fecha de inicio", hoy(1)),
                        fecha("fin", "Fecha de fin", hoy(45)), op("estatus", "Estatus", ValidadorPlanRefuerzoAcademico.EstatusRefuerzo.class),
                        num("final", "Nota final (para medir la mejora)", "72")),
                e -> {
                    BigDecimal inicial = e.dec("inicial");
                    ValidadorPlanRefuerzoAcademico.ResultadoValidacion r = ValidadorPlanRefuerzoAcademico.validar(
                            new ValidadorPlanRefuerzoAcademico.PlanRefuerzo(e.txt("codigo"), e.txt("estudiante"), e.txt("asignatura"),
                                    inicial, e.ent("sesiones"), e.fecha("inicio"), e.fecha("fin"),
                                    e.enumerado(ValidadorPlanRefuerzoAcademico.EstatusRefuerzo.class, "estatus")));
                    return (r.aprobado() ? "Plan válido" : "Plan no válido") + ": " + r.mensaje()
                            + "\nRequiere refuerzo: " + si(ValidadorPlanRefuerzoAcademico.requiereRefuerzo(inicial))
                            + "\nMejora de la nota: " + ValidadorPlanRefuerzoAcademico.calcularPorcentajeMejora(inicial, e.dec("final")) + " %";
                }));

        l.add(herramienta("feria-cientifica", ACADEMICO, "Feria científica", "Puntaje y lugar de un proyecto según el baremo.",
                roles(PROFESOR, DIRECTOR),
                List.of(num("original", "Originalidad (0-25)", "20"), num("rigor", "Rigor científico (0-30)", "25"),
                        num("exposicion", "Exposición oral (0-25)", "20"), num("prototipo", "Prototipo práctico (0-20)", "15")),
                e -> {
                    double p = CalculadorFeriaCientifica.calcularPuntajeTotal(new CalculadorFeriaCientifica.EvaluacionProyecto(
                            e.dbl("original"), e.dbl("rigor"), e.dbl("exposicion"), e.dbl("prototipo")));
                    return "Puntaje total: " + p + " / 100\nResultado: " + CalculadorFeriaCientifica.determinarLugar(p);
                }));

        l.add(herramienta("calendario", ACADEMICO, "Días hábiles del calendario", "Cuenta los días hábiles de un rango descontando feriados.",
                roles(DIRECTOR, SECRETARIA, PROFESOR, ESTUDIANTE, PADRE),
                List.of(ent("anio", "Año académico", String.valueOf(LocalDate.now().getYear())), fecha("desde", "Desde", hoy(0)),
                        fecha("hasta", "Hasta", hoy(30)), lista("feriados", "Feriados (fechas AAAA-MM-DD separadas por coma)", "")),
                e -> {
                    GestorCalendarioAcademico cal = new GestorCalendarioAcademico(e.ent("anio"));
                    int n = 1;
                    for (String f : e.items("feriados")) {
                        try {
                            cal.registrarEvento(new GestorCalendarioAcademico.EventoAcademico("F" + n++, "Feriado", LocalDate.parse(f),
                                    GestorCalendarioAcademico.TipoEvento.FERIADO_NACIONAL));
                        } catch (java.time.format.DateTimeParseException x) {
                            throw new IllegalArgumentException("Feriado con fecha no válida: " + f);
                        }
                    }
                    LocalDate d = e.fecha("desde"), h = e.fecha("hasta");
                    if (d.isAfter(h)) throw new IllegalArgumentException("«Desde» debe ser anterior a «Hasta».");
                    return "Días hábiles (lunes a viernes, sin feriados): " + cal.contarDiasHabilesEnRango(d, h)
                            + "\nFeriados considerados: " + cal.getEventos().size();
                }));

        // ---------- Docentes ----------
        l.add(herramienta("carga-docente", DOCENTES, "Carga docente", "Detecta sobrecarga de horas y secciones.",
                roles(DIRECTOR, SECRETARIA),
                List.of(ent("horas", "Horas semanales", "30"), ent("secciones", "Secciones", "5"), ent("estudiantes", "Estudiantes", "150"),
                        op("contrato", "Contrato", CalculadorCargaDocente.ModalidadContrato.class)),
                e -> {
                    CalculadorCargaDocente.EvaluacionCarga r = CalculadorCargaDocente.evaluar(e.ent("horas"), e.ent("secciones"),
                            e.ent("estudiantes"), e.enumerado(CalculadorCargaDocente.ModalidadContrato.class, "contrato"));
                    return "Estado: " + r.estadoCarga() + "\nHoras: " + r.totalHorasSemanales() + "   Secciones: " + r.totalSecciones()
                            + "\nEstudiantes por sección: " + r.promedioEstudiantesPorSeccion()
                            + "\nSobrecarga de horas: " + si(r.sobrecargaHoras()) + "   Sobrecarga de secciones: " + si(r.sobrecargaSecciones());
                }));

        l.add(herramienta("evaluacion-docente", DOCENTES, "Evaluación docente", "Puntaje y valoración cualitativa del desempeño.",
                roles(DIRECTOR),
                List.of(num("dominio", "Dominio del tema (1-5)", "4.5"), num("puntualidad", "Puntualidad (1-5)", "4"),
                        num("metodologia", "Metodología (1-5)", "4.2"), num("relacion", "Relación con estudiantes (1-5)", "4.8")),
                e -> {
                    double p = CalculadorEvaluacionDocente.calcularPuntajeTotal(new CalculadorEvaluacionDocente.EvaluacionDocente(
                            e.dbl("dominio"), e.dbl("puntualidad"), e.dbl("metodologia"), e.dbl("relacion")));
                    return "Puntaje: " + p + " / 5\nValoración: " + CalculadorEvaluacionDocente.calificarCualitativo(p);
                }));

        l.add(herramienta("titulo-docente", DOCENTES, "Título del docente", "Valida el registro del título universitario.",
                roles(DIRECTOR, SECRETARIA),
                List.of(texto("registro", "Registro (TIT-AAAA-NNNN)", "TIT-2026-1234"), texto("titulo", "Título obtenido", "Licenciatura en Educación"),
                        texto("universidad", "Universidad", "UNEV"), ent("anio", "Año de graduación", "2018")),
                e -> "Acreditación: " + (ValidadorTituloDocente.esAcreditacionValida(new ValidadorTituloDocente.AcreditacionDocente(
                        e.txt("registro"), e.txt("titulo"), e.txt("universidad"), e.ent("anio"))) ? "VÁLIDA" : "NO VÁLIDA")
                        + "\nRegistro con formato correcto: " + si(ValidadorTituloDocente.esNumeroRegistroValido(e.txt("registro")))));

        l.add(herramienta("reunion-consejo", DOCENTES, "Reunión de consejo", "Quórum y acta resumida de una reunión de docentes.",
                roles(DIRECTOR, SECRETARIA, PROFESOR),
                List.of(texto("codigo", "Código (CONS-…)", "CONS-001"), op("tipo", "Tipo", GestorReunionConsejo.TipoReunion.class),
                        fecha("fecha", "Fecha", hoy(7)), hora("hora", "Hora", "15:00"), ent("quorum", "Quórum mínimo", "3"),
                        lista("convocados", "Docentes convocados", "D-1, D-2, D-3, D-4"), lista("asistentes", "Docentes presentes", "D-1, D-2, D-3")),
                e -> {
                    GestorReunionConsejo r = new GestorReunionConsejo(e.txt("codigo"), e.enumerado(GestorReunionConsejo.TipoReunion.class, "tipo"),
                            LocalDateTime.of(e.fecha("fecha"), e.hora("hora")), e.ent("quorum"));
                    e.items("convocados").forEach(r::convocar);
                    e.items("asistentes").forEach(r::registrarAsistencia);
                    boolean quorum = r.hayQuorum();
                    if (quorum) r.iniciarReunion();
                    return "Quórum: " + (quorum ? "se cumple" : "no se cumple") + "\nEstatus: " + r.getEstatus() + "\n" + r.generarActaResumen();
                }));

        // ---------- Bienestar y seguridad ----------
        l.add(herramienta("excusa", BIENESTAR, "Excusa de inasistencia", "Evalúa si una excusa es válida y si habilita reprogramar el examen.",
                roles(PADRE, SECRETARIA, DIRECTOR, ESTUDIANTE),
                List.of(op("tipo", "Tipo de excusa", ValidadorExcusaEscolar.TipoExcusa.class), fecha("inasistencia", "Fecha de la inasistencia", hoy(-2)),
                        fecha("presentacion", "Fecha de presentación", hoy(0)), sn("comprobante", "Adjunta comprobante", true)),
                e -> {
                    ValidadorExcusaEscolar.EvaluacionExcusa r = ValidadorExcusaEscolar.evaluar(
                            e.enumerado(ValidadorExcusaEscolar.TipoExcusa.class, "tipo"), e.fecha("inasistencia"), e.fecha("presentacion"), e.sn("comprobante"));
                    return "Excusa válida: " + si(r.esValida()) + "\nHabilita reprogramar examen: " + si(r.habilitaReprogramacionExamen()) + "\n" + r.dictamen();
                }));

        l.add(herramienta("comedor", BIENESTAR, "Comedor y alérgenos", "Comprueba si un menú es apto según las alergias del estudiante.",
                roles(SECRETARIA, PADRE, ESTUDIANTE),
                List.of(op("plan", "Plan alimentario", ValidadorComedorEscolar.PlanAlimentario.class), lista("alergias", "Alergias", "MANI, HUEVO"),
                        sn("supervision", "Requiere supervisión", true), lista("ingredientes", "Ingredientes del plato", "ARROZ, POLLO, VEGETALES")),
                e -> {
                    boolean apto = ValidadorComedorEscolar.esAptoMenu(new ValidadorComedorEscolar.FichaNutricional(
                            e.enumerado(ValidadorComedorEscolar.PlanAlimentario.class, "plan"), Set.copyOf(e.items("alergias")), e.sn("supervision")),
                            Set.copyOf(e.items("ingredientes")));
                    return "Menú apto: " + si(apto) + (apto ? "" : "\nSe detectó un alérgeno en el plato. No servir.");
                }));

        l.add(herramienta("retiro-estudiante", BIENESTAR, "Retiro de estudiante", "Verifica si una persona autorizada puede retirar a un alumno (ejemplo).",
                roles(SECRETARIA, PADRE, DIRECTOR),
                List.of(texto("nombre", "Nombre del autorizado", "María Pérez"), texto("parentesco", "Parentesco", "Tía"),
                        texto("cedula", "Cédula", "001-0000002-5"), texto("pin", "PIN registrado (4 a 6 dígitos)", "4821"),
                        sn("activo", "Autorización activa", true), texto("pinIngresado", "PIN presentado", "4821")),
                e -> {
                    ValidadorRetiroEstudiante.AutorizadoRetiro a = new ValidadorRetiroEstudiante.AutorizadoRetiro(
                            e.txt("nombre"), e.txt("parentesco"), e.txt("cedula"), e.txt("pin"), e.sn("activo"));
                    return "PIN con formato válido: " + si(ValidadorRetiroEstudiante.esPinValido(e.txt("pin")))
                            + "\nPuede retirar al estudiante: " + si(ValidadorRetiroEstudiante.puedeRetirar(a, e.txt("pinIngresado")))
                            + "\nSimulación: no se guarda ningún PIN.";
                }));

        l.add(herramienta("permiso-permanencia", BIENESTAR, "Permiso de permanencia", "Valida la permanencia de un estudiante después del horario regular.",
                roles(PADRE, SECRETARIA, DIRECTOR, PROFESOR),
                List.of(texto("codigo", "Código (PERM-…)", "PERM-001"), texto("estudiante", "Código del estudiante", "E-101"),
                        hora("hora", "Hora de salida", "18:30"), ent("dias", "Días consecutivos (1 a 5)", "2"),
                        op("motivo", "Motivo", ValidadorPermisoPermanencia.MotivoPermanencia.class), sn("tutor", "Autorizado por el tutor", true)),
                e -> {
                    ValidadorPermisoPermanencia.ResultadoValidacion r = ValidadorPermisoPermanencia.validar(new ValidadorPermisoPermanencia.SolicitudPermiso(
                            e.txt("codigo"), e.txt("estudiante"), e.hora("hora"), e.ent("dias"),
                            e.enumerado(ValidadorPermisoPermanencia.MotivoPermanencia.class, "motivo"), e.sn("tutor")));
                    return (r.aprobado() ? "Permiso aprobado" : "Permiso rechazado") + ": " + r.mensaje();
                }));

        l.add(herramienta("transporte", BIENESTAR, "Transporte escolar", "Valida la ruta, la parada y la autorización de los padres.",
                roles(SECRETARIA, PADRE, DIRECTOR),
                List.of(texto("ruta", "Ruta (RUTA-NN)", "RUTA-12"), ent("autobus", "Número de autobús", "5"), texto("parada", "Parada", "Parque central"),
                        op("turno", "Turno", "MATUTINO", "VESPERTINO"), sn("autorizacion", "Autorización de padres firmada", true)),
                e -> "Asignación: " + (ValidadorTransporteEscolar.esAsignacionValida(new ValidadorTransporteEscolar.AsignacionTransporte(
                        e.txt("ruta"), e.ent("autobus"), e.txt("parada"), e.txt("turno"), e.sn("autorizacion"))) ? "VÁLIDA" : "NO VÁLIDA")
                        + "\nCódigo de ruta correcto: " + si(ValidadorTransporteEscolar.esCodigoRutaValido(e.txt("ruta")))));

        l.add(codigo("enfermeria-codigo", BIENESTAR, "Código de atención de enfermería", "Valida el código de una atención de enfermería.",
                "ENF-2026-0001", "ENF-AAAA-NNNN", ValidadorEnfermeriaEscolar::esCodigoAtencionValido, roles(SECRETARIA, DIRECTOR, PROFESOR)));
        l.add(codigo("excursion-codigo", BIENESTAR, "Código de excursión", "Valida el código de una salida escolar.",
                "EXC-2026-001", "EXC-AAAA-NNN", ValidadorExcursionEscolar::esCodigoExcursionValido, roles(PROFESOR, DIRECTOR, SECRETARIA)));
        l.add(codigo("citacion-codigo", BIENESTAR, "Código de citación a padres", "Valida el código de una citación formal.",
                "CIT-2026-0001", "CIT-AAAA-NNNN", ValidadorCitacionPadres::esCodigoCitacionValido, roles(DIRECTOR, SECRETARIA)));

        // ---------- Administración ----------
        l.add(herramienta("finanzas", ADMINISTRACION, "Mensualidad y mora", "Descuentos por hermanos y pronto pago, y recargo por mora.",
                roles(SECRETARIA, DIRECTOR, PADRE),
                List.of(num("monto", "Mensualidad (RD$)", "5000"), ent("hermanos", "Hermanos inscritos", "1"),
                        sn("pronto", "Pago anticipado", false), ent("meses", "Meses vencidos", "0")),
                e -> {
                    CalculadorFinanzasEscolares.EstadoCuentaEstudiante c = CalculadorFinanzasEscolares.calcularCuota(
                            e.dbl("monto"), e.ent("hermanos"), e.sn("pronto"), e.ent("meses"));
                    return "Monto base: RD$ " + c.montoBase() + "\nDescuento: RD$ " + c.descuentoAplicado() + "\nRecargo por mora: RD$ " + c.recargoMora()
                            + "\nSaldo a pagar: RD$ " + c.saldoFinalPagar() + "\nAl día: " + si(c.alDia());
                }));

        l.add(herramienta("inventario", ADMINISTRACION, "Inventario escolar", "Valor total y artículos deteriorados.",
                roles(SECRETARIA, DIRECTOR),
                List.of(lista("articulos", "Una por línea: código;descripción;categoría;cantidad;valor;estado",
                        "INV-001;Pupitre;MOBILIARIO;40;1500;BUENO\nINV-002;Proyector;EQUIPO_TECNOLOGICO;2;18000;DETERIORADO")),
                e -> {
                    GestorInventarioEscolar inv = new GestorInventarioEscolar();
                    for (String linea : e.lineas("articulos")) {
                        String[] p = linea.split(";");
                        if (p.length != 6) throw new IllegalArgumentException("Formato de línea no válido: " + linea);
                        try {
                            inv.registrarArticulo(new GestorInventarioEscolar.ArticuloInventario(p[0].trim(), p[1].trim(),
                                    GestorInventarioEscolar.CategoriaArticulo.valueOf(p[2].trim()), Integer.parseInt(p[3].trim()),
                                    new BigDecimal(p[4].trim().replace(',', '.')), GestorInventarioEscolar.EstadoArticulo.valueOf(p[5].trim())));
                        } catch (NumberFormatException x) {
                            throw new IllegalArgumentException("Cantidad o valor no numéricos en: " + linea);
                        }
                    }
                    return "Artículos: " + inv.totalArticulos() + "\nValor total: RD$ " + inv.calcularValorTotalInventario()
                            + "\nEn deterioro o dados de baja: " + inv.obtenerArticulosDeterioro().size();
                }));

        l.add(herramienta("banco-libros", ADMINISTRACION, "Banco de libros", "Indica si corresponde cobrar reposición de un libro.",
                roles(SECRETARIA, PROFESOR),
                List.of(op("entrega", "Estado al entregar", GestorBancoLibrosTexto.EstadoFisicoLibro.class),
                        op("devolucion", "Estado al devolver", GestorBancoLibrosTexto.EstadoFisicoLibro.class), sn("devuelto", "Fue devuelto", true)),
                e -> "Cobrar reposición: " + si(GestorBancoLibrosTexto.requiereCobroReposicion(
                        e.enumerado(GestorBancoLibrosTexto.EstadoFisicoLibro.class, "entrega"),
                        e.enumerado(GestorBancoLibrosTexto.EstadoFisicoLibro.class, "devolucion"), e.sn("devuelto")))));

        l.add(herramienta("prestamo-dispositivo", ADMINISTRACION, "Préstamo de dispositivos", "Mora y días de retraso en la devolución de tablets o laptops.",
                roles(SECRETARIA, PROFESOR, ESTUDIANTE),
                List.of(fecha("esperada", "Fecha de devolución esperada", hoy(-3)), fecha("actual", "Fecha de hoy", hoy(0))),
                e -> "En mora: " + si(ValidadorPrestamoDispositivo.estaEnMora(e.fecha("esperada"), e.fecha("actual")))
                        + "\nDías de retraso: " + ValidadorPrestamoDispositivo.diasRetraso(e.fecha("esperada"), e.fecha("actual"))
                        + "\nPlazo máximo de préstamo: " + ValidadorPrestamoDispositivo.PLAZO_MAXIMO_PRESTAMO_DIAS + " días"));

        l.add(herramienta("transferencia", ADMINISTRACION, "Transferencia escolar", "Valida la solicitud de traslado a otro centro.",
                roles(SECRETARIA, DIRECTOR, PADRE),
                List.of(texto("codigo", "Código (TRF-AAAA-XXXXX)", "TRF-2026-AB123"), texto("origen", "Centro de origen", "Centro Educativo A"),
                        texto("destino", "Centro de destino", "Centro Educativo B"), texto("motivo", "Motivo", "Cambio de residencia"),
                        sn("paz", "Paz y salvo financiero", true), sn("record", "Récord de notas anexo", true)),
                e -> "Solicitud: " + (ValidadorTransferenciaEscolar.esSolicitudValida(new ValidadorTransferenciaEscolar.SolicitudTransferencia(
                        e.txt("codigo"), e.txt("origen"), e.txt("destino"), e.txt("motivo"), e.sn("paz"), e.sn("record"))) ? "VÁLIDA" : "NO VÁLIDA")
                        + "\nCódigo con formato correcto: " + si(ValidadorTransferenciaEscolar.esCodigoTransferenciaValido(e.txt("codigo")))));

        l.add(codigo("admision-codigo", ADMINISTRACION, "Código de solicitud de admisión", "Valida el código de una solicitud de nuevo ingreso.",
                "ADM-2026-00001", "ADM-AAAA-NNNNN", ValidadorAdmisionNuevoIngreso::esCodigoSolicitudValido, roles(SECRETARIA, DIRECTOR)));

        // ---------- Familia y comunidad ----------
        l.add(herramienta("escuela-padres", FAMILIA, "Escuela de padres", "Cumplimiento del compromiso familiar de talleres.",
                roles(SECRETARIA, DIRECTOR, PADRE),
                List.of(ent("asistidos", "Talleres a los que asistió", "2")),
                e -> {
                    int n = Math.max(0, Math.min(50, e.ent("asistidos")));
                    List<GestorEscuelaParaPadres.AsistenciaTaller> t = new ArrayList<>();
                    for (int i = 1; i <= n; i++) t.add(new GestorEscuelaParaPadres.AsistenciaTaller("Taller " + i, "", true));
                    return "Cumple el compromiso familiar: " + si(GestorEscuelaParaPadres.cumpleCompromisoFamiliar(t))
                            + "\nMínimo requerido por ciclo: " + GestorEscuelaParaPadres.TALLERES_MINIMOS_CICLO + " talleres";
                }));

        l.add(herramienta("club", FAMILIA, "Clubes extracurriculares", "Comprueba el límite de clubes y las horas de participación.",
                roles(ESTUDIANTE, PROFESOR, PADRE),
                List.of(ent("clubes", "Clubes actuales del estudiante", "1"), ent("horas", "Horas semanales en el club", "3")),
                e -> "Puede inscribirse en otro club: " + si(ValidadorClubExtracurricular.puedeInscribirClub(e.ent("clubes")))
                        + " (máximo " + ValidadorClubExtracurricular.MAX_CLUBES_POR_ESTUDIANTE + ")"
                        + "\nHoras válidas: " + si(ValidadorClubExtracurricular.sonHorasValidas(e.ent("horas")))));

        l.add(herramienta("voto-estudiantil", FAMILIA, "Voto estudiantil", "Valida un voto dentro del período de elecciones.",
                roles(ESTUDIANTE, PROFESOR, DIRECTOR),
                List.of(texto("votante", "Código del votante", "SIGE-2026-12345"), texto("plancha", "Plancha", "PLANCHA-A"),
                        fecha("fecha", "Fecha del voto", hoy(0)), fecha("inicio", "Inicio de elecciones", hoy(-1)), fecha("fin", "Fin de elecciones", hoy(1))),
                e -> "Voto válido: " + si(ValidadorVotoEstudiantil.esVotoValido(
                        new ValidadorVotoEstudiantil.VotoEmitido(e.txt("votante"), e.txt("plancha"), e.fecha("fecha")), e.fecha("inicio"), e.fecha("fin")))));

        l.add(codigo("egresado-codigo", FAMILIA, "Código de egresado", "Valida el código de la red de egresados.",
                "ALM-2026-0001", "ALM-AAAA-NNNN", ValidadorRedEgresados::esCodigoEgresadoValido, roles(SECRETARIA, DIRECTOR)));
        l.add(codigo("publicacion-codigo", FAMILIA, "Código de publicación estudiantil", "Valida el código de un artículo del periódico escolar.",
                "PUB-ABC-2026-001", "PUB-XXX-AAAA-NNN", GestorPublicacionEscolar::esCodigoPublicacionValido, roles(PROFESOR, ESTUDIANTE, SECRETARIA)));

        // ---------- Documentos y códigos ----------
        l.add(herramienta("carnet", DIGITAL, "Carnet digital con QR", "Genera y analiza el contenido del código QR del carnet.",
                roles(SECRETARIA, DIRECTOR, ESTUDIANTE, PADRE),
                List.of(texto("cue", "Código único del estudiante", "SIGE-2026-12345"), ent("anio", "Año de vigencia", String.valueOf(LocalDate.now().getYear())),
                        texto("firma", "Firma (6 a 12 letras o números)", "ABC123XYZ")),
                e -> {
                    String payload = ValidadorCarnetInteligente.generarPayloadQr(e.txt("cue"), e.ent("anio"), e.txt("firma").toUpperCase());
                    ValidadorCarnetInteligente.CarnetDigital c = ValidadorCarnetInteligente.analizar(payload, LocalDate.now().getYear());
                    return "Contenido del QR: " + payload + "\nFormato válido: " + si(ValidadorCarnetInteligente.esPayloadValido(payload))
                            + "\nVigente este año: " + si(c.estaVigente());
                }));

        l.add(herramienta("casillero", DIGITAL, "Casillero escolar", "Genera el código de un casillero por pabellón.",
                roles(SECRETARIA, ESTUDIANTE),
                List.of(texto("pabellon", "Pabellón (una letra)", "B"), ent("numero", "Número de casillero", "12")),
                e -> {
                    String p = e.txt("pabellon");
                    if (p.length() != 1 || !Character.isLetter(p.charAt(0))) throw new IllegalArgumentException("El pabellón debe ser una sola letra.");
                    String c = ValidadorCasilleroEscolar.generarCodigo(p.charAt(0), e.ent("numero"));
                    return "Código del casillero: " + c + "\nFormato válido: " + si(ValidadorCasilleroEscolar.esCodigoCasilleroValido(c));
                }));

        l.add(herramienta("firma-digital", DIGITAL, "Documento certificado", "Certificación con código de integridad y enlace de verificación.",
                roles(SECRETARIA, DIRECTOR),
                List.of(texto("codigo", "Código (DOC-XXXX-XXXX-XXXX)", "DOC-AB12-CD34-EF56"), texto("tipo", "Tipo de documento", "BOLETIN"),
                        texto("matricula", "Matrícula del estudiante", "2026-0001"), texto("hash", "Hash de seguridad", "A1B2C3")),
                e -> {
                    ValidadorFirmaDigitalDocumento.DocumentoCertificado d = ValidadorFirmaDigitalDocumento.certificar(
                            e.txt("codigo"), e.txt("tipo"), e.txt("matricula"), e.txt("hash"));
                    return "Documento: " + d.codigoDocumento() + " (" + d.tipoDocumento() + ")\nVerificación: " + d.urlVerificacion()
                            + "\nCódigo con formato correcto: " + si(ValidadorFirmaDigitalDocumento.esCodigoValido(d.codigoDocumento()));
                }));

        l.add(codigo("laboratorio-mac", DIGITAL, "Equipo de laboratorio (MAC)", "Valida la dirección MAC de un equipo de la sala de cómputo.",
                "AA:BB:CC:DD:EE:FF", "seis pares hexadecimales separados por «:» o «-»", ValidadorLaboratorioInformatica::esDireccionMacValida,
                roles(PROFESOR, DIRECTOR)));

        return List.copyOf(l);
    }
}
