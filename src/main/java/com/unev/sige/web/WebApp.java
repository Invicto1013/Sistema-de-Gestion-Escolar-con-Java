package com.unev.sige.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.unev.sige.data.Autenticacion2FADAO;
import com.unev.sige.data.AsistenciaDAO;
import com.unev.sige.data.AulaDAO;
import com.unev.sige.data.AsignaturaDAO;
import com.unev.sige.data.BoletaDAO;
import com.unev.sige.data.CalificacionDAO;
import com.unev.sige.data.ConexionBD;
import com.unev.sige.data.CorreoNotificaciones;
import com.unev.sige.data.CurriculoDAO;
import com.unev.sige.data.DashboardDAO;
import com.unev.sige.data.InscripcionDAO;
import com.unev.sige.data.InscripcionInvalidaException;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.ReporteDAO;
import com.unev.sige.data.SesionDAO;
import com.unev.sige.data.SesionWebDAO;
import com.unev.sige.data.SeccionDAO;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.data.VinculacionDAO;
import com.unev.sige.model.ControladorSesion;
import com.unev.sige.model.GradoCurricular;
import com.unev.sige.model.Calificacion;
import com.unev.sige.model.Asignatura;
import com.unev.sige.model.FilaAsistencia;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.LineaBoleta;
import com.unev.sige.model.ResumenAsistencia;
import com.unev.sige.model.SesionRegistrada;
import com.unev.sige.model.Seccion;
import com.unev.sige.model.NotaEstudiante;
import com.unev.sige.model.LineaReporteInscripcion;
import com.unev.sige.model.Rol;
import com.unev.sige.model.PoliticaContrasena;
import com.unev.sige.model.Usuario;
import com.unev.sige.model.VinculoPadreEstudiante;
import com.unev.sige.security.Configuracion2FAException;
import com.unev.sige.security.TOTP;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.InetSocketAddress;
import java.net.BindException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Servidor web ligero de SIGE. La JavaFX sigue siendo la aplicación de escritorio. */
public final class WebApp {
    private static final String COOKIE = "SIGEWEB";
    private static final long MAX_IDLE_SECONDS = 8 * 60 * 60;
    private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
    private static final int MAX_MULTIPART_BYTES = MAX_UPLOAD_BYTES + 256 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, WebSession> SESSIONS = new ConcurrentHashMap<>();
    private static final SesionWebDAO SESIONES_WEB = new SesionWebDAO();
    private static volatile long ultimaPurgaSesiones = 0;
    private static volatile HttpServer SERVER;
    private static volatile ExecutorService EXECUTOR;
    private static final ThreadLocal<Boolean> LOCAL_REQUEST = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ADMIN_REQUEST = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> ROLE_REQUEST = new ThreadLocal<>();
    private static final ThreadLocal<String> PATH_REQUEST = new ThreadLocal<>();
    private static final UsuarioDAO USERS = new UsuarioDAO();
    private static final Autenticacion2FADAO MFA = new Autenticacion2FADAO();

    private WebApp() { }

    public static void main(String[] args) throws IOException {
        com.unev.sige.app.RegistroLog.iniciar();
        String host = env("SIGE_WEB_HOST", "127.0.0.1");
        // En hosting (por ejemplo Render), PORT lo asigna la plataforma.
        // SIGE_WEB_PORT conserva prioridad para desarrollo y despliegues propios.
        int port = Integer.parseInt(env("SIGE_WEB_PORT", env("PORT", "8080")));
        try {
            SERVER = HttpServer.create(new InetSocketAddress(host, port), 64);
        } catch (BindException e) {
            System.err.println("No se inició SIGE Web: el puerto " + port
                    + " ya está ocupado. Detén la instancia anterior desde SIGE Web o con Ctrl+C.");
            return;
        }
        SERVER.createContext("/", WebApp::handle);
        EXECUTOR = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        SERVER.setExecutor(EXECUTOR);
        SERVER.start();
        Thread limpieza = new Thread(() -> {
            try { new AulaDAO().limpiarArchivosHuerfanos(); } catch (SQLException ignored) { /* la tabla puede no existir aún */ }
        }, "SIGE-Limpieza-Aula");
        limpieza.setDaemon(true);
        limpieza.start();
        System.out.println("SIGE Web disponible en http://" + host + ":" + port
                + " (SIGE_WEB_HOST permite configurar la interfaz de red).");
    }

    private static void handle(HttpExchange x) throws IOException {
        try {
            cleanupSessions();
            String path = x.getRequestURI().getPath();
            PATH_REQUEST.set(path);
            String method = x.getRequestMethod();
            if (path.equals("/styles.css") && method.equals("GET")) { css(x); return; }
            if (path.equals("/pwa.js") && method.equals("GET")) { send(x,200,"application/javascript; charset=utf-8","if('serviceWorker' in navigator){window.addEventListener('load',()=>navigator.serviceWorker.register('/sw.js').catch(()=>{}));}document.addEventListener('DOMContentLoaded',()=>{const b=document.querySelector('.menu-toggle'),n=document.querySelector('#main-navigation');if(!b||!n)return;const close=()=>{n.classList.remove('is-open');b.setAttribute('aria-expanded','false');b.setAttribute('aria-label','Abrir menú');};b.addEventListener('click',()=>{const open=b.getAttribute('aria-expanded')!=='true';n.classList.toggle('is-open',open);b.setAttribute('aria-expanded',String(open));b.setAttribute('aria-label',open?'Cerrar menú':'Abrir menú');});n.addEventListener('click',e=>{if(e.target.closest('a'))close();});document.addEventListener('keydown',e=>{if(e.key==='Escape')close();});document.addEventListener('click',e=>{if(!n.contains(e.target)&&!b.contains(e.target))close();});});"); return; }
            if (path.equals("/manifest.webmanifest") && method.equals("GET")) { send(x,200,"application/manifest+json; charset=utf-8",manifest()); return; }
            if (path.equals("/sw.js") && method.equals("GET")) { send(x,200,"application/javascript; charset=utf-8",serviceWorker()); return; }
            if ((path.equals("/logo.svg") || path.equals("/login-art.svg") || path.equals("/login-hero.svg") || path.equals("/dashboard-hero.svg")) && method.equals("GET")) { svgResource(x, path); return; }
            if (path.equals("/icon.svg") && method.equals("GET")) { send(x,200,"image/svg+xml; charset=utf-8",icon()); return; }
            if (path.equals("/health") && (method.equals("GET") || method.equals("HEAD"))) { health(x); return; }
            WebSession s = sessionFor(x);
            if (method.equals("GET")) {
                switch (path) {
                    case "/" -> page(x, s, home(s, null));
                    case "/registro" -> { if (s.user != null) redirect(x, "/"); else page(x, s, registroPage(s, null, Map.of())); }
                    case "/perfil" -> page(x, s, s.user == null ? errorPage("Inicia sesión para ver tu perfil.") : profilePage(s, x.getRequestURI().getRawQuery()));
                    case "/aula" -> page(x,s,aulaPage(s,x.getRequestURI().getRawQuery(),null));
                    case "/archivo-aula" -> downloadAulaFile(x,s,x.getRequestURI().getRawQuery());
                    case "/boleta" -> page(x, s, boletaPage(s, x.getRequestURI().getRawQuery()));
                    case "/asistencia" -> page(x, s, attendancePage(s, x.getRequestURI().getRawQuery()));
                    case "/materias" -> page(x, s, subjectsPage(s, x.getRequestURI().getRawQuery()));
                    case "/inscripciones" -> page(x, s, enrollmentPage(s, x.getRequestURI().getRawQuery(), null));
                    case "/calificaciones" -> page(x, s, gradesPage(s, x.getRequestURI().getRawQuery(), null));
                    case "/asistencia-docente" -> page(x, s, teacherAttendancePage(s, x.getRequestURI().getRawQuery(), null));
                    case "/reporte-asistencia" -> page(x, s, attendanceReportPage(s, x.getRequestURI().getRawQuery(), null));
                    case "/reporte-asistencia.csv" -> downloadAttendanceReport(x,s,x.getRequestURI().getRawQuery());
                    case "/reporte-inscripciones" -> page(x, s, enrollmentReportPage(s, x.getRequestURI().getRawQuery(), null));
                    case "/reporte-inscripciones.csv" -> downloadEnrollmentReport(x,s,x.getRequestURI().getRawQuery());
                    case "/asignaturas" -> page(x,s,subjectsAdminPage(s,x.getRequestURI().getRawQuery(),null,null));
                    case "/curriculo" -> page(x,s,curriculumPage(s));
                    case "/secciones" -> page(x,s,sectionsAdminPage(s,x.getRequestURI().getRawQuery(),null,null));
                    case "/usuarios" -> page(x,s,usersAdminPage(s,x.getRequestURI().getRawQuery(),null));
                    case "/solicitudes" -> page(x,s,solicitudesPage(s,x.getRequestURI().getRawQuery(),null));
                    case "/usuarios.csv" -> downloadUsersCsv(x,s,x.getRequestURI().getRawQuery());
                    case "/cambiar-rol" -> page(x,s,changeRolePage(s,x.getRequestURI().getRawQuery(),null));
                    case "/vinculaciones" -> page(x,s,parentLinksAdminPage(s,x.getRequestURI().getRawQuery(),null));
                    case "/crear-profesor" -> page(x,s,createTeacherPage(s,x.getRequestURI().getRawQuery(),null));
                    case "/mis-hijos" -> page(x,s,parentChildrenPage(s));
                    case "/historial-sesiones" -> page(x,s,sessionHistoryPage(s,x.getRequestURI().getRawQuery()));
                    case "/historial-sesiones.csv" -> downloadSessionHistoryCsv(x,s,x.getRequestURI().getRawQuery());
                    case "/2fa" -> page(x, s, mfaPage(s, null));
                    case "/password" -> page(x, s, passwordPage(s, null));
                    default -> page(x, s, errorPage("Página no encontrada."));
                }
                return;
            }
            if (!method.equals("POST")) { send(x, 405, "text/plain; charset=utf-8", "Método no permitido"); return; }
            Map<String,String> form; Map<String,UploadedFile> files=Map.of();
            String contentType=x.getRequestHeaders().getFirst("Content-Type");
            if(contentType!=null&&contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")){
                MultipartData data=multipartForm(x,contentType);form=data.fields();files=data.files();
            }else form=form(x);
            if (!constantEquals(s.csrf, form.get("_csrf"))) { page(x, s, errorPage("La solicitud expiró. Recarga la página e inténtalo otra vez.")); return; }
            String campoInvalido = LimitesCampos.validar(path, form);
            if (campoInvalido != null) { page(x, s, errorPage(campoInvalido)); return; }
            switch (path) {
                case "/login" -> login(x, s, form);
                case "/registro" -> registrar(x, s, form);
                case "/solicitud" -> requestAction(x, s, form);
                case "/verify-2fa" -> verifyMfa(x, s, form);
                case "/change-password" -> changePassword(x, s, form);
                case "/inscribir" -> enroll(x, s, form);
                case "/guardar-calificacion" -> saveGrade(x, s, form);
                case "/guardar-asistencia" -> saveAttendance(x, s, form);
                case "/aula-publicar" -> publishAula(x,s,form,files);
                case "/aula-entregar" -> submitAula(x,s,form,files);
                case "/guardar-asignatura" -> saveSubject(x,s,form);
                case "/eliminar-asignatura" -> deleteSubject(x,s,form);
                case "/guardar-seccion" -> saveSection(x,s,form);
                case "/eliminar-seccion" -> deleteSection(x,s,form);
                case "/accion-usuario" -> userAction(x,s,form);
                case "/cerrar-sesion-admin" -> closeSessionAsAdmin(x,s,form);
                case "/resetear-contrasena" -> resetUserPassword(x,s,form);
                case "/actualizar-rol" -> changeRole(x,s,form);
                case "/crear-vinculo" -> createParentLink(x,s,form);
                case "/eliminar-vinculo" -> deleteParentLink(x,s,form);
                case "/guardar-profesor" -> createTeacher(x,s,form);
                case "/logout" -> logout(x, s);
                case "/shutdown" -> shutdown(x, s);
                default -> page(x, s, errorPage("Página no encontrada."));
            }
        } catch (UploadTooLargeException e) {
            send(x,413,"text/plain; charset=utf-8","El archivo supera el límite de 10 MB.");
        } catch (FormTooLargeException e) {
            send(x,413,"text/plain; charset=utf-8","La solicitud es demasiado grande.");
        } catch (Exception e) {
            e.printStackTrace();
            page(x, sessionFor(x), errorPage("No se pudo completar la solicitud. Revisa la conexión con la base de datos."));
        } finally {
            PATH_REQUEST.remove();
            x.close();
        }
    }

    // ---- Registro público (ESTUDIANTE y PADRE; PROFESOR/ADMIN solo los crea un administrador) ----
    private static final Object REGISTRO_LOCK = new Object();
    private static long registroVentana = 0; private static int registroCuenta = 0;

    private static boolean registroPermitido() {
        synchronized (REGISTRO_LOCK) {
            long ahora = Instant.now().getEpochSecond() / 60;
            if (ahora != registroVentana) { registroVentana = ahora; registroCuenta = 0; }
            return ++registroCuenta <= 20;
        }
    }

    // ---- Comprobación de salud (para monitores externos que evitan que Render se duerma) ----
    private static final CacheSalud SALUD_DB = new CacheSalud(() -> {
        try (java.sql.Connection c = ConexionBD.obtener(); java.sql.Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) { return false; }
    }, 30);

    /** /health responde "ok" sin tocar sesiones; con ?db=1 comprueba también MySQL (503 si no responde). */
    private static void health(HttpExchange x) throws IOException {
        String query = x.getRequestURI().getRawQuery();
        boolean conBase = query != null && query.contains("db=1");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        if (conBase && !SALUD_DB.estaSano()) { send(x, 503, "text/plain; charset=utf-8", "base de datos no disponible"); return; }
        send(x, 200, "text/plain; charset=utf-8", conBase ? "ok db" : "ok");
    }

    // ---- Límite de intentos fallidos de login por IP ----
    private static final boolean TRUST_PROXY = "true".equalsIgnoreCase(env("SIGE_WEB_TRUST_PROXY", "false"));
    private static final LimiteIntentos LOGIN_POR_IP = new LimiteIntentos(
            positivo(env("SIGE_LOGIN_MAX_FALLOS_IP", "20"), 20), 600);

    private static int positivo(String texto, int porDefecto) {
        try { int n = Integer.parseInt(texto.trim()); return n < 1 ? porDefecto : n; }
        catch (NumberFormatException e) { return porDefecto; }
    }

    private static String clientIp(HttpExchange x) {
        String remota = x.getRemoteAddress() == null ? null : x.getRemoteAddress().getAddress().getHostAddress();
        return LimiteIntentos.ipCliente(remota, x.getRequestHeaders().getFirst("X-Forwarded-For"),
                x.getRequestHeaders().getFirst("CF-Connecting-IP"), TRUST_PROXY);
    }

    private static boolean ipBloqueada(HttpExchange x, WebSession s) throws IOException {
        String ip = clientIp(x);
        long espera = LOGIN_POR_IP.segundosRestantes(ip);
        if (espera <= 0) return false;
        accessLog(null, "(ip " + ip + ")", "BLOQUEADO", "Demasiados intentos desde la IP");
        page(x, s, home(s, "Demasiados intentos fallidos desde tu conexión. Inténtalo de nuevo en " + ((espera + 59) / 60) + " minuto(s)."));
        return true;
    }

    private static final String AUTH_BRAND = "<aside class='auth-brand'>"
        + "<div class='auth-brand-top'><img src='/logo.svg' alt='' width='52' height='52'><div><strong>SIGE</strong><span>Sistema de Gestión Educativa</span></div></div>"
        + "<div class='auth-brand-body'><img class='auth-hero' src='/login-hero.svg' alt='Boleta de calificaciones con gráficos y birrete' width='440' height='300'><h2>Tu información académica, en un solo lugar</h2>"
        + "<ul class='auth-features'>"
        + "<li><b>Calificaciones y boletas</b></li>"
        + "<li><b>Asistencia y aula virtual</b></li>"
        + "<li><b>Acceso protegido con verificación en dos pasos</b></li></ul></div>"
        + "<div class='auth-brand-foot'>UNEV · Programación II</div></aside>";

    private static String registroPage(WebSession s, String error, Map<String, String> v) {
        String rol = v.getOrDefault("rol", "ESTUDIANTE");
        return shell("Crear registro", (error == null ? "" : alert(error))
            + "<div class='auth-split'>" + AUTH_BRAND + "<section class='auth-card auth-panel'><div class='eyebrow'>NUEVA CUENTA</div><h1>Crear registro</h1>"
            + "<p>Completa tus datos para registrarte como estudiante o padre/tutor.</p>"
            + "<form method='post' action='/registro'>" + hiddenCsrf(s)
            + "<label>Tipo de cuenta<select name='rol' required>"
            + "<option value='ESTUDIANTE'" + ("ESTUDIANTE".equals(rol) ? " selected" : "") + ">Estudiante</option>"
            + "<option value='PADRE'" + ("PADRE".equals(rol) ? " selected" : "") + ">Padre / tutor</option></select></label>"
            + "<label>Nombre<input name='nombre' maxlength='50' required value='" + esc(v.getOrDefault("nombre", "")) + "'></label>"
            + "<label>Apellido<input name='apellido' maxlength='50' required value='" + esc(v.getOrDefault("apellido", "")) + "'></label>"
            + "<label>Correo electrónico<input name='correo' type='email' maxlength='100' autocomplete='username' required value='" + esc(v.getOrDefault("correo", "")) + "'></label>"
            + "<label>Contraseña<input name='contrasena' type='password' autocomplete='new-password' required></label>"
            + "<label>Confirmar contraseña<input name='confirmar' type='password' autocomplete='new-password' required></label>"
            + "<p class='hint'>" + esc(PoliticaContrasena.REQUISITOS) + "</p>"
            + "<fieldset class='reg-extra'><legend>Solo estudiantes</legend>"
            + "<label>Matrícula<input name='matricula' maxlength='20' placeholder='M-2026-001' value='" + esc(v.getOrDefault("matricula", "")) + "'></label>"
            + "<label>Grado<input name='grado' maxlength='30' placeholder='8vo' value='" + esc(v.getOrDefault("grado", "")) + "'></label></fieldset>"
            + "<fieldset class='reg-extra'><legend>Solo padres / tutores (opcional)</legend>"
            + "<label>Teléfono<input name='telefono' maxlength='20' placeholder='809-000-0000' value='" + esc(v.getOrDefault("telefono", "")) + "'></label>"
            + "<label>Ocupación<input name='ocupacion' maxlength='80' value='" + esc(v.getOrDefault("ocupacion", "")) + "'></label></fieldset>"
            + "<button class='primary' type='submit'>Crear cuenta</button></form>"
            + "<p class='hint'>¿Ya tienes cuenta? <a class='text-link inline-link' href='/'>Iniciar sesión</a></p></section></div>", false, "");
    }

    private static void registrar(HttpExchange x, WebSession s, Map<String, String> f) throws IOException {
        if (s.user != null) { redirect(x, "/"); return; }
        String rolTxt = f.getOrDefault("rol", "").trim();
        String nombre = f.getOrDefault("nombre", "").trim();
        String apellido = f.getOrDefault("apellido", "").trim();
        String correo = f.getOrDefault("correo", "").trim().toLowerCase(java.util.Locale.ROOT);
        String clave = f.getOrDefault("contrasena", "");
        String confirmar = f.getOrDefault("confirmar", "");
        String matricula = f.getOrDefault("matricula", "").trim();
        String grado = f.getOrDefault("grado", "").trim();
        String telefono = f.getOrDefault("telefono", "").trim();
        String ocupacion = f.getOrDefault("ocupacion", "").trim();
        Map<String, String> v = new HashMap<>(f);   // se reenvía al formulario, nunca las contraseñas
        v.remove("contrasena"); v.remove("confirmar");
        String err = null;
        if (!registroPermitido()) err = "Demasiados registros en poco tiempo. Inténtalo en un minuto.";
        else if (!rolTxt.equals("ESTUDIANTE") && !rolTxt.equals("PADRE")) err = "Ese tipo de cuenta solo puede crearlo un administrador.";
        else if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || clave.isEmpty() || confirmar.isEmpty()) err = "Completa todos los campos obligatorios.";
        else if (nombre.length() > 80 || apellido.length() > 80 || correo.length() > 120) err = "Algún dato supera la longitud permitida.";
        else if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) err = "Ingresa un correo electrónico válido.";
        else if (!clave.equals(confirmar)) err = "Las contraseñas no coinciden.";
        else if (rolTxt.equals("ESTUDIANTE") && (matricula.isEmpty() || grado.isEmpty())) err = "Los estudiantes deben indicar matrícula y grado.";
        else err = PoliticaContrasena.validar(clave);
        if (err != null) { page(x, s, registroPage(s, err, v)); return; }
        try {
            if (USERS.existeCorreo(correo)) { page(x, s, registroPage(s, "Ese correo ya está registrado.", v)); return; }
            Usuario nuevo;
            if (rolTxt.equals("ESTUDIANTE")) {
                if (USERS.existeMatricula(matricula)) { page(x, s, registroPage(s, "Esa matrícula ya existe.", v)); return; }
                nuevo = new Estudiante(0, nombre, apellido, correo, null, matricula, grado, 0.0);
            } else {
                nuevo = new Padre(0, nombre, apellido, correo, null, 0,
                        telefono.isEmpty() ? null : telefono, ocupacion.isEmpty() ? null : ocupacion);
            }
            boolean needsApproval = UsuarioDAO.registroRequiereAprobacion();
            if (needsApproval) USERS.registrarSolicitud(nuevo, clave); else USERS.registrar(nuevo, clave);
            accessLog(nuevo.getId(), correo, "REGISTRO", (needsApproval ? "Solicitud " : "Cuenta ") + rolTxt + " desde la web");
            page(x, s, shell(needsApproval ? "Solicitud enviada" : "Registro completado", "<section class='auth-card'><div class='success' role='status'>" + (needsApproval ? "Tu solicitud de registro fue enviada." : "Tu cuenta se creó correctamente.") + "</div>"
                + "<h1>¡Listo, " + esc(nombre) + "!</h1><p>" + (needsApproval ? "La administración revisará tu solicitud. Podrás iniciar sesión cuando la apruebe." : "Ya puedes iniciar sesión con tu correo y contraseña.") + "</p>"
                + "<a class='primary link-button' href='/'>Ir a iniciar sesión</a></section>", false, ""));
        } catch (SQLIntegrityConstraintViolationException e) {
            page(x, s, registroPage(s, "Ya existe un registro con esos datos (correo o matrícula).", v));
        } catch (SQLException e) {
            page(x, s, registroPage(s, "No se pudo guardar el registro. Inténtalo más tarde.", v));
        }
    }

    private static void login(HttpExchange x, WebSession s, Map<String, String> f) throws IOException {
        String correo = f.getOrDefault("correo", "").trim();
        String password = f.getOrDefault("contrasena", "");
        if (correo.isBlank() || password.isBlank()) { page(x, s, home(s, "Completa el correo y la contraseña.")); return; }
        if (ipBloqueada(x, s)) return;
        try {
            Usuario user = USERS.buscarPorCorreo(correo).orElse(null);
            if (user == null || !user.autenticar(correo, password)) {
                LOGIN_POR_IP.registrarFallo(clientIp(x));
                accessLog(user == null ? null : user.getId(), correo, "FALLIDO", "Credenciales incorrectas");
                page(x, s, home(s, "Correo o contraseña incorrectos.")); return;
            }
            if (!user.isActivo()) { page(x, s, home(s, isPendingApproval(user.getId()) ? "Tu cuenta está pendiente de aprobación por la administración." : "La cuenta está desactivada. Contacta al administrador.")); return; }
            Autenticacion2FADAO.Estado state = MFA.obtenerEstado(user.getId());
            if (state.bloqueado()) { page(x, s, home(s, "El segundo factor está bloqueado temporalmente. Inténtalo más tarde.")); return; }
            rotateCookie(x, s);
            s.pending = user;
            s.passwordWasVerified = true;
            s.activeMfa = state.activo();
            if (state.activo()) {
                s.secret = TOTP.descifrar(state.secretoCifrado(), requireEncryptionKey());
            } else {
                String secret = state.secretoCifrado();
                if (secret == null || secret.isBlank()) secret = TOTP.cifrar(TOTP.nuevoSecreto(), requireEncryptionKey());
                secret = MFA.guardarSecretoPendienteSiFalta(user.getId(), secret);
                s.secret = TOTP.descifrar(secret, requireEncryptionKey());
            }
            persistSession(s);
            redirect(x, "/2fa");
        } catch (SQLException | Configuracion2FAException e) {
            page(x, s, home(s, e.getMessage() == null ? "No se pudo iniciar sesión." : e.getMessage()));
        }
    }

    private static void verifyMfa(HttpExchange x, WebSession s, Map<String, String> f) throws IOException {
        if (s.pending == null || !s.passwordWasVerified) { redirect(x, "/"); return; }
        try {
            if (TOTP.verificar(s.secret, f.get("codigo"))) {
                MFA.registrarExito(s.pending.getId(), !s.activeMfa);
                accessLog(s.pending.getId(), s.pending.getCorreo(), "EXITOSO", "Inicio web correcto");
                if (s.pending.isDebeCambiarContrasena()) { persistSession(s); redirect(x, "/password"); return; }
                finishLogin(x, s);
            } else {
                LOGIN_POR_IP.registrarFallo(clientIp(x));
                Autenticacion2FADAO.Intento attempt = MFA.registrarIntentoFallido(s.pending.getId());
                accessLog(s.pending.getId(), s.pending.getCorreo(), "FALLIDO", "Código 2FA web incorrecto");
                page(x, s, mfaPage(s, attempt.bloqueado()
                        ? "Demasiados intentos. El acceso quedó bloqueado por 15 minutos."
                        : "Código incorrecto. Intentos restantes: " + attempt.restantes() + "."));
            }
        } catch (Exception e) {
            page(x, s, mfaPage(s, "No se pudo verificar el código. Comprueba la clave 2FA del servidor."));
        }
    }

    private static void changePassword(HttpExchange x, WebSession s, Map<String, String> f) throws IOException {
        Usuario target = s.pending != null ? s.pending : s.user;
        if (target == null) { redirect(x, "/"); return; }
        String current = f.getOrDefault("actual", "");
        String next = f.getOrDefault("nueva", "");
        String confirm = f.getOrDefault("confirmar", "");
        if (!next.equals(confirm)) { page(x, s, passwordPage(s, "Las contraseñas nuevas no coinciden.")); return; }
        try {
            USERS.cambiarContrasena(target.getId(), current, next);
            if (s.pending != null) {
                s.pending.setDebeCambiarContrasena(false);
                finishLogin(x, s);
            } else {
                redirect(x, "/perfil?contrasena=actualizada");
            }
        } catch (SQLException | PermisoDenegadoException e) {
            page(x, s, passwordPage(s, e.getMessage()));
        }
    }

    private static void finishLogin(HttpExchange x, WebSession s) throws SQLException, IOException {
        s.user = s.pending;
        s.pending = null;
        s.secret = null;
        s.passwordWasVerified = false;
        s.login = new SesionDAO().iniciar(s.user.getId(), clientIp(x));
        persistSession(s);
        redirect(x, "/");
    }

    private static void logout(HttpExchange x, WebSession s) throws SQLException, IOException {
        if (s.login != null) new SesionDAO().cerrar(s.login.getIdSesion());
        SESSIONS.remove(s.id);
        try { SESIONES_WEB.eliminar(s.id); } catch (SQLException ignored) { /* caduca sola */ }
        clearCookie(x);
        redirect(x, "/");
    }

    private static void shutdown(HttpExchange x,WebSession s)throws IOException{
        boolean local=x.getRemoteAddress()!=null&&x.getRemoteAddress().getAddress().isLoopbackAddress();
        if(!local||s.user==null||s.user.getRol()==null||!"ADMIN".equals(s.user.getRol().name())){
            send(x,403,"text/plain; charset=utf-8","Solo un administrador desde el servidor puede detener SIGE Web.");return;
        }
        send(x,200,"text/html; charset=utf-8","<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width'><title>SIGE detenido</title><body style='font:16px Segoe UI,Arial;padding:3rem;max-width:620px;margin:auto'><h1>SIGE Web se está deteniendo</h1><p>El servidor cerrará las sesiones abiertas. Ya puedes cerrar esta pestaña.</p></body></html>");
        Thread stopper=new Thread(()->{
            try{Thread.sleep(400);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
            for(WebSession active:SESSIONS.values())if(active.login!=null){
                try{new SesionDAO().cerrar(active.login.getIdSesion());}catch(SQLException e){System.err.println("No se pudo cerrar la sesión SIGE: "+e.getMessage());}
            }
            if(SERVER!=null)SERVER.stop(1);
            if(EXECUTOR!=null)EXECUTOR.shutdownNow();
            SESSIONS.clear();
            try{SESIONES_WEB.eliminarTodas();}catch(SQLException ignored){}
        },"sige-web-shutdown");
        stopper.setDaemon(true);stopper.start();
    }

    private static String home(WebSession s, String error) {
        if (s.user == null) return shell("Iniciar sesión", (error == null ? "" : alert(error))
                + "<div class='auth-split'>" + AUTH_BRAND + "<section class='auth-card auth-panel'><div class='eyebrow'>ACCESO AL SISTEMA</div><h1>Iniciar sesión</h1>"
                + "<p>Ingresa con tu correo y contraseña para continuar.</p><form method='post' action='/login'>"
                + hiddenCsrf(s) + "<label>Correo electrónico<input name='correo' type='email' autocomplete='username' required></label>"
                + "<label>Contraseña<input name='contrasena' type='password' autocomplete='current-password' required></label>"
                + "<button class='primary' type='submit'>Iniciar sesión</button></form><p class='hint'>¿No tienes cuenta? <a class='text-link inline-link' href='/registro'>Crear registro</a></p><p class='hint'>Las cuentas de profesor las crea la administración.</p></section></div>", false, "");
        try {
            DashboardDAO.Datos data = new DashboardDAO().cargar(s.user.getRol(), s.user.getId());
            DashboardDAO.Indicadores i = data.indicadores();
            String metrics = metricsFor(s.user, i);
            StringBuilder bars = new StringBuilder();
            double max = data.porAsignatura().stream().mapToDouble(DashboardDAO.DatoGrafico::valor).max().orElse(1);
            for (var d : data.porAsignatura()) bars.append("<div class='bar-row'><span>").append(esc(d.etiqueta()))
                    .append("</span><div class='bar-track'><i style='width:").append(Math.max(3, (int)(d.valor()/max*100)))
                    .append("%'></i></div><b>").append((long)d.valor()).append("</b></div>");
            if (bars.isEmpty()) bars.append("<p class='empty'>Todavía no hay inscripciones para mostrar.</p>");
            StringBuilder states = new StringBuilder();
            double stateTotal = data.porEstado().stream().mapToDouble(DashboardDAO.DatoGrafico::valor).sum();
            double approvedTotal = data.porEstado().stream()
                    .filter(d -> d.etiqueta() != null && d.etiqueta().toLowerCase(java.util.Locale.ROOT).contains("aprob"))
                    .mapToDouble(DashboardDAO.DatoGrafico::valor).sum();
            int approvedPct = stateTotal == 0 ? 0 : (int) Math.round(approvedTotal / stateTotal * 100);
            for (var d : data.porEstado()) {
                String normalized=d.etiqueta()==null?"":d.etiqueta().toLowerCase(java.util.Locale.ROOT);
                String tone=normalized.contains("aprob")?"approved":normalized.contains("reprob")?"failed":"in-progress";
                int percent=stateTotal==0?0:(int)Math.round(d.valor()/stateTotal*100);
                states.append("<div class='state-row'><div class='state-title'><span class='state-dot ").append(tone)
                        .append("'></span><b>").append(esc(d.etiqueta())).append("</b><strong>").append((long)d.valor())
                        .append("</strong></div><div class='state-track'><i class='").append(tone).append("' style='width:")
                        .append(percent).append("%'></i></div><small>").append(percent).append("% de las materias</small></div>");
            }
            if(states.isEmpty())states.append("<p class='empty'>Todavía no hay estados de materias para mostrar.</p>");
            StringBuilder rows = new StringBuilder();
            for (var row : data.recientes()) rows.append("<tr><td>").append(esc(row.fecha())).append("</td><td>")
                    .append(esc(row.estudiante())).append("</td><td>").append(esc(row.asignatura()))
                    .append("</td><td>").append(esc(row.periodo())).append("</td></tr>");
            if (rows.isEmpty()) rows.append("<tr><td colspan='4' class='empty'>No hay actividad académica reciente.</td></tr>");
            String body = "<section class='hero'><div class='hero-text'><div class='eyebrow'>PANEL PRINCIPAL</div><h1>Resumen académico</h1>"
                    + "<p>Hola, "+esc(s.user.getNombreCompleto())+". Estos son los indicadores actualizados de tu cuenta.</p><span class='role-badge'>"+esc(s.user.getRol().name())+"</span>"+pendingNotice(s.user)+"</div>"
                    + heroArt(approvedPct, stateTotal > 0) + "</section>"
                    + "<section class='metrics'>"+metrics+"</section><section class='cards-grid'><article class='card chart-card'><h2>Inscripciones por asignatura</h2>"
                    +bars+"</article><article class='card chart-card'><h2>Estado de las materias</h2>"+states+"</article><article class='card'><h2>Tu cuenta</h2><div class='profile-line'><span>Nombre</span><b>"+esc(s.user.getNombreCompleto())
                    +"</b></div><div class='profile-line'><span>Correo</span><b>"+esc(s.user.getCorreo())+"</b></div><div class='profile-line'><span>Rol</span><b>"
                    +esc(s.user.getRol().name())+"</b></div></article></section><section class='card table-card'><h2>Actividad académica reciente</h2>"
                    +"<div class='table-wrap'><table><thead><tr><th>Fecha</th><th>Estudiante</th><th>Asignatura</th><th>Período</th></tr></thead><tbody>"
                    +rows+"</tbody></table></div></section>";
            return shell("Resumen académico", body, true, s.user.getNombreCompleto());
        } catch (SQLException e) {
            return shell("Resumen académico", alert("No se pudieron cargar los datos. Verifica la conexión con MySQL."), true, s.user.getNombreCompleto());
        }
    }

    private static String mfaPage(WebSession s, String error) {
        if (s.pending == null) return shell("Verificación", errorPage("No hay un inicio de sesión pendiente."), false, "");
        String setup = s.activeMfa ? "" : "<p>Abre tu app autenticadora y escanea este código QR. También puedes ingresar la clave manualmente:</p>"
                + "<img class='qr' alt='Código QR de configuración 2FA' src='"+qrData(TOTP.uri(s.pending.getCorreo(), s.secret))+"'>"
                + "<code class='secret'>"+esc(s.secret)+"</code>";
        return shell("Verificación de seguridad", (error == null ? "" : alert(error))+"<section class='auth-card'>"
                +"<div class='eyebrow'>SEGURIDAD DE LA CUENTA</div><h1>"+(s.activeMfa?"Verifica tu identidad":"Configura tu autenticación")+"</h1>"
                +setup+"<form method='post' action='/verify-2fa'>"+hiddenCsrf(s)+"<label>Código de 6 dígitos<input name='codigo' inputmode='numeric' pattern='[0-9]{6}' maxlength='6' autocomplete='one-time-code' required></label>"
                +"<button class='primary' type='submit'>Continuar</button></form></section>", false, "");
    }

    private static String passwordPage(WebSession s, String error) {
        boolean initial = s.pending != null;
        if (!initial && s.user == null) return shell("Cambio de contraseña", errorPage("Inicia sesión para continuar."), false, "");
        return shell("Cambio de contraseña", (error == null ? "" : alert(error))+"<section class='auth-card'><div class='eyebrow'>SEGURIDAD</div><h1>Cambia tu contraseña</h1>"
                +(initial?"<p>Debes actualizar la contraseña inicial para continuar.</p>":"<p>Confirma tu contraseña actual para guardar una nueva.</p>")+"<form method='post' action='/change-password'>"+hiddenCsrf(s)
                +"<label>Contraseña actual<input name='actual' type='password' autocomplete='current-password' required></label>"
                +"<label>Nueva contraseña<input name='nueva' type='password' minlength='8' autocomplete='new-password' required></label>"
                +"<label>Confirma la nueva contraseña<input name='confirmar' type='password' minlength='8' autocomplete='new-password' required></label>"
                +"<p class='hint'>"+esc(PoliticaContrasena.REQUISITOS)+"</p>"+"<button class='primary' type='submit'>"+(initial?"Actualizar y entrar":"Guardar contraseña")+"</button></form></section>", initial, initial?"":s.user.getNombreCompleto());
    }

    private static String profilePage(WebSession s, String query) {
        Usuario u=s.user;
        StringBuilder fields=new StringBuilder();
        fields.append(profileField("Nombre",u.getNombreCompleto())).append(profileField("Correo",u.getCorreo()))
                .append(profileField("Rol",u.getRol()==null?"":u.getRol().name()));
        if(u instanceof Estudiante e) fields.append(profileField("Matrícula",e.getMatricula()))
                .append(profileField("Grado",e.getGrado())).append(profileField("Promedio académico",
                        String.format(java.util.Locale.forLanguageTag("es-DO"),"%.2f",e.getPromedioAcademico())));
        else if(u instanceof Profesor p) fields.append(profileField("Código de profesor",p.getCodigoProfesor()))
                .append(profileField("Especialidad",p.getEspecialidad()));
        else if(u instanceof Padre p) fields.append(profileField("Teléfono de contacto",p.getTelefonoContacto()))
                .append(profileField("Ocupación",p.getOcupacion()));
        String notice="actualizada".equals(query)?"<div class='success' role='status'>La contraseña se actualizó correctamente.</div>":"";
        String body="<div class='page-heading'><div><div class='eyebrow'>CUENTA</div><h1>Mi perfil</h1><p>Información asociada a tu cuenta SIGE.</p></div></div>"
                +notice+"<section class='card profile-card'><div class='profile-fields'>"+fields+"</div><a class='primary link-button' href='/password'>Cambiar contraseña</a>"+(u.getRol()==Rol.PROFESOR||u.getRol()==Rol.ESTUDIANTE?" <a class='secondary-link' href='/aula'>Abrir aula y tareas</a>":"")+"</section>";
        return shell("Mi perfil",body,true,u.getNombreCompleto());
    }

    private static String boletaPage(WebSession s,String query){
        if(s.user==null)return shell("Boleta",errorPage("Inicia sesión para consultar la boleta."),false,"");
        try{
            int studentId=s.user.getId();String studentName=s.user.getNombreCompleto();List<Estudiante> children=List.of();
            if(s.user.getRol()==Rol.PADRE){
                children=new VinculacionDAO().listarHijos(s.user.getId());
                if(children.isEmpty())return shell("Boleta",errorPage("Tu cuenta todavía no tiene estudiantes vinculados."),true,s.user.getNombreCompleto());
                Integer chosen=parseStudentId(query);
                if(chosen==null)return shell("Boleta de mis hijos",childPicker(children,"/boleta","Selecciona un hijo para consultar su boleta."),true,s.user.getNombreCompleto());
                Estudiante linked=children.stream().filter(e->e.getId()==chosen).findFirst().orElse(null);
                if(linked==null)return shell("Boleta",errorPage("Ese estudiante no está vinculado a tu cuenta."),true,s.user.getNombreCompleto());
                studentId=linked.getId();studentName=linked.getNombreCompleto();
            }else if(s.user.getRol()!=Rol.ESTUDIANTE){
                return shell("Boleta",errorPage("La consulta web de boletas está habilitada para estudiantes y padres/tutores."),true,s.user.getNombreCompleto());
            }
            List<LineaBoleta> lines=s.user.getRol()==Rol.PADRE
                    ?new BoletaDAO().listarParaPadre(s.user.getId(),studentId)
                    :new BoletaDAO().listarParaEstudiante(studentId);
            long evaluated=lines.stream().filter(l->l.notaFinal()>0).count();
            double average=lines.stream().filter(l->l.notaFinal()>0).mapToDouble(LineaBoleta::notaFinal).average().orElse(0);
            StringBuilder rows=new StringBuilder();
            for(LineaBoleta line:lines)rows.append("<tr><td>").append(esc(line.periodo())).append("</td><td><b>")
                    .append(esc(line.codigoAsignatura())).append("</b><br>").append(esc(line.asignatura())).append("</td><td>")
                    .append(formatGrade(line.parcial1())).append("</td><td>").append(formatGrade(line.parcial2())).append("</td><td>")
                    .append(formatGrade(line.examenFinal())).append("</td><td><b>").append(formatGrade(line.notaFinal()))
                    .append("</b></td><td>").append(esc(line.literal())).append("</td><td><span class='state-pill'>")
                    .append(esc(line.estado())).append("</span></td></tr>");
            if(rows.isEmpty())rows.append("<tr><td colspan='8' class='empty'>No hay calificaciones registradas para mostrar.</td></tr>");
            String picker=s.user.getRol()==Rol.PADRE?childPicker(children,"/boleta","Estudiante: "+studentName):"";
            String body="<div class='page-heading'><div><div class='eyebrow'>CONSULTA ACADÉMICA</div><h1>Boleta de calificaciones</h1>"
                    +"<p>"+esc(studentName)+" · notas y estado por período.</p></div><span class='role-badge'>"+evaluated+" evaluadas</span></div>"
                    +picker+"<section class='metrics grade-summary'>"+metric("Materias",lines.size())+metric("Evaluadas",evaluated)
                    +metric("Promedio",String.format(java.util.Locale.ROOT,"%.2f",average))+"</section><section class='card table-card'>"
                    +"<div class='table-wrap'><table class='grade-table'><thead><tr><th>Período</th><th>Asignatura</th><th>P1</th><th>P2</th><th>Examen</th><th>Final</th><th>Literal</th><th>Estado</th></tr></thead><tbody>"
                    +rows+"</tbody></table></div></section>";
            return shell("Boleta de calificaciones",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Boleta",alert("No se pudo consultar la boleta. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static String childPicker(List<Estudiante> children,String action,String heading){
        StringBuilder options=new StringBuilder();for(Estudiante e:children)options.append("<option value='").append(e.getId()).append("'>")
                .append(esc(e.getNombreCompleto())).append(" · ").append(esc(e.getMatricula())).append("</option>");
        return "<section class='card child-picker'><form method='get' action='"+action+"'><label>"+esc(heading)
                +"<select name='estudiante' required><option value=''>Selecciona...</option>"+options+"</select></label><button class='primary' type='submit'>Consultar</button></form></section>";
    }

    private static Integer parseStudentId(String query){
        if(query==null)return null;for(String part:query.split("&")){String[] kv=part.split("=",2);if(kv.length==2&&kv[0].equals("estudiante"))try{return Integer.valueOf(kv[1]);}catch(NumberFormatException ignored){return -1;}}return null;
    }

    private static String formatGrade(double grade){return String.format(java.util.Locale.ROOT,"%.1f",grade);}

    private static String subjectsPage(WebSession s,String query){
        if(s.user==null)return shell("Mis materias",errorPage("Inicia sesión para consultar las materias."),false,"");
        try{
            int studentId=s.user.getId();String studentName=s.user.getNombreCompleto();List<Estudiante> children=List.of();
            if(s.user.getRol()==Rol.PADRE){
                children=new VinculacionDAO().listarHijos(s.user.getId());
                if(children.isEmpty())return shell("Materias",errorPage("Tu cuenta todavía no tiene estudiantes vinculados."),true,s.user.getNombreCompleto());
                Integer chosen=parseStudentId(query);
                if(chosen==null)return shell("Materias de mis hijos",childPicker(children,"/materias","Selecciona un hijo para consultar sus materias."),true,s.user.getNombreCompleto());
                Estudiante linked=children.stream().filter(e->e.getId()==chosen).findFirst().orElse(null);
                if(linked==null)return shell("Materias",errorPage("Ese estudiante no está vinculado a tu cuenta."),true,s.user.getNombreCompleto());
                studentId=linked.getId();studentName=linked.getNombreCompleto();
            }else if(s.user.getRol()!=Rol.ESTUDIANTE){
                return shell("Materias",errorPage("La consulta web de materias está habilitada para estudiantes y padres/tutores."),true,s.user.getNombreCompleto());
            }
            InscripcionDAO dao=new InscripcionDAO();
            var enrollments=s.user.getRol()==Rol.PADRE
                    ?dao.listarParaPadre(s.user.getId(),studentId):dao.listarPorEstudiante(studentId);
            int count=enrollments.stream().mapToInt(i->i.getDetalles().size()).sum();
            int credits=enrollments.stream().flatMap(i->i.getDetalles().stream())
                    .mapToInt(d->d.getSeccion().getAsignatura().getCreditos()).sum();
            StringBuilder rows=new StringBuilder();
            for(var enrollment:enrollments)for(var detail:enrollment.getDetalles()){
                var section=detail.getSeccion();var subject=section.getAsignatura();var teacher=section.getProfesor();
                rows.append("<tr><td>").append(esc(enrollment.getPeriodoLectivo())).append("</td><td><b>")
                        .append(esc(subject.getCodigo())).append("</b><br>").append(esc(subject.getNombre()))
                        .append("</td><td>").append(esc(subject.getAreaCurricular()==null?"Área pendiente":subject.getAreaCurricular()))
                        .append("</td><td>").append(esc(section.getGrado()==null?"Grado pendiente":section.getGrado().nombre()))
                        .append("</td><td>").append(subject.getCreditos()).append("</td><td>")
                        .append(esc(teacher==null?"Sin asignar":teacher.getNombreCompleto())).append("</td><td>")
                        .append(esc(section.getHorario())).append("</td><td>").append(esc(section.getAula()))
                        .append("</td><td><span class='state-pill'>").append(esc(detail.getEstadoMateria())).append("</span></td></tr>");
            }
            if(rows.isEmpty())rows.append("<tr><td colspan='9' class='empty'>Aún no hay materias inscritas para mostrar.</td></tr>");
            String picker=s.user.getRol()==Rol.PADRE?childPicker(children,"/materias","Estudiante: "+studentName):"";
            String body="<div class='page-heading'><div><div class='eyebrow'>CONSULTA ACADÉMICA</div><h1>Mis materias</h1><p>Secciones y horarios de "+esc(studentName)+".</p></div></div>"
                    +picker+"<section class='metrics grade-summary'>"+metric("Materias inscritas",count)+metric("Períodos",enrollments.size())
                    +metric("Créditos",credits)+"</section><section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Período</th><th>Asignatura</th><th>Área curricular</th><th>Grado</th><th>Créditos</th><th>Profesor</th><th>Horario</th><th>Aula</th><th>Estado</th></tr></thead><tbody>"
                    +rows+"</tbody></table></div></section>";
            return shell("Mis materias",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Mis materias",alert("No se pudieron consultar las materias. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static String enrollmentPage(WebSession s,String query,String message){
        if(s.user==null)return shell("Inscripciones",errorPage("Inicia sesión para gestionar tus inscripciones."),false,"");
        if(s.user.getRol()!=Rol.ESTUDIANTE)return shell("Inscripciones",errorPage("Solo los estudiantes pueden inscribirse desde esta página."),true,s.user.getNombreCompleto());
        try{
            if(message==null&&query!=null&&query.contains("resultado=ok"))message="La inscripción se guardó correctamente.";
            List<Seccion> sections=new SeccionDAO().listarTodas();
            StringBuilder rows=new StringBuilder();int available=0;
            for(Seccion section:sections){
                if(section.getInscritos()>=section.getCupoMaximo())continue;
                available++;
                var subject=section.getAsignatura();var teacher=section.getProfesor();
                rows.append("<tr><td><b>").append(esc(subject.getCodigo())).append("</b><br>").append(esc(subject.getNombre()))
                        .append("</td><td>").append(esc(subject.getAreaCurricular()==null?"Área pendiente":subject.getAreaCurricular()))
                        .append("</td><td>").append(esc(section.getGrado()==null?"Grado pendiente":section.getGrado().nombre()))
                        .append("</td><td>").append(subject.getCreditos()).append("</td><td>")
                        .append(esc(teacher==null?"Sin asignar":teacher.getNombreCompleto())).append("</td><td>")
                        .append(esc(section.getHorario())).append("</td><td>").append(esc(section.getAula())).append("</td><td>")
                        .append(section.getInscritos()).append(" / ").append(section.getCupoMaximo()).append("</td><td>")
                        .append("<form class='enroll-form' method='post' action='/inscribir'>").append(hiddenCsrf(s))
                        .append("<input type='hidden' name='id_seccion' value='").append(section.getId()).append("'>")
                        .append("<label>Período lectivo<input name='periodo' maxlength='20' placeholder='Ej. 2026-2' required></label>")
                        .append("<button class='primary' type='submit'>Inscribirme</button></form></td></tr>");
            }
            if(rows.isEmpty())rows.append("<tr><td colspan='9' class='empty'>No hay secciones con cupo disponible en este momento.</td></tr>");
            String notice=message==null?"":"<div class='success' role='status'>"+esc(message)+"</div>";
            String body="<div class='page-heading'><div><div class='eyebrow'>INSCRIPCIÓN ACADÉMICA</div><h1>Secciones disponibles</h1><p>Elige una sección con espacio e indica el período lectivo que corresponde.</p></div><span class='role-badge'>"+available+" disponibles</span></div>"
                    +notice+"<section class='card table-card'><div class='table-wrap'><table class='grade-table enrollment-table'><thead><tr><th>Asignatura</th><th>Área curricular</th><th>Grado</th><th>Créditos</th><th>Profesor</th><th>Horario</th><th>Aula</th><th>Cupo</th><th>Inscripción</th></tr></thead><tbody>"
                    +rows+"</tbody></table></div></section><p class='hint'>El sistema valida cupo y materias repetidas al guardar la inscripción.</p>";
            return shell("Secciones disponibles",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Inscripciones",alert("No se pudieron cargar las secciones. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static String gradesPage(WebSession s,String query,String message){
        if(s.user==null)return shell("Calificaciones",errorPage("Inicia sesión para consultar calificaciones."),false,"");
        boolean admin=s.user.getRol()==Rol.ADMIN;
        if(!admin&&s.user.getRol()!=Rol.PROFESOR)return shell("Calificaciones",errorPage("Esta vista está disponible para profesores y administradores."),true,s.user.getNombreCompleto());
        try{
            List<Seccion> sections=admin?new SeccionDAO().listarTodas():new SeccionDAO().listarPorProfesor(s.user.getId());
            Integer selected=parseQueryInteger(query,"seccion");
            Seccion active=sections.stream().filter(sec->selected!=null&&sec.getId()==selected).findFirst().orElse(null);
            String notice=message==null&&query!=null&&query.contains("guardado=1")
                    ?"La calificación se guardó correctamente.":message;
            StringBuilder options=new StringBuilder("<option value=''>Selecciona una sección...</option>");
            for(Seccion sec:sections)options.append("<option value='").append(sec.getId()).append("'")
                    .append(active!=null&&active.getId()==sec.getId()?" selected":"").append(">")
                    .append(esc(sec.getAsignatura().getCodigo()+" · "+sec.getAsignatura().getNombre()+" · "+(sec.getGrado()==null?"Grado pendiente":sec.getGrado().nombre())+" · "+sec.getHorario()+" · Aula "+sec.getAula()))
                    .append("</option>");
            StringBuilder body=new StringBuilder("<div class='page-heading'><div><div class='eyebrow'>GESTIÓN ACADÉMICA</div><h1>Calificaciones</h1><p>Selecciona una de tus secciones para registrar o corregir notas.</p></div></div>");
            if(notice!=null)body.append("<div class='success' role='status'>").append(esc(notice)).append("</div>");
            body.append("<section class='card section-picker'><form method='get' action='/calificaciones'><label>Sección<select name='seccion' required>")
                    .append(options).append("</select></label><button class='primary' type='submit'>Consultar estudiantes</button></form></section>");
            if(selected!=null&&active==null)body.append(alert("La sección elegida no está asignada a tu cuenta."));
            if(active!=null){
                List<NotaEstudiante> students=new CalificacionDAO().listarPorSeccion(active.getId());
                body.append("<section class='card table-card'><h2>").append(esc(active.getAsignatura().getCodigo()+" · "+active.getAsignatura().getNombre()+" · "+(active.getGrado()==null?"Grado pendiente":active.getGrado().nombre())))
                        .append("</h2><p class='muted'>").append(esc(active.getHorario()+" · Aula "+active.getAula()+" · "+students.size()+" estudiante(s)"))
                        .append("</p><div class='table-wrap'><table class='grade-table teacher-grades'><thead><tr><th>Estudiante</th><th>Matrícula</th><th>Período</th><th>Parcial 1</th><th>Parcial 2</th><th>Examen</th><th>Final calculado</th><th>Estado</th><th>Guardar</th></tr></thead><tbody>");
                if(students.isEmpty())body.append("<tr><td colspan='9' class='empty'>Esta sección todavía no tiene estudiantes inscritos.</td></tr>");
                for(NotaEstudiante student:students){
                    Calificacion grade=student.calificacion();
                    String formId="grade-"+student.idDetalleInscripcion();
                    body.append("<tr><td>").append(esc(student.estudiante().getNombreCompleto())).append("</td><td>").append(esc(student.estudiante().getMatricula()))
                            .append("</td><td>").append(esc(student.periodo())).append("</td><td>").append(gradeInput("p1",grade.getNotaParcial1(),formId))
                            .append("</td><td>").append(gradeInput("p2",grade.getNotaParcial2(),formId)).append("</td><td>")
                            .append(gradeInput("examen",grade.getNotaExamenFinal(),formId)).append("</td><td><span class='grade-result'>")
                            .append(formatGrade(grade.getNotaFinal())).append(" · ").append(esc(grade.getCalificacionLiteral()))
                            .append("</span></td><td><span class='state-pill'>").append(esc(student.estadoMateria()))
                            .append("</span></td><td><form id='").append(formId).append("' class='grade-edit' method='post' action='/guardar-calificacion'>")
                            .append(hiddenCsrf(s)).append("<input type='hidden' name='detalle' value='").append(student.idDetalleInscripcion())
                            .append("'><input type='hidden' name='seccion' value='").append(active.getId())
                            .append("'><button class='primary' type='submit'>Guardar</button></form></td></tr>");
                }
                body.append("</tbody></table></div></section><p class='hint'>La nota final y la calificación literal se calculan automáticamente al guardar.</p>");
            }
            return shell("Calificaciones",body.toString(),true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Calificaciones",alert("No se pudieron cargar las calificaciones. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static String gradeInput(String name,double value,String formId){
        return "<label class='grade-input'><input aria-label='"+name.toUpperCase(java.util.Locale.ROOT)+"' form='"+formId+"' name='"+name+"' type='number' min='0' max='100' step='0.01' value='"+String.format(java.util.Locale.ROOT,"%.2f",value)+"' required></label>";
    }

    private static void saveGrade(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        boolean admin=s.user!=null&&s.user.getRol()==Rol.ADMIN;
        if(!admin&&(s.user==null||s.user.getRol()!=Rol.PROFESOR)){page(x,s,errorPage("Solo profesores y administradores pueden guardar calificaciones."));return;}
        int detailId,sectionId;double p1,p2,exam;
        try{
            detailId=Integer.parseInt(values.getOrDefault("detalle",""));sectionId=Integer.parseInt(values.getOrDefault("seccion",""));
            p1=parseGrade(values.get("p1"));p2=parseGrade(values.get("p2"));exam=parseGrade(values.get("examen"));
        }catch(IllegalArgumentException e){page(x,s,gradesPage(s,"seccion="+values.getOrDefault("seccion",""),"Las notas deben ser números entre 0 y 100."));return;}
        Calificacion grade=new Calificacion(0,detailId,p1,p2,exam,0,"");
        try{
            CalificacionDAO dao=new CalificacionDAO();
            if(admin)dao.guardarComoAdministrador(grade,s.user.getId());else dao.guardar(grade,s.user.getId());
            redirect(x,"/calificaciones?seccion="+sectionId+"&guardado=1");
        }catch(PermisoDenegadoException e){page(x,s,gradesPage(s,"seccion="+sectionId,e.getMessage()));}
        catch(SQLException e){page(x,s,gradesPage(s,"seccion="+sectionId,"No se pudieron guardar las notas. Verifica la conexión con MySQL."));}
    }

    private static double parseGrade(String value){
        double grade=Double.parseDouble(value==null?"":value.trim().replace(',','.'));
        if(!Double.isFinite(grade)||grade<0||grade>100)throw new IllegalArgumentException("nota fuera de rango");
        return grade;
    }

    private static String aulaPage(WebSession s,String query,String message){
        if(s.user==null)return shell("Aula y tareas",errorPage("Inicia sesión para continuar."),false,"");
        if(s.user.getRol()!=Rol.PROFESOR&&s.user.getRol()!=Rol.ESTUDIANTE)return shell("Aula y tareas",errorPage("El aula está disponible para profesores y estudiantes."),true,s.user.getNombreCompleto());
        try{
            AulaDAO dao=new AulaDAO();List<AulaDAO.Material> materials=s.user.getRol()==Rol.PROFESOR?dao.paraProfesor(s.user.getId()):dao.paraEstudiante(s.user.getId());
            String notice="";
            if(message!=null)notice=alert(message);
            else if(query!=null&&query.contains("publicado=1"))notice="<div class='success' role='status'>La publicación quedó disponible para la sección.</div>";
            else if(query!=null&&query.contains("entregado=1"))notice="<div class='success' role='status'>La entrega se subió correctamente. Puedes volver a subir una versión corregida.</div>";
            StringBuilder content=new StringBuilder();
            if(s.user.getRol()==Rol.PROFESOR){
                List<Seccion> sections=new SeccionDAO().listarPorProfesor(s.user.getId());StringBuilder options=new StringBuilder("<option value=''>Selecciona una sección...</option>");
                for(Seccion sec:sections)options.append("<option value='").append(sec.getId()).append("'>").append(esc(sec.getAsignatura().getNombre()+" · "+(sec.getGrado()==null?"Sin grado":sec.getGrado().nombre())+" · sección "+sec.getId())).append("</option>");
                if(sections.isEmpty())content.append("<section class='card'><p class='empty'>No tienes secciones asignadas. Solicita al administrador que te asigne una sección para publicar.</p></section>");
                else content.append("<section class='card aula-editor'><h2>Publicar una clase o tarea</h2><form class='aula-form' method='post' action='/aula-publicar' enctype='multipart/form-data'>").append(hiddenCsrf(s))
                        .append("<label>Sección<select name='seccion' required>").append(options).append("</select></label><div class='aula-fields'><label>Tipo<select name='tipo' required><option value='CLASE'>Clase o material</option><option value='TAREA'>Tarea</option></select></label><label>Título<input name='titulo' maxlength='120' required></label><label>Fecha límite (opcional)<input type='date' name='fecha_limite'></label></div><label>Instrucciones o descripción<textarea name='descripcion' maxlength='5000' rows='4'></textarea></label><label>Archivo adjunto (opcional)<input type='file' name='archivo'><small>Se acepta cualquier formato. Máximo 10 MB por archivo.</small></label><button class='primary' type='submit'>Publicar</button></form></section>");
            }
            if(materials.isEmpty())content.append("<section class='card'><h2>Publicaciones</h2><p class='empty'>Aún no hay clases ni tareas para mostrar.</p></section>");
            else{
                content.append("<section class='aula-list'><h2>").append(s.user.getRol()==Rol.PROFESOR?"Mis publicaciones y entregas":"Clases y tareas asignadas").append("</h2>");
                java.util.Set<Integer> rendered=new java.util.HashSet<>();
                for(AulaDAO.Material m:materials){
                    if(s.user.getRol()==Rol.PROFESOR&&!rendered.add(m.id()))continue;
                    content.append("<article class='card aula-card'><div class='aula-card-head'><div><span class='aula-type'>").append(esc(m.tipo().equals("TAREA")?"TAREA":"CLASE")).append("</span><h3>").append(esc(m.titulo())).append("</h3><p>").append(esc(m.asignatura())).append(" · ").append(esc(m.grado()==null?"Grado no indicado":m.grado())).append(" · Publicado ").append(esc(m.fechaPublicacion())).append("</p></div>");
                    if(m.tipo().equals("TAREA")&&m.fechaLimite()!=null)content.append("<span class='role-badge'>Entrega: ").append(esc(m.fechaLimite().toString())).append("</span>");
                    content.append("</div>");if(m.descripcion()!=null&&!m.descripcion().isBlank())content.append("<p class='aula-description'>").append(esc(m.descripcion()).replace("\n","<br>")).append("</p>");
                    if(m.storageKey()!=null)content.append("<p><a class='secondary-link' href='/archivo-aula?material=").append(m.id()).append("'>Descargar material: ").append(esc(m.archivoNombre())).append(" ( ").append(humanBytes(m.archivoBytes())).append(" )</a></p>");
                    if(s.user.getRol()==Rol.ESTUDIANTE&&m.tipo().equals("TAREA")){
                        if(m.entregaId()>0)content.append("<div class='success'>Entrega enviada: ").append(esc(m.entregaNombre())).append(" · ").append(esc(m.entregaFecha())).append(". Puedes reemplazarla con una nueva versión.</div>");
                        content.append("<form class='submit-work' method='post' action='/aula-entregar' enctype='multipart/form-data'>").append(hiddenCsrf(s)).append("<input type='hidden' name='material' value='").append(m.id()).append("'><label>Archivo de tu entrega<input type='file' name='archivo' required></label><small>Se acepta cualquier formato. Máximo 10 MB.</small><button class='primary' type='submit'>Subir entrega</button></form>");
                    }else if(s.user.getRol()==Rol.PROFESOR&&m.tipo().equals("TAREA")){
                        content.append("<div class='submissions'><b>Entregas recibidas</b>");
                        for(AulaDAO.Material submission:materials)if(submission.id()==m.id()&&submission.entregaId()>0)content.append("<div class='submission-row'><span><b>").append(esc(submission.entregaEstudiante())).append("</b> · ").append(esc(submission.entregaNombre())).append(" · ").append(esc(submission.entregaFecha())).append("</span><a class='secondary-link' href='/archivo-aula?entrega=").append(submission.entregaId()).append("'>Descargar</a></div>");
                        if(materials.stream().noneMatch(row->row.id()==m.id()&&row.entregaId()>0))content.append("<p class='empty'>Todavía no hay entregas.</p>");content.append("</div>");
                    }
                    content.append("</article>");
                }
                content.append("</section>");
            }
            String body="<div class='page-heading'><div><div class='eyebrow'>ESPACIO ACADÉMICO</div><h1>Aula y tareas</h1><p>Publica materiales de clase, consulta las tareas y gestiona las entregas de tus secciones.</p></div></div>"+notice+content;
            return shell("Aula y tareas",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Aula y tareas",alert("No se pudo cargar el aula. Verifica la conexión con MySQL y que se haya instalado sql/aula_virtual.sql."),true,s.user.getNombreCompleto());}
    }

    private static void publishAula(HttpExchange x,WebSession s,Map<String,String> f,Map<String,UploadedFile> files)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.PROFESOR){page(x,s,errorPage("Solo los profesores pueden publicar en el aula."));return;}
        int section;try{section=Integer.parseInt(f.getOrDefault("seccion",""));}catch(NumberFormatException e){page(x,s,aulaPage(s,null,"Selecciona una sección válida."));return;}
        String type=f.getOrDefault("tipo","").trim(),title=f.getOrDefault("titulo","").trim(),description=f.getOrDefault("descripcion","").trim();
        if(!type.equals("CLASE")&&!type.equals("TAREA")||title.isBlank()||title.length()>120||description.length()>5000){page(x,s,aulaPage(s,null,"Completa el tipo, el título y una descripción de hasta 5000 caracteres."));return;}
        java.time.LocalDate due=null;String date=f.getOrDefault("fecha_limite","").trim();try{if(!date.isBlank())due=java.time.LocalDate.parse(date);}catch(java.time.DateTimeException e){page(x,s,aulaPage(s,null,"La fecha límite no tiene un formato válido."));return;}
        UploadedFile file=files.get("archivo");String key=null,name=null,mime=null;long bytes=0;
        try{
            if(file!=null&&file.data().length>0){key=saveUpload(file.data());name=safeFileName(file.filename());mime=file.contentType();bytes=file.data().length;}
            new AulaDAO().publicar(s.user.getId(),section,type,title,description,due,key,name,mime,bytes);redirect(x,"/aula?publicado=1");
        }catch(SQLException|IOException e){if(key!=null)deleteUpload(key);page(x,s,aulaPage(s,null,"No se pudo publicar. Comprueba que el esquema aula_virtual.sql esté instalado y que la sección sea tuya."));}
    }

    private static void submitAula(HttpExchange x,WebSession s,Map<String,String> f,Map<String,UploadedFile> files)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ESTUDIANTE){page(x,s,errorPage("Solo los estudiantes pueden subir entregas."));return;}
        int material;try{material=Integer.parseInt(f.getOrDefault("material",""));}catch(NumberFormatException e){page(x,s,aulaPage(s,null,"La tarea no es válida."));return;}
        UploadedFile file=files.get("archivo");if(file==null||file.data().length==0){page(x,s,aulaPage(s,null,"Selecciona un archivo antes de subir la entrega."));return;}
        String key=null;try{key=saveUpload(file.data());new AulaDAO().entregar(s.user.getId(),material,key,safeFileName(file.filename()),file.contentType(),file.data().length);redirect(x,"/aula?entregado=1");}
        catch(SQLException|IOException e){if(key!=null)deleteUpload(key);page(x,s,aulaPage(s,null,"No se pudo subir la entrega. Verifica que estés inscrito en la sección de esta tarea y que aula_virtual.sql esté instalado."));}
    }

    private static void downloadAulaFile(HttpExchange x,WebSession s,String query)throws IOException{
        if(s.user==null||(s.user.getRol()!=Rol.PROFESOR&&s.user.getRol()!=Rol.ESTUDIANTE)){send(x,403,"text/plain; charset=utf-8","Acceso denegado");return;}
        Integer material=parseQueryInteger(query,"material"),submission=parseQueryInteger(query,"entrega");if((material==null)==(submission==null)){send(x,400,"text/plain; charset=utf-8","Solicitud no válida");return;}
        try{
            AulaDAO dao=new AulaDAO();AulaDAO.Archivo file=material!=null?dao.archivoMaterial(s.user.getId(),s.user.getRol(),material):dao.archivoEntrega(s.user.getId(),s.user.getRol(),submission);
            if(file==null){send(x,404,"text/plain; charset=utf-8","Archivo no encontrado");return;}
            byte[] data=dao.leerArchivo(file.storageKey());if(data==null)data=leerArchivoAntiguo(file.storageKey());if(data==null){send(x,404,"text/plain; charset=utf-8","Archivo no encontrado");return;}
            Headers h=x.getResponseHeaders();h.set("Cache-Control","no-store");h.set("X-Content-Type-Options","nosniff");h.set("Content-Disposition","attachment; filename=\"archivo-descargado\"; filename*=UTF-8''"+java.net.URLEncoder.encode(file.nombre(),StandardCharsets.UTF_8).replace("+","%20"));h.set("Content-Type","application/octet-stream");x.sendResponseHeaders(200,data.length);x.getResponseBody().write(data);
        }catch(SQLException|IOException e){send(x,500,"text/plain; charset=utf-8","No se pudo descargar el archivo");}
    }

    /** Guarda el archivo en MySQL (aula_archivos): el disco de los servicios gratuitos se borra al reiniciar. */
    private static String saveUpload(byte[] data)throws IOException,SQLException{if(data.length>MAX_UPLOAD_BYTES)throw new IOException("El archivo supera 10 MB.");return new AulaDAO().guardarArchivo(data);}
    /** Lee archivos subidos antes de usar MySQL (carpeta local data/uploads); null si no existe. */
    private static byte[] leerArchivoAntiguo(String key)throws IOException{Path root=uploadRoot();Path path=root.resolve(key).normalize();if(!root.equals(path.getParent())||!Files.isRegularFile(path))return null;return Files.readAllBytes(path);}
    private static void deleteUpload(String key){try{new AulaDAO().eliminarArchivo(key);}catch(SQLException ignored){}}
    private static Path uploadRoot(){String configured=System.getenv("SIGE_UPLOAD_DIR");Path root=configured==null||configured.isBlank()?Path.of("data","uploads"):Path.of(configured);return root.toAbsolutePath().normalize();}
    private static String safeFileName(String original){String value=original==null?"archivo":original.replace('\\','/');int slash=value.lastIndexOf('/');if(slash>=0)value=value.substring(slash+1);value=value.replaceAll("[\\p{Cntrl}]", "_").trim();if(value.isBlank()||value.equals(".")||value.equals(".."))value="archivo";return value.length()>180?value.substring(value.length()-180):value;}
    private static String humanBytes(long n){if(n<1024)return n+" B";if(n<1024*1024)return String.format(java.util.Locale.ROOT,"%.1f KB",n/1024.0);return String.format(java.util.Locale.ROOT,"%.1f MB",n/(1024.0*1024));}

    private record UploadedFile(String filename,String contentType,byte[] data){}
    private record MultipartData(Map<String,String> fields,Map<String,UploadedFile> files){}
    private static MultipartData multipartForm(HttpExchange x,String contentType)throws IOException{
        String boundary=null;for(String part:contentType.split(";")){String t=part.trim();if(t.startsWith("boundary=")){boundary=t.substring(9).replaceAll("^\"|\"$","");break;}}
        if(boundary==null||boundary.isBlank()||boundary.length()>100)throw new IOException("Formulario multipart inválido.");
        int declared=x.getRequestHeaders().getFirst("Content-Length")==null?-1:Integer.parseInt(x.getRequestHeaders().getFirst("Content-Length"));if(declared>MAX_MULTIPART_BYTES)throw new UploadTooLargeException();
        byte[] body=x.getRequestBody().readNBytes(MAX_MULTIPART_BYTES+1);if(body.length>MAX_MULTIPART_BYTES)throw new UploadTooLargeException();
        byte[] marker=("--"+boundary).getBytes(StandardCharsets.ISO_8859_1),separator=("\r\n--"+boundary).getBytes(StandardCharsets.ISO_8859_1);Map<String,String> fields=new HashMap<>();Map<String,UploadedFile> files=new HashMap<>();int pos=indexOf(body,marker,0);
        while(pos>=0){int headStart=pos+marker.length;if(headStart+2<=body.length&&body[headStart]=='-'&&body[headStart+1]=='-')break;if(headStart+2>body.length||body[headStart]!='\r'||body[headStart+1]!='\n')throw new IOException("Formulario multipart inválido.");int headerStart=headStart+2,headerEnd=indexOf(body,new byte[]{13,10,13,10},headerStart);if(headerEnd<0)throw new IOException("Encabezado de archivo inválido.");String headers=new String(body,headerStart,headerEnd-headerStart,StandardCharsets.ISO_8859_1);int dataStart=headerEnd+4,next=indexOf(body,separator,dataStart);if(next<0)throw new IOException("Parte multipart incompleta.");String disposition="";String mime="application/octet-stream";for(String line:headers.split("\r\n")){if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-disposition:"))disposition=line;if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-type:"))mime=line.substring(line.indexOf(':')+1).trim();}
            String field=dispositionValue(disposition,"name");String filename=dispositionValue(disposition,"filename");int length=next-dataStart;if(field!=null){if(filename!=null){if(length>MAX_UPLOAD_BYTES)throw new UploadTooLargeException();files.put(field,new UploadedFile(filename,mime,java.util.Arrays.copyOfRange(body,dataStart,next)));}else if(length<=64*1024)fields.put(field,new String(body,dataStart,length,StandardCharsets.UTF_8));}
            pos=next+2;
        }
        return new MultipartData(fields,files);
    }
    private static String dispositionValue(String header,String name){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:^|;)\\s*"+name+"=\"([^\"]*)\"|(?:^|;)\\s*"+name+"=([^;]*)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(header);if(!m.find())return null;return m.group(1)!=null?m.group(1):m.group(2).trim();}
    private static int indexOf(byte[] data,byte[] needle,int from){outer:for(int i=Math.max(0,from);i<=data.length-needle.length;i++){for(int j=0;j<needle.length;j++)if(data[i+j]!=needle[j])continue outer;return i;}return -1;}
    private static final class UploadTooLargeException extends IOException{private static final long serialVersionUID=1L;}
    private static final class FormTooLargeException extends IOException{private static final long serialVersionUID=1L;}


    private static Integer parseQueryInteger(String query,String key){
        if(query==null)return null;
        for(String part:query.split("&")){String[] kv=part.split("=",2);if(kv.length==2&&kv[0].equals(key))try{return Integer.valueOf(URLDecoder.decode(kv[1],StandardCharsets.UTF_8));}catch(NumberFormatException ignored){return -1;}}
        return null;
    }

    private static String teacherAttendancePage(WebSession s,String query,String message){
        if(s.user==null||s.user.getRol()!=Rol.PROFESOR)return shell("Tomar asistencia",errorPage("Esta vista está disponible para profesores."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            List<Seccion> sections=new SeccionDAO().listarPorProfesor(s.user.getId());
            Integer selected=parseQueryInteger(query,"seccion");
            String requestedDate=queryValue(query,"fecha");
            java.time.LocalDate date;
            try{date=requestedDate==null?java.time.LocalDate.now():java.time.LocalDate.parse(requestedDate);}
            catch(java.time.DateTimeException e){date=java.time.LocalDate.now();message="La fecha seleccionada no es válida.";}
            Seccion active=sections.stream().filter(sec->selected!=null&&sec.getId()==selected).findFirst().orElse(null);
            if(message==null&&query!=null&&query.contains("guardado=1"))message="La lista de asistencia se guardó correctamente.";
            StringBuilder options=new StringBuilder("<option value=''>Selecciona una sección...</option>");
            for(Seccion sec:sections)options.append("<option value='").append(sec.getId()).append("'")
                    .append(active!=null&&active.getId()==sec.getId()?" selected":"").append(">")
                    .append(esc(sec.getAsignatura().getCodigo()+" · "+sec.getAsignatura().getNombre()+" · "+(sec.getGrado()==null?"Grado pendiente":sec.getGrado().nombre())+" · "+sec.getHorario()+" · Aula "+sec.getAula())).append("</option>");
            StringBuilder body=new StringBuilder("<div class='page-heading'><div><div class='eyebrow'>GESTIÓN ACADÉMICA</div><h1>Tomar asistencia</h1><p>Selecciona una de tus secciones y la fecha de la clase.</p></div></div>");
            if(message!=null)body.append("<div class='success' role='status'>").append(esc(message)).append("</div>");
            body.append("<section class='card section-picker'><form method='get' action='/asistencia-docente'><label>Sección<select name='seccion' required>")
                    .append(options).append("</select></label><label>Fecha<input type='date' name='fecha' value='").append(date)
                    .append("' required></label><button class='primary' type='submit'>Cargar lista</button></form></section>");
            if(selected!=null&&active==null)body.append(alert("La sección elegida no está asignada a tu cuenta."));
            if(active!=null){
                List<FilaAsistencia> roster=new AsistenciaDAO().listarPorSeccionYFecha(active.getId(),date);
                body.append("<section class='card table-card'><h2>").append(esc(active.getAsignatura().getCodigo()+" · "+active.getAsignatura().getNombre()+" · "+(active.getGrado()==null?"Grado pendiente":active.getGrado().nombre())))
                        .append("</h2><p class='muted'>").append(esc(date+" · "+active.getHorario()+" · Aula "+active.getAula()))
                        .append("</p><form method='post' action='/guardar-asistencia'>").append(hiddenCsrf(s))
                        .append("<input type='hidden' name='seccion' value='").append(active.getId()).append("'><input type='hidden' name='fecha' value='").append(date)
                        .append("'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Matrícula</th><th>Estudiante</th><th>Presente</th></tr></thead><tbody>");
                if(roster.isEmpty())body.append("<tr><td colspan='3' class='empty'>Esta sección todavía no tiene estudiantes inscritos.</td></tr>");
                for(FilaAsistencia student:roster)body.append("<tr><td>").append(esc(student.matricula())).append("</td><td>").append(esc(student.estudiante()))
                        .append("</td><td><label class='attendance-check'><input type='checkbox' name='presente_").append(student.idDetalleInscripcion())
                        .append("' value='1'").append(student.presente()==null||student.presente()?" checked":"").append("><span>Presente</span></label></td></tr>");
                body.append("</tbody></table></div><button class='primary attendance-save' type='submit' ")
                        .append(roster.isEmpty()?"disabled":"").append(">Guardar lista de asistencia</button></form></section>")
                        .append("<p class='hint'>Los estudiantes aparecen marcados como presentes por defecto. Desmarca a quienes estuvieron ausentes.</p>");
            }
            return shell("Tomar asistencia",body.toString(),true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Tomar asistencia",alert("No se pudo cargar la asistencia. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void saveAttendance(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.PROFESOR){page(x,s,errorPage("Solo los profesores pueden tomar asistencia."));return;}
        int sectionId;java.time.LocalDate date;
        try{sectionId=Integer.parseInt(values.getOrDefault("seccion",""));date=java.time.LocalDate.parse(values.getOrDefault("fecha",""));}
        catch(RuntimeException e){page(x,s,teacherAttendancePage(s,null,"La sección o fecha enviada no es válida."));return;}
        try{
            List<FilaAsistencia> roster=new AsistenciaDAO().listarPorSeccionYFecha(sectionId,date);
            List<FilaAsistencia> marked=roster.stream().map(row->new FilaAsistencia(row.idDetalleInscripcion(),row.matricula(),row.estudiante(),date,
                    values.containsKey("presente_"+row.idDetalleInscripcion()))).toList();
            new AsistenciaDAO().guardarPorSeccion(sectionId,s.user.getId(),date,marked);
            redirect(x,"/asistencia-docente?seccion="+sectionId+"&fecha="+date+"&guardado=1");
        }catch(SQLException e){page(x,s,teacherAttendancePage(s,"seccion="+sectionId+"&fecha="+date,"No se pudo guardar la asistencia. Verifica que la sección sea tuya y la conexión con MySQL."));}
    }

    private static String queryValue(String query,String key){
        if(query==null)return null;for(String part:query.split("&")){String[] kv=part.split("=",2);if(kv.length==2&&kv[0].equals(key))return URLDecoder.decode(kv[1],StandardCharsets.UTF_8);}return null;
    }

    private static String enrollmentReportPage(WebSession s,String query,String message){
        if(s.user==null)return shell("Reporte de inscripciones",errorPage("Inicia sesión para consultar el reporte."),false,"");
        java.time.LocalDate today=java.time.LocalDate.now();
        java.time.LocalDate from=parseDate(queryValue(query,"desde"),today.withDayOfMonth(1));
        java.time.LocalDate to=parseDate(queryValue(query,"hasta"),today);
        try{
            List<LineaReporteInscripcion> rows=new ReporteDAO().listarInscripciones(from,to,s.user.getRol(),s.user.getId());
            StringBuilder table=new StringBuilder();
            for(LineaReporteInscripcion row:rows)table.append("<tr><td>").append(row.fecha()).append("</td><td>")
                    .append(esc(row.estudiante())).append("</td><td>").append(esc(row.matricula())).append("</td><td>")
                    .append(esc(row.periodo())).append("</td><td>").append(esc(row.asignatura())).append("</td><td>")
                    .append(esc(row.grado()==null?"Pendiente":row.grado())).append("</td><td>")
                    .append(esc(row.area()==null?"Pendiente":row.area())).append("</td><td>")
                    .append(row.idSeccion()).append("</td><td><span class='state-pill'>").append(esc(row.estado())).append("</span></td></tr>");
            if(table.isEmpty())table.append("<tr><td colspan='9' class='empty'>No hay inscripciones dentro del rango seleccionado.</td></tr>");
            String notice=message==null?"":"<div class='alert' role='alert'>"+esc(message)+"</div>";
            String body="<div class='page-heading'><div><div class='eyebrow'>INFORME ACADÉMICO</div><h1>Reporte de inscripciones</h1><p>Los resultados respetan el alcance de tu rol.</p></div></div>"
                    +notice+"<section class='card section-picker'><form method='get' action='/reporte-inscripciones'><label>Desde<input type='date' name='desde' value='"+from+"' required></label>"
                    +"<label>Hasta<input type='date' name='hasta' value='"+to+"' required></label><button class='primary' type='submit'>Consultar</button></form></section>"
                    +"<section class='metrics grade-summary'>"+metric("Inscripciones",rows.size())+metric("Desde",from.toString())+metric("Hasta",to.toString())+"</section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Fecha</th><th>Estudiante</th><th>Matrícula</th><th>Período</th><th>Asignatura</th><th>Grado</th><th>Área</th><th>Sección</th><th>Estado</th></tr></thead><tbody>"
                    +table+"</tbody></table></div><a class='primary link-button' href='/reporte-inscripciones.csv?desde="+from+"&amp;hasta="+to+"'>Descargar CSV</a></section>";
            return shell("Reporte de inscripciones",body,true,s.user.getNombreCompleto());
        }catch(PermisoDenegadoException e){return shell("Reporte de inscripciones",errorPage(e.getMessage()),true,s.user.getNombreCompleto());}
        catch(SQLException e){return shell("Reporte de inscripciones",alert("No se pudo consultar el reporte. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void downloadEnrollmentReport(HttpExchange x,WebSession s,String query)throws IOException{
        if(s.user==null){page(x,s,errorPage("Inicia sesión para descargar el reporte."));return;}
        java.time.LocalDate today=java.time.LocalDate.now();
        java.time.LocalDate from=parseDate(queryValue(query,"desde"),today.withDayOfMonth(1));
        java.time.LocalDate to=parseDate(queryValue(query,"hasta"),today);
        try{
            List<LineaReporteInscripcion> rows=new ReporteDAO().listarInscripciones(from,to,s.user.getRol(),s.user.getId());
            StringBuilder csv=new StringBuilder("\uFEFFFecha,Estudiante,Matrícula,Período,Asignatura,Grado,Área,Sección,Estado\r\n");
            for(LineaReporteInscripcion row:rows)csv.append(csv(row.fecha().toString())).append(',').append(csv(row.estudiante())).append(',')
                    .append(csv(row.matricula())).append(',').append(csv(row.periodo())).append(',').append(csv(row.asignatura())).append(',')
                    .append(csv(row.grado()==null?"Pendiente":row.grado())).append(',').append(csv(row.area()==null?"Pendiente":row.area())).append(',')
                    .append(row.idSeccion()).append(',').append(csv(row.estado())).append("\r\n");
            x.getResponseHeaders().set("Content-Disposition","attachment; filename=\"reporte-inscripciones.csv\"");
            send(x,200,"text/csv; charset=utf-8",csv.toString());
        }catch(PermisoDenegadoException e){page(x,s,errorPage(e.getMessage()));}
        catch(SQLException e){page(x,s,alert("No se pudo generar el reporte. Verifica la conexión con MySQL."));}
    }

    private static String attendanceReportPage(WebSession s,String query,String message){
        if(s.user==null||(s.user.getRol()!=Rol.ADMIN&&s.user.getRol()!=Rol.PROFESOR))return shell("Reporte de asistencia",errorPage("El reporte está disponible para administración y profesores."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            SeccionDAO sectionsDao=new SeccionDAO();List<Seccion> sections=s.user.getRol()==Rol.ADMIN?sectionsDao.listarTodas():sectionsDao.listarPorProfesor(s.user.getId());
            Integer selected=parseQueryInteger(query,"seccion");Seccion selectedSection=sections.stream().filter(sec->selected!=null&&sec.getId()==selected).findFirst().orElse(null);
            java.time.LocalDate today=java.time.LocalDate.now();java.time.LocalDate from=parseDate(queryValue(query,"desde"),today.withDayOfMonth(1));java.time.LocalDate to=parseDate(queryValue(query,"hasta"),today);
            StringBuilder options=new StringBuilder("<option value=''>Selecciona una sección...</option>");
            for(Seccion sec:sections)options.append("<option value='").append(sec.getId()).append("'").append(selectedSection!=null&&selectedSection.getId()==sec.getId()?" selected":"").append(">").append(esc(sec.getAsignatura().getCodigo()+" · "+sec.getAsignatura().getNombre()+" · "+sec.getHorario()+" · Aula "+sec.getAula())).append("</option>");
            StringBuilder rows=new StringBuilder();List<FilaAsistencia> records=List.of();String error=message;
            if(selected!=null&&selectedSection==null)error="La sección elegida no está asignada a tu cuenta.";
            else if(selectedSection!=null){
                if(from.isAfter(to))error="La fecha inicial no puede ser posterior a la fecha final.";
                else if(java.time.temporal.ChronoUnit.DAYS.between(from,to)>366)error="El rango máximo del reporte es de 366 días.";
                else records=new AsistenciaDAO().listarReportePorSeccion(selectedSection.getId(),s.user.getId(),s.user.getRol()==Rol.ADMIN,from,to);
            }
            long present=records.stream().filter(row->Boolean.TRUE.equals(row.presente())).count();long absent=records.stream().filter(row->Boolean.FALSE.equals(row.presente())).count();
            for(FilaAsistencia row:records)rows.append("<tr><td>").append(esc(row.fecha().toString())).append("</td><td>").append(esc(row.matricula())).append("</td><td><b>").append(esc(row.estudiante())).append("</b></td><td><span class='state-pill ").append(Boolean.TRUE.equals(row.presente())?"account-active":"account-disabled").append("'>").append(Boolean.TRUE.equals(row.presente())?"Presente":"Ausente").append("</span></td></tr>");
            if(records.size()>5000){error="Hay más de 5,000 marcas. Reduce el rango de fechas para consultar el reporte completo.";records=List.of();rows.setLength(0);present=0;absent=0;}
            if(rows.isEmpty())rows.append("<tr><td colspan='4' class='empty'>No hay marcas de asistencia en el rango seleccionado.</td></tr>");
            String body="<div class='page-heading'><div><div class='eyebrow'>INFORME ACADÉMICO</div><h1>Reporte de asistencia</h1><p>Consulta las marcas registradas por sección y período.</p></div></div>"+(error==null?"":"<div class='alert' role='status'>"+esc(error)+"</div>")
                    +"<section class='card section-picker'><form method='get' action='/reporte-asistencia'><label>Sección<select name='seccion' required>"+options+"</select></label><label>Desde<input type='date' name='desde' value='"+from+"' required></label><label>Hasta<input type='date' name='hasta' value='"+to+"' required></label><button class='primary' type='submit'>Consultar</button></form></section>"
                    +(selectedSection==null?"":"<section class='metrics grade-summary'>"+metric("Marcas",records.size())+metric("Presentes",present)+metric("Ausentes",absent)+metric("Asistencia",String.format(java.util.Locale.ROOT,"%.1f%%",records.isEmpty()?0:present*100.0/records.size()))+"</section><section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Fecha</th><th>Matrícula</th><th>Estudiante</th><th>Estado</th></tr></thead><tbody>"+rows+"</tbody></table></div><a class='primary link-button' href='/reporte-asistencia.csv?seccion="+selectedSection.getId()+"&amp;desde="+from+"&amp;hasta="+to+"'>Descargar CSV</a></section>");
            return shell("Reporte de asistencia",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Reporte de asistencia",alert("No se pudo cargar el reporte. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void downloadAttendanceReport(HttpExchange x,WebSession s,String query)throws IOException{
        if(s.user==null||(s.user.getRol()!=Rol.ADMIN&&s.user.getRol()!=Rol.PROFESOR)){send(x,403,"text/plain; charset=utf-8","El reporte está disponible para administración y profesores.");return;}
        int sectionId;try{sectionId=Integer.parseInt(queryValue(query,"seccion"));}catch(RuntimeException e){send(x,400,"text/plain; charset=utf-8","Sección inválida.");return;}
        java.time.LocalDate today=java.time.LocalDate.now();java.time.LocalDate from=parseDate(queryValue(query,"desde"),today.withDayOfMonth(1));java.time.LocalDate to=parseDate(queryValue(query,"hasta"),today);
        if(from.isAfter(to)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>366){send(x,400,"text/plain; charset=utf-8","El rango de fechas no es válido o supera 366 días.");return;}
        try{
            List<Seccion> allowed=s.user.getRol()==Rol.ADMIN?new SeccionDAO().listarTodas():new SeccionDAO().listarPorProfesor(s.user.getId());
            if(allowed.stream().noneMatch(section->section.getId()==sectionId)){send(x,403,"text/plain; charset=utf-8","No tienes permiso para consultar esa sección.");return;}
            List<FilaAsistencia> rows=new AsistenciaDAO().listarReportePorSeccion(sectionId,s.user.getId(),s.user.getRol()==Rol.ADMIN,from,to);
            if(rows.size()>5000){send(x,413,"text/plain; charset=utf-8","El reporte supera 5,000 marcas. Reduce el rango de fechas.");return;}
            StringBuilder csv=new StringBuilder("\uFEFFFecha,Matrícula,Estudiante,Estado\r\n");
            for(FilaAsistencia row:rows)csv.append(csv(row.fecha().toString())).append(',').append(csv(row.matricula())).append(',').append(csv(row.estudiante())).append(',').append(csv(Boolean.TRUE.equals(row.presente())?"Presente":"Ausente")).append("\r\n");
            x.getResponseHeaders().set("Content-Disposition","attachment; filename=\"reporte-asistencia-sige.csv\"");send(x,200,"text/csv; charset=utf-8",csv.toString());
        }catch(SQLException e){send(x,500,"text/plain; charset=utf-8","No se pudo generar el reporte. Verifica la conexión con MySQL.");}
    }

    private static java.time.LocalDate parseDate(String value,java.time.LocalDate fallback){
        if(value==null||value.isBlank())return fallback;
        try{return java.time.LocalDate.parse(value);}catch(java.time.DateTimeException e){return fallback;}
    }

    private static String csv(String value){
        String safe=value==null?"":value;
        if(!safe.isEmpty()&&"=+-@\t\r".indexOf(safe.charAt(0))>=0)safe="'"+safe;
        return "\""+safe.replace("\"","\"\"")+"\"";
    }

    private static String curriculumPage(WebSession s){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)
            return shell("Currículo",errorPage("Solo un administrador puede consultar el catálogo curricular."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            List<GradoCurricular> grades=new CurriculoDAO().listarGrados();
            if(grades.isEmpty())return shell("Currículo",alert("El catálogo curricular aún no tiene datos. Ejecuta sql/fase_curriculo_grados.sql en sige_db."),true,s.user.getNombreCompleto());
            StringBuilder body=new StringBuilder("<div class='page-heading'><div><div class='eyebrow'>PLAN DE ESTUDIOS · REPÚBLICA DOMINICANA</div><h1>Grados y áreas curriculares</h1><p>Catálogo base de Primaria y Secundaria, organizado por ciclo.</p></div></div>");
            body.append("<div class='curriculum-note'><b>Base común:</b> el catálogo nacional sirve para centros públicos y privados. Los planes pueden ampliar idiomas, talleres y optativas. Las horas semanales quedan configurables según el plan y la jornada del centro.</div>");
            for(String level:List.of("Primaria","Secundaria")){
                body.append("<section class='curriculum-level'><div class='curriculum-level-heading'><div><div class='eyebrow'>NIVEL</div><h2>").append(level).append("</h2></div><span class='role-badge'>").append(level.equals("Primaria")?"6 grados":"6 grados · áreas comunes y modalidad en segundo ciclo").append("</span></div><div class='curriculum-grid'>");
                for(GradoCurricular grade:grades){
                    if(!grade.nivel().equals(level))continue;
                    body.append("<article class='card curriculum-grade'><div class='curriculum-grade-heading'><span class='role-badge'>").append(esc(grade.ciclo())).append("</span><h3>").append(esc(grade.nombre())).append("</h3></div><ul>");
                    for(String area:grade.areas())body.append("<li>").append(esc(area)).append("</li>");
                    body.append("</ul>");
                    if(grade.codigo().equals("P1"))body.append("<p class='curriculum-focus'>Prioridad: alfabetización inicial y sentido numérico.</p>");
                    if(grade.codigo().equals("P4"))body.append("<p class='curriculum-focus'>En el segundo ciclo se incorpora Inglés.</p>");
                    if(grade.codigo().equals("S1"))body.append("<p class='curriculum-focus'>Primer ciclo: formación común y orientación.</p>");
                    if(grade.codigo().equals("S4"))body.append("<p class='curriculum-focus'>Segundo ciclo: Académica, Técnico-Profesional o Artes.</p>");
                    body.append("</article>");
                }
                body.append("</div></section>");
            }
            body.append("<p class='help-text'>Las áreas se asocian a las asignaturas y estas a sus grados. Las secciones operativas heredan el grado y muestran su asignatura, profesor y horario.</p>");
            return shell("Currículo",body.toString(),true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Currículo",alert("No se pudo leer el catálogo curricular. Ejecuta sql/fase_curriculo_grados.sql en sige_db y verifica la conexión."),true,s.user.getNombreCompleto());}
    }
    private static String subjectsAdminPage(WebSession s,String query,String message,Asignatura editing){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Asignaturas",errorPage("Solo un administrador puede gestionar las asignaturas."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            AsignaturaDAO dao=new AsignaturaDAO();List<Asignatura> subjects=dao.listarTodas();List<GradoCurricular> grades=new CurriculoDAO().listarGrados();Map<String,String> areas=new CurriculoDAO().listarAreas();
            if(editing==null){
                Integer editId=parseQueryInteger(query,"editar");
                if(editId!=null)editing=subjects.stream().filter(a->a.getId()==editId).findFirst().orElse(null);
            }
            boolean actionSuccess=message==null&&query!=null&&(query.contains("guardado=1")||query.contains("eliminado=1"));
            if(message==null&&query!=null&&query.contains("guardado=1"))message="La asignatura se guardó correctamente.";
            if(message==null&&query!=null&&query.contains("eliminado=1"))message="La asignatura se eliminó correctamente.";
            Asignatura value=editing==null?new Asignatura(0,"","",0,""):editing;
            StringBuilder areaOptions=new StringBuilder("<option value=''>Selecciona el área...</option>");
            areas.forEach((code,name)->areaOptions.append("<option value='").append(esc(code)).append("'").append(code.equals(value.getCodigoArea())?" selected":"").append(">").append(esc(name)).append("</option>"));
            StringBuilder gradeOptions=new StringBuilder();
            for(GradoCurricular g:grades){boolean selected=value.getIdsGrados()!=null&&List.of(value.getIdsGrados().split(",")).contains(String.valueOf(g.id()));gradeOptions.append("<option value='").append(g.id()).append("'").append(selected?" selected":"").append(">").append(esc(g.nombre()+" · "+g.ciclo())).append("</option>");}
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Asignaturas</h1><p>Gestiona el catálogo de materias del centro.</p></div></div>"
                    +(message==null?"":"<div class='"+(actionSuccess?"success":"alert")+"' role='status'>"+esc(message)+"</div>")
                    +"<section class='card subject-editor'><h2>"+(value.getId()==0?"Nueva asignatura":"Editar asignatura")+"</h2><form method='post' action='/guardar-asignatura'>"+hiddenCsrf(s)
                    +"<input type='hidden' name='id' value='"+value.getId()+"'><div class='subject-fields'><label>Código<input name='codigo' maxlength='10' value='"+esc(value.getCodigo())+"' required></label>"
                    +"<label>Nombre<input name='nombre' maxlength='100' value='"+esc(value.getNombre())+"' required></label><label>Área curricular<select name='area' required>"+areaOptions+"</select></label><label>Grados (puedes elegir varios)<select name='grados' multiple size='4' required>"+gradeOptions+"</select></label><label>Créditos / carga<input name='creditos' type='number' min='0' max='99' value='"+value.getCreditos()+"' required></label>"
                    +"<label class='subject-description'>Descripción<textarea name='descripcion' rows='3'>"+esc(value.getDescripcion())+"</textarea></label></div><div class='subject-actions'><button class='primary' type='submit'>"+(value.getId()==0?"Crear asignatura":"Guardar cambios")+"</button>"
                    +(value.getId()==0?"":"<a class='text-link' href='/asignaturas'>Cancelar edición</a>")+"</div></form></section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Código</th><th>Nombre</th><th>Área curricular</th><th>Grados</th><th>Créditos</th><th>Descripción</th><th>Acciones</th></tr></thead><tbody>";
            StringBuilder rows=new StringBuilder();
            for(Asignatura a:subjects)rows.append("<tr><td><b>").append(esc(a.getCodigo())).append("</b></td><td>").append(esc(a.getNombre()))
                    .append("</td><td>").append(esc(a.getAreaCurricular()==null?"Pendiente":a.getAreaCurricular())).append("</td><td>").append(esc(a.getGradosCurriculares()==null?"Pendiente":a.getGradosCurriculares())).append("</td><td>").append(a.getCreditos()).append("</td><td>").append(esc(a.getDescripcion())).append("</td><td><div class='subject-actions'><a class='text-link' href='/asignaturas?editar=")
                    .append(a.getId()).append("'>Editar</a><form method='post' action='/eliminar-asignatura' class='inline-form'>").append(hiddenCsrf(s))
                    .append("<input type='hidden' name='id' value='").append(a.getId()).append("'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar eliminación</label><button class='danger' type='submit'>Eliminar</button></form></div></td></tr>");
            if(rows.isEmpty())rows.append("<tr><td colspan='7' class='empty'>No hay asignaturas registradas.</td></tr>");
            return shell("Asignaturas",body+rows+"</tbody></table></div></section>",true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Asignaturas",alert("No se pudo cargar el catálogo. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void saveSubject(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede guardar asignaturas."));return;}
        int id,credits;
        try{id=Integer.parseInt(values.getOrDefault("id","0"));credits=Integer.parseInt(values.getOrDefault("creditos",""));}
        catch(NumberFormatException e){page(x,s,subjectsAdminPage(s,null,"Los créditos deben ser un número entero.",null));return;}
        String code=values.getOrDefault("codigo","").trim(),name=values.getOrDefault("nombre","").trim(),description=values.getOrDefault("descripcion","").trim();
        String validation=null;
        if(code.isBlank()||name.isBlank())validation="El código y el nombre son obligatorios.";
        else if(code.length()>10)validation="El código admite máximo 10 caracteres.";
        else if(name.length()>100)validation="El nombre admite máximo 100 caracteres.";
        else if(credits<0||credits>99)validation="El valor debe estar entre 0 y 99.";
        String areaCode=values.getOrDefault("area","").trim();List<Integer> gradeIds=new java.util.ArrayList<>();
        for(String rawGrade:values.getOrDefault("grados","").split(",")){if(!rawGrade.isBlank())try{gradeIds.add(Integer.parseInt(rawGrade));}catch(NumberFormatException ignored){}}
        if(validation==null&&(areaCode.isBlank()||gradeIds.isEmpty()))validation="Selecciona el área curricular y al menos un grado.";
        if(validation!=null){page(x,s,subjectsAdminPage(s,null,validation,new Asignatura(id,code,name,credits,description)));return;}
        try{
            AsignaturaDAO dao=new AsignaturaDAO();
            if(id>0&&dao.listarTodas().stream().noneMatch(a->a.getId()==id)){page(x,s,subjectsAdminPage(s,null,"La asignatura seleccionada ya no existe.",null));return;}
            if(dao.existeCodigo(code,id)){page(x,s,subjectsAdminPage(s,null,"Ya existe una asignatura con ese código.",new Asignatura(id,code,name,credits,description)));return;}
            Asignatura subject=new Asignatura(id,code,name,credits,description.isBlank()?null:description);
            if(id==0)dao.insertar(subject);else dao.actualizar(subject);
            dao.guardarCurriculo(subject.getId(),areaCode,gradeIds);
            redirect(x,"/asignaturas?guardado=1");
        }catch(SQLException e){page(x,s,subjectsAdminPage(s,null,"No se pudo guardar la asignatura. Verifica la conexión y el catálogo curricular.",new Asignatura(id,code,name,credits,description)));}
    }

    private static void deleteSubject(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede eliminar asignaturas."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,subjectsAdminPage(s,null,"Confirma la eliminación antes de continuar.",null));return;}
        int id;try{id=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,subjectsAdminPage(s,null,"La asignatura seleccionada no es válida.",null));return;}
        try{
            AsignaturaDAO dao=new AsignaturaDAO();
            if(dao.tieneSecciones(id)){page(x,s,subjectsAdminPage(s,null,"La asignatura no se puede eliminar porque tiene secciones asociadas.",null));return;}
            dao.eliminar(id);redirect(x,"/asignaturas?eliminado=1");
        }catch(SQLException e){page(x,s,subjectsAdminPage(s,null,"No se pudo eliminar la asignatura.",null));}
    }

    private static String sectionsAdminPage(WebSession s,String query,String message,Seccion editing){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Secciones",errorPage("Solo un administrador puede gestionar las secciones."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            SeccionDAO dao=new SeccionDAO();List<Seccion> sections=dao.listarTodas();List<Asignatura> subjects=new AsignaturaDAO().listarTodas();List<GradoCurricular> grades=new CurriculoDAO().listarGrados();
            List<Profesor> teachers=USERS.listarProfesores();
            if(editing==null){Integer editId=parseQueryInteger(query,"editar");if(editId!=null)editing=sections.stream().filter(z->z.getId()==editId).findFirst().orElse(null);}
            boolean success=message==null&&query!=null&&(query.contains("guardado=1")||query.contains("eliminado=1"));
            if(message==null&&query!=null&&query.contains("guardado=1"))message="La sección se guardó correctamente.";
            if(message==null&&query!=null&&query.contains("eliminado=1"))message="La sección se eliminó correctamente.";
            Seccion value=editing==null?new Seccion(0,"","",30,null,null):editing;
            StringBuilder subjectOptions=new StringBuilder("<option value=''>Selecciona una asignatura...</option>");
            for(Asignatura a:subjects)subjectOptions.append("<option value='").append(a.getId()).append("'")
                    .append(value.getAsignatura()!=null&&value.getAsignatura().getId()==a.getId()?" selected":"").append(">")
                    .append(esc(a.getCodigo()+" · "+a.getNombre())).append("</option>");
            StringBuilder gradeOptions=new StringBuilder("<option value=''>Selecciona el grado...</option>");
            for(GradoCurricular g:grades)gradeOptions.append("<option value='").append(g.id()).append("'").append(value.getGrado()!=null&&value.getGrado().id()==g.id()?" selected":"").append(">").append(esc(g.nombre()+" · "+g.ciclo())).append("</option>");
            StringBuilder teacherOptions=new StringBuilder("<option value='0'>Sin profesor asignado</option>");
            for(Profesor p:teachers)teacherOptions.append("<option value='").append(p.getId()).append("'")
                    .append(value.getProfesor()!=null&&value.getProfesor().getId()==p.getId()?" selected":"").append(">")
                    .append(esc(p.getNombreCompleto()+" · "+p.getCodigoProfesor())).append("</option>");
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Secciones</h1><p>Asigna asignaturas, profesores, horarios, aulas y cupos.</p></div></div>"
                    +(message==null?"":"<div class='"+(success?"success":"alert")+"' role='status'>"+esc(message)+"</div>")
                    +"<section class='card subject-editor'><h2>"+(value.getId()==0?"Nueva sección":"Editar sección")+"</h2><form method='post' action='/guardar-seccion'>"+hiddenCsrf(s)
                    +"<input type='hidden' name='id' value='"+value.getId()+"'><div class='subject-fields'><label>Asignatura<select name='asignatura' required>"+subjectOptions+"</select></label>"
                    +"<label>Grado<select name='grado' required>"+gradeOptions+"</select></label><label>Profesor<select name='profesor'>"+teacherOptions+"</select></label><label>Aula<input name='aula' maxlength='20' value='"+esc(value.getAula())+"' required></label>"
                    +"<label>Horario<input name='horario' maxlength='50' value='"+esc(value.getHorario())+"' required></label><label>Cupo máximo<input name='cupo' type='number' min='1' max='500' value='"+value.getCupoMaximo()+"' required></label></div>"
                    +"<div class='subject-actions'><button class='primary' type='submit'>"+(value.getId()==0?"Crear sección":"Guardar cambios")+"</button>"+(value.getId()==0?"":"<a class='text-link' href='/secciones'>Cancelar edición</a>")+"</div></form></section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>ID</th><th>Grado</th><th>Asignatura</th><th>Profesor</th><th>Aula</th><th>Horario</th><th>Inscritos / Cupo</th><th>Acciones</th></tr></thead><tbody>";
            StringBuilder rows=new StringBuilder();
            for(Seccion section:sections)rows.append("<tr><td>").append(section.getId()).append("</td><td>").append(esc(section.getGrado()==null?"Pendiente":section.getGrado().nombre())).append("</td><td><b>").append(esc(section.getAsignatura().getCodigo()))
                    .append("</b><br>").append(esc(section.getAsignatura().getNombre())).append("</td><td>")
                    .append(esc(section.getProfesor()==null?"Sin asignar":section.getProfesor().getNombreCompleto())).append("</td><td>")
                    .append(esc(section.getAula())).append("</td><td>").append(esc(section.getHorario())).append("</td><td>")
                    .append(section.getInscritos()).append(" / ").append(section.getCupoMaximo()).append("</td><td><div class='subject-actions'><a class='text-link' href='/secciones?editar=")
                    .append(section.getId()).append("'>Editar</a><form method='post' action='/eliminar-seccion' class='inline-form'>").append(hiddenCsrf(s))
                    .append("<input type='hidden' name='id' value='").append(section.getId()).append("'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='danger' type='submit'>Eliminar</button></form></div></td></tr>");
            if(rows.isEmpty())rows.append("<tr><td colspan='8' class='empty'>No hay secciones registradas.</td></tr>");
            return shell("Secciones",body+rows+"</tbody></table></div></section>",true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Secciones",alert("No se pudo cargar la gestión de secciones. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void saveSection(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede guardar secciones."));return;}
        int id,subjectId,teacherId,capacity,gradeId;
        try{id=Integer.parseInt(values.getOrDefault("id","0"));subjectId=Integer.parseInt(values.getOrDefault("asignatura",""));teacherId=Integer.parseInt(values.getOrDefault("profesor","0"));gradeId=Integer.parseInt(values.getOrDefault("grado",""));capacity=Integer.parseInt(values.getOrDefault("cupo",""));}
        catch(NumberFormatException e){page(x,s,sectionsAdminPage(s,null,"Revisa la asignatura, el profesor y el cupo ingresados.",null));return;}
        String room=values.getOrDefault("aula","").trim(),schedule=values.getOrDefault("horario","").trim();
        String error=room.isBlank()||schedule.isBlank()?"Aula y horario son obligatorios.":room.length()>20?"El aula admite máximo 20 caracteres.":schedule.length()>50?"El horario admite máximo 50 caracteres.":capacity<1||capacity>500?"El cupo debe estar entre 1 y 500.":null;
        try{
            SeccionDAO dao=new SeccionDAO();List<Seccion> current=dao.listarTodas();
            Seccion old=id==0?null:current.stream().filter(z->z.getId()==id).findFirst().orElse(null);
            List<Asignatura> subjects=new AsignaturaDAO().listarTodas();List<Profesor> teachers=USERS.listarProfesores();List<GradoCurricular> grades=new CurriculoDAO().listarGrados();
            Asignatura subject=subjects.stream().filter(a->a.getId()==subjectId).findFirst().orElse(null);
            Profesor teacher=teacherId==0?null:teachers.stream().filter(p->p.getId()==teacherId).findFirst().orElse(null);
            GradoCurricular grade=grades.stream().filter(g->g.id()==gradeId).findFirst().orElse(null);
            if(error==null&&id>0&&old==null)error="La sección seleccionada ya no existe.";
            if(error==null&&subject==null)error="Selecciona una asignatura válida.";
            if(error==null&&grade==null)error="Selecciona un grado curricular válido.";
            if(error==null&&!new AsignaturaDAO().puedeOfrecerse(subjectId,gradeId))error="La asignatura no está asociada al grado seleccionado. Revísala en Asignaturas.";
            if(error==null&&teacherId!=0&&teacher==null)error="Selecciona un profesor válido.";
            if(error==null&&old!=null&&capacity<old.getInscritos())error="El cupo no puede ser menor que los "+old.getInscritos()+" estudiantes ya inscritos.";
            if(error==null&&old!=null&&old.getInscritos()>0&&old.getAsignatura().getId()!=subjectId)error="No se puede cambiar la asignatura de una sección que ya tiene estudiantes inscritos.";
            if(error==null&&old!=null&&old.getInscritos()>0&&old.getGrado()!=null&&old.getGrado().id()!=gradeId)error="No se puede cambiar el grado de una sección que ya tiene estudiantes inscritos.";
            if(error!=null){Seccion rejected=new Seccion(id,room,schedule,capacity,subject,teacher);rejected.setGrado(grade);page(x,s,sectionsAdminPage(s,null,error,rejected));return;}
            Seccion section=new Seccion(id,room,schedule,capacity,subject,teacher);section.setGrado(grade);
            if(id==0)dao.insertar(section);else dao.actualizar(section);
            redirect(x,"/secciones?guardado=1");
        }catch(SQLException e){page(x,s,sectionsAdminPage(s,null,"No se pudo guardar la sección. Verifica la conexión con MySQL.",null));}
    }

    private static void deleteSection(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede eliminar secciones."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,sectionsAdminPage(s,null,"Confirma la eliminación antes de continuar.",null));return;}
        int id;try{id=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,sectionsAdminPage(s,null,"La sección seleccionada no es válida.",null));return;}
        try{
            SeccionDAO dao=new SeccionDAO();int count=dao.contarInscritos(id);
            if(count>0){page(x,s,sectionsAdminPage(s,null,"No se puede eliminar la sección: tiene "+count+" estudiante(s) inscrito(s).",null));return;}
            dao.eliminar(id);redirect(x,"/secciones?eliminado=1");
        }catch(SQLException e){page(x,s,sectionsAdminPage(s,null,"No se pudo eliminar la sección.",null));}
    }

    private static String solicitudesPage(WebSession s,String query,String message){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Solicitudes de registro",errorPage("Solo un administrador puede revisar solicitudes de registro."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            boolean success=message==null&&query!=null&&query.contains("atendida=1");
            if(success)message="La solicitud se atendió correctamente.";
            List<UsuarioDAO.Solicitud> requests=USERS.listarSolicitudes();
            StringBuilder rows=new StringBuilder();
            for(UsuarioDAO.Solicitud r:requests){
                rows.append("<tr><td>").append(r.id()).append("</td><td><b>").append(esc(r.nombre()+" "+r.apellido())).append("</b></td><td>")
                        .append(esc(r.correo())).append("</td><td>").append("ESTUDIANTE".equals(r.rol())?"Estudiante":"Padre / tutor").append("</td><td>")
                        .append(esc(r.detalle())).append("</td><td><div class='user-actions'>")
                        .append(requestForm(s,r.id(),"aprobar","Aprobar","primary")).append(requestForm(s,r.id(),"rechazar","Rechazar","danger"))
                        .append("</div></td></tr>");
            }
            if(rows.isEmpty())rows.append("<tr><td colspan='6' class='empty'>No hay solicitudes pendientes.</td></tr>");
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Solicitudes de registro</h1><p>Aprueba o rechaza las cuentas creadas desde el registro público.</p></div></div>"
                    +(message==null?"":"<div class='"+(success?"success":"alert")+"' role='status'>"+esc(message)+"</div>")
                    +"<section class='metrics grade-summary'>"+metric("Pendientes",requests.size())+"</section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table users-table'><thead><tr><th>ID</th><th>Nombre</th><th>Correo</th><th>Tipo</th><th>Datos</th><th>Acciones</th></tr></thead><tbody>"
                    +rows+"</tbody></table></div></section>";
            return shell("Solicitudes de registro",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Solicitudes de registro",alert("No se pudieron cargar las solicitudes. Comprueba que se ejecutó sql/fase_registro_aprobacion.sql en la base de datos."),true,s.user.getNombreCompleto());}
    }
    private static String requestForm(WebSession s,int id,String action,String text,String style){
        return "<form method='post' action='/solicitud' class='inline-form'>"+hiddenCsrf(s)+"<input type='hidden' name='id' value='"+id+"'><input type='hidden' name='accion' value='"+action
                +"'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='"+style+"' type='submit'>"+text+"</button></form>";
    }
    private static void requestAction(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede atender solicitudes de registro."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,solicitudesPage(s,null,"Confirma la acción antes de continuar."));return;}
        int id;try{id=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,solicitudesPage(s,null,"La solicitud seleccionada no es válida."));return;}
        String action=values.getOrDefault("accion","");boolean approve="aprobar".equals(action);
        if(!approve&&!"rechazar".equals(action)){page(x,s,solicitudesPage(s,null,"La acción solicitada no es válida."));return;}
        try{
            String email=approve?USERS.aprobarSolicitudComoAdmin(s.user.getId(),id):USERS.rechazarSolicitudComoAdmin(s.user.getId(),id);
            accessLog(approve?id:null,email,approve?"APROBADO":"RECHAZADO","Solicitud de registro "+(approve?"aprobada":"rechazada")+" por admin "+s.user.getId());
            redirect(x,"/solicitudes?atendida=1");
        }catch(PermisoDenegadoException e){page(x,s,solicitudesPage(s,null,e.getMessage()));}
        catch(SQLException e){page(x,s,solicitudesPage(s,null,"No se pudo atender la solicitud. Verifica la conexión con MySQL."));}
    }
    private static boolean isPendingApproval(int id){try{return USERS.estaPendienteAprobacion(id);}catch(SQLException e){return false;}}
    private static java.util.Set<Integer> pendingIds(){try{return USERS.idsPendientes();}catch(SQLException e){return java.util.Set.of();}}
    private static String pendingNotice(Usuario u){
        if(u==null||u.getRol()!=Rol.ADMIN)return "";
        try{int n=USERS.contarPendientes();if(n<1)return "";
            return "<a class='hero-notice' href='/solicitudes'>"+n+(n==1?" solicitud de registro pendiente":" solicitudes de registro pendientes")+" →</a>";
        }catch(SQLException e){return "";}
    }

    private static String usersAdminPage(WebSession s,String query,String message){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Usuarios",errorPage("Solo un administrador puede gestionar cuentas."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            List<Usuario> allUsers=USERS.listarTodos();String search=queryValue(query,"q");if(search==null)search="";if(search.length()>120)search=search.substring(0,120);
            String role=queryValue(query,"rol");if(role==null||!List.of("ADMIN","PROFESOR","ESTUDIANTE","PADRE").contains(role))role="";
            String state=queryValue(query,"estado");if(state==null||!List.of("activa","inactiva").contains(state))state="";
            List<Usuario> users=USERS.buscarTodos(search,role,state);java.util.Set<Integer> pending=pendingIds();
            boolean success=message==null&&query!=null&&query.contains("actualizado=1");
            if(message==null&&success)message="La acción se completó correctamente.";
            long active=allUsers.stream().filter(Usuario::isActivo).count();
            StringBuilder rows=new StringBuilder();
            for(Usuario u:users){
                rows.append("<tr><td>").append(u.getId()).append("</td><td><b>").append(esc(u.getNombreCompleto())).append("</b></td><td>")
                        .append(esc(u.getCorreo())).append("</td><td>").append(u.getRol()==null?"?":u.getRol().name()).append("</td><td><span class='state-pill ")
                        .append(u.isActivo()?"account-active":"account-disabled").append("'>").append(u.isActivo()?"Activa":pending.contains(u.getId())?"Pendiente":"Inactiva").append("</span></td><td><div class='user-actions'>")
                        .append("<form method='post' action='/accion-usuario' class='inline-form'>").append(hiddenCsrf(s)).append("<input type='hidden' name='id' value='").append(u.getId())
                        .append("'><input type='hidden' name='accion' value='estado'><input type='hidden' name='activo' value='").append(!u.isActivo())
                        .append("'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='").append(u.isActivo()?"danger":"primary")
                        .append("' type='submit'>").append(u.isActivo()?"Desactivar":"Activar").append("</button></form>")
                        .append("<form method='post' action='/accion-usuario' class='inline-form'>").append(hiddenCsrf(s)).append("<input type='hidden' name='id' value='").append(u.getId())
                        .append("'><input type='hidden' name='accion' value='2fa'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='secondary' type='submit'>Restablecer 2FA</button></form>")
                        .append("<form method='post' action='/resetear-contrasena' class='inline-form'>").append(hiddenCsrf(s)).append("<input type='hidden' name='id' value='").append(u.getId())
                        .append("'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='secondary' type='submit' ").append(u.getId()==s.user.getId()?"disabled":"").append(">Clave temporal</button></form>")
                        .append("</div></td></tr>");
            }
            if(rows.isEmpty())rows.append("<tr><td colspan='6' class='empty'>No hay cuentas que coincidan con los filtros.</td></tr>");
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Usuarios</h1><p>Administra el acceso y el segundo factor de las cuentas.</p></div></div>"
                    +(message==null?"":"<div class='"+(success?"success":"alert")+"' role='status'>"+esc(message)+"</div>")
                    +"<section class='metrics grade-summary'>"+metric("Cuentas",allUsers.size())+metric("Activas",active)+metric("Inactivas",allUsers.size()-active)+"</section>"
                    +"<section class='card section-picker'><form method='get' action='/usuarios' class='subject-fields'><label>Buscar nombre o correo<input name='q' maxlength='100' value='"+esc(search)+"' placeholder='Nombre, apellido o correo'></label><label>Rol<select name='rol'><option value=''>Todos</option><option value='ADMIN' "+("ADMIN".equals(role)?"selected":"")+">Administrador</option><option value='PROFESOR' "+("PROFESOR".equals(role)?"selected":"")+">Profesor</option><option value='ESTUDIANTE' "+("ESTUDIANTE".equals(role)?"selected":"")+">Estudiante</option><option value='PADRE' "+("PADRE".equals(role)?"selected":"")+">Padre/tutor</option></select></label><label>Estado<select name='estado'><option value=''>Todos</option><option value='activa' "+("activa".equals(state)?"selected":"")+">Activas</option><option value='inactiva' "+("inactiva".equals(state)?"selected":"")+">Inactivas</option></select></label><button class='primary' type='submit'>Filtrar</button><a class='secondary-link' href='/usuarios'>Limpiar</a></form><p class='help-text'>Resultados: "+users.size()+" cuenta(s)</p><a class='primary link-button' href='/usuarios.csv?q="+java.net.URLEncoder.encode(search,StandardCharsets.UTF_8)+"&amp;rol="+role+"&amp;estado="+state+"'>Exportar resultados CSV</a></section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table users-table'><thead><tr><th>ID</th><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>"
                    +rows+"</tbody></table></div></section>";
            return shell("Usuarios",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Usuarios",alert("No se pudo cargar la lista de usuarios. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void userAction(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede modificar cuentas."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,usersAdminPage(s,null,"Confirma la acción antes de continuar."));return;}
        int userId;try{userId=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,usersAdminPage(s,null,"La cuenta seleccionada no es válida."));return;}
        String action=values.get("accion");
        try{
            if("estado".equals(action)){
                boolean active=Boolean.parseBoolean(values.getOrDefault("activo","false"));
                USERS.cambiarEstadoCuentaComoAdmin(s.user.getId(),userId,active);
            }else if("2fa".equals(action)){
                USERS.restablecer2FAComoAdmin(s.user.getId(),userId);
            }else{page(x,s,usersAdminPage(s,null,"La acción solicitada no es válida."));return;}
            redirect(x,"/usuarios?actualizado=1");
        }catch(PermisoDenegadoException e){page(x,s,usersAdminPage(s,null,e.getMessage()));}
        catch(SQLException e){page(x,s,usersAdminPage(s,null,"No se pudo actualizar la cuenta. Verifica la conexión con MySQL."));}
    }

    private static void downloadUsersCsv(HttpExchange x,WebSession s,String query)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){send(x,403,"text/plain; charset=utf-8","Solo un administrador puede exportar usuarios.");return;}
        String search=queryValue(query,"q");if(search==null)search="";if(search.length()>120)search=search.substring(0,120);
        String role=queryValue(query,"rol");if(role==null||!List.of("ADMIN","PROFESOR","ESTUDIANTE","PADRE").contains(role))role="";
        String state=queryValue(query,"estado");if(state==null||!List.of("activa","inactiva").contains(state))state="";
        try{
            List<Usuario> users=USERS.buscarTodos(search,role,state);StringBuilder csv=new StringBuilder("\uFEFFID,Nombre,Correo,Rol,Estado\r\n");
            for(Usuario user:users)csv.append(user.getId()).append(',').append(csv(user.getNombreCompleto())).append(',').append(csv(user.getCorreo())).append(',')
                    .append(csv(user.getRol()==null?"":user.getRol().name())).append(',').append(csv(user.isActivo()?"Activa":"Inactiva")).append("\r\n");
            x.getResponseHeaders().set("Content-Disposition","attachment; filename=\"usuarios-sige.csv\"");
            send(x,200,"text/csv; charset=utf-8",csv.toString());
        }catch(SQLException e){send(x,500,"text/plain; charset=utf-8","No se pudo generar el CSV. Verifica la conexión con MySQL.");}
    }

    private static void resetUserPassword(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede restablecer contraseñas."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,usersAdminPage(s,null,"Confirma el restablecimiento de contraseña."));return;}
        int id;try{id=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,usersAdminPage(s,null,"La cuenta seleccionada no es válida."));return;}
        String temporary=temporaryPassword();
        try{
            USERS.restablecerContrasenaComoAdmin(s.user.getId(),id,temporary);
            SESSIONS.entrySet().removeIf(entry->entry.getValue().user!=null&&entry.getValue().user.getId()==id);
            SESIONES_WEB.eliminarDeUsuario(id);
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Contraseña restablecida</h1></div></div>"
                    +"<section class='card subject-editor'><p>Se generó una clave temporal. Compártela con el usuario de forma segura; se le pedirá cambiarla al iniciar sesión.</p>"
                    +"<label>Clave temporal<input readonly value='"+esc(temporary)+"' onclick='this.select()'></label><p class='help-text'>Esta clave se muestra una sola vez. Si no la guardaste, genera otra desde Usuarios.</p>"
                    +"<div class='subject-actions'><a class='primary-link' href='/usuarios'>Volver a usuarios</a></div></section>";
            page(x,s,shell("Contraseña restablecida",body,true,s.user.getNombreCompleto()));
        }catch(PermisoDenegadoException e){page(x,s,usersAdminPage(s,null,e.getMessage()));}
        catch(SQLException e){page(x,s,usersAdminPage(s,null,"No se pudo restablecer la contraseña. Verifica la conexión con MySQL."));}
    }

    private static String temporaryPassword(){
        final String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        StringBuilder value=new StringBuilder(20);for(int i=0;i<20;i++)value.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));return value.toString();
    }

    private static String changeRolePage(WebSession s,String query,String message){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Cambiar rol",errorPage("Solo un administrador puede cambiar roles."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            if(message==null&&query!=null&&query.contains("notificacion=ok"))message="Perfil actualizado y notificación enviada al correo personal.";
            if(message==null&&query!=null&&query.contains("notificacion=fallida"))message="El perfil se guardó, pero no se pudo enviar el correo. Revisa la configuración SMTP.";
            if(message==null&&query!=null&&query.contains("notificacion=omitido"))message="Perfil creado. Se omitió el correo: por ahora solo se notifican cuentas personales Gmail, Outlook, Hotmail, Live o MSN.";
            List<Usuario> users=USERS.listarTodos();Integer selectedId=parseQueryInteger(query,"id");
            StringBuilder options=new StringBuilder("<option value=''>Selecciona una cuenta...</option>");Usuario selected=null;
            for(Usuario u:users){if(u.getId()==s.user.getId())continue;boolean isSelected=selectedId!=null&&selectedId==u.getId();if(isSelected)selected=u;
                options.append("<option value='").append(u.getId()).append("'").append(isSelected?" selected":"").append(">").append(esc(u.getNombreCompleto()+" · "+u.getCorreo()+" · "+(u.getRol()==null?"?":u.getRol().name()))).append("</option>");}
            String profile=selected==null?"": "<div class='role-profile-fields'><label>Matrícula (estudiante)<input name='matricula' maxlength='20'></label><label>Grado (estudiante)<input name='grado' maxlength='30'></label><label>Código de profesor<input name='codigoProfesor' maxlength='20'></label><label>Especialidad<input name='especialidad' maxlength='50'></label><label>Teléfono (padre/tutor)<input name='telefono' maxlength='20'></label><label>Ocupación (padre/tutor)<input name='ocupacion' maxlength='100'></label></div>";
            String picked=selected==null?"<p>Elige la cuenta que deseas actualizar. No puedes modificar tu propio rol.</p>":"<div class='notice'><b>Cuenta:</b> "+esc(selected.getNombreCompleto())+" · rol actual <b>"+esc(selected.getRol()==null?"?":selected.getRol().name())+"</b></div>";
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Cambiar rol de usuario</h1><p>Actualiza el tipo de cuenta y crea el perfil que corresponde.</p></div></div>"+(message==null?"":"<div class='alert' role='status'>"+esc(message)+"</div>")
                    +"<section class='card subject-editor'>"+picked+"<form method='get' action='/cambiar-rol' class='section-picker'><label>Cuenta<select name='id' required>"+options+"</select></label><button class='secondary' type='submit'>Seleccionar cuenta</button></form>"
                    +(selected==null?"":"<form method='post' action='/actualizar-rol'>"+hiddenCsrf(s)+"<input type='hidden' name='id' value='"+selected.getId()+"'><div class='subject-fields'><label>Nuevo rol<select name='rol' required><option value=''>Selecciona...</option><option value='ADMIN'>Administrador</option><option value='PROFESOR'>Profesor</option><option value='ESTUDIANTE'>Estudiante</option><option value='PADRE'>Padre o tutor</option></select></label></div>"+profile+"<p class='help-text'>Para estudiante, completa matrícula y grado. Para profesor, código y especialidad. El usuario deberá cambiar su contraseña al iniciar sesión.</p><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmo el cambio de rol</label><div class='subject-actions'><button class='primary' type='submit'>Cambiar rol</button><a class='secondary-link' href='/usuarios'>Volver a usuarios</a></div></form>")+"</section>";
            return shell("Cambiar rol",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Cambiar rol",alert("No se pudo cargar la lista de usuarios. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void changeRole(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede cambiar roles."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,changeRolePage(s,"id="+values.getOrDefault("id",""),"Confirma el cambio de rol antes de continuar."));return;}
        int id;try{id=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,changeRolePage(s,null,"La cuenta seleccionada no es válida."));return;}
        Rol role;try{role=Rol.valueOf(values.getOrDefault("rol", ""));}catch(IllegalArgumentException e){page(x,s,changeRolePage(s,"id="+id,"Selecciona un rol válido."));return;}
        try{
            if(role==Rol.ESTUDIANTE&&values.get("matricula")!=null&&!values.get("matricula").isBlank()&&USERS.existeMatricula(values.get("matricula").trim())){page(x,s,changeRolePage(s,"id="+id,"Esa matrícula ya está registrada."));return;}
            if(role==Rol.PROFESOR&&values.get("codigoProfesor")!=null&&!values.get("codigoProfesor").isBlank()&&USERS.existeCodigoProfesor(values.get("codigoProfesor").trim())){page(x,s,changeRolePage(s,"id="+id,"Ese código de profesor ya está registrado."));return;}
            Usuario target=USERS.listarTodos().stream().filter(u->u.getId()==id).findFirst().orElseThrow(()->new PermisoDenegadoException("La cuenta seleccionada ya no existe."));
            Rol previousRole=target.getRol();
            USERS.cambiarRolComoAdmin(s.user.getId(),id,role,values.get("matricula"),values.get("grado"),values.get("codigoProfesor"),values.get("especialidad"),values.get("telefono"),values.get("ocupacion"));
            if(previousRole!=role&&(role==Rol.ESTUDIANTE||role==Rol.PADRE)){
                if(!CorreoNotificaciones.aceptaDestinatario(target.getCorreo())){redirect(x,"/cambiar-rol?id="+id+"&notificacion=omitido");return;}
                try{
                    if(role==Rol.ESTUDIANTE)CorreoNotificaciones.enviarPerfilEstudiante(new Estudiante(target.getId(),target.getNombre(),target.getApellido(),target.getCorreo(),null,values.get("matricula"),values.get("grado"),0));
                    else CorreoNotificaciones.enviarPerfilPadre(new Padre(target.getId(),target.getNombre(),target.getApellido(),target.getCorreo(),null,0,values.get("telefono"),values.get("ocupacion")));
                    redirect(x,"/cambiar-rol?id="+id+"&notificacion=ok");
                }catch(Exception mailError){System.err.println("No se pudo enviar aviso de creación de perfil estudiante/padre.");redirect(x,"/cambiar-rol?id="+id+"&notificacion=fallida");}
            }else redirect(x,"/usuarios?actualizado=1");

        }catch(PermisoDenegadoException e){page(x,s,changeRolePage(s,"id="+id,e.getMessage()));}
        catch(SQLException e){page(x,s,changeRolePage(s,"id="+id,"No se pudo cambiar el rol. Verifica los datos y la conexión con MySQL."));}
    }

    private static String parentLinksAdminPage(WebSession s,String query,String message){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Vincular padres e hijos",errorPage("Solo un administrador puede gestionar vínculos."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            VinculacionDAO dao=new VinculacionDAO();List<Padre> parents=dao.listarPadres();List<Estudiante> students=dao.listarEstudiantes();List<VinculoPadreEstudiante> links=dao.listarVinculos();
            boolean success=message==null&&query!=null&&(query.contains("resultado=creado")||query.contains("resultado=eliminado"));
            if(message==null&&query!=null&&query.contains("resultado=creado"))message="El vínculo se creó correctamente.";
            if(message==null&&query!=null&&query.contains("resultado=eliminado"))message="El vínculo se eliminó correctamente.";
            if(message==null&&query!=null&&query.contains("resultado=duplicado"))message="Ese padre/tutor ya está vinculado con el estudiante.";
            StringBuilder parentOptions=new StringBuilder("<option value=''>Selecciona un padre o tutor...</option>");
            for(Padre parent:parents)parentOptions.append("<option value='").append(parent.getIdPadre()).append("'>")
                    .append(esc(parent.getNombreCompleto()+" · "+parent.getCorreo())).append("</option>");
            StringBuilder studentOptions=new StringBuilder("<option value=''>Selecciona un estudiante...</option>");
            for(Estudiante student:students)studentOptions.append("<option value='").append(student.getId()).append("'>")
                    .append(esc(student.getNombreCompleto()+" · "+student.getMatricula())).append("</option>");
            StringBuilder rows=new StringBuilder();
            for(VinculoPadreEstudiante link:links)rows.append("<tr><td>").append(esc(link.padre())).append("</td><td><b>").append(esc(link.estudiante()))
                    .append("</b></td><td>").append(esc(link.matricula())).append("</td><td>").append(esc(link.parentesco()))
                    .append("</td><td><form method='post' action='/eliminar-vinculo' class='inline-form'>").append(hiddenCsrf(s))
                    .append("<input type='hidden' name='padre' value='").append(link.idPadre()).append("'><input type='hidden' name='estudiante' value='")
                    .append(link.idEstudiante()).append("'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='danger' type='submit'>Quitar vínculo</button></form></td></tr>");
            if(rows.isEmpty())rows.append("<tr><td colspan='7' class='empty'>No hay vínculos registrados.</td></tr>");
            String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Vincular padres e hijos</h1><p>Asocia cuentas de padre o tutor con estudiantes registrados.</p></div></div>"
                    +(message==null?"":"<div class='"+(success?"success":"alert")+"' role='status'>"+esc(message)+"</div>")
                    +"<section class='card subject-editor'><h2>Crear vínculo</h2><form method='post' action='/crear-vinculo'>"+hiddenCsrf(s)
                    +"<div class='subject-fields'><label>Padre o tutor<select name='padre' required>"+parentOptions+"</select></label><label>Estudiante<select name='estudiante' required>"+studentOptions+"</select></label>"
                    +"<label>Parentesco<input name='parentesco' maxlength='50' value='PADRE/MADRE/TUTOR'></label></div><div class='subject-actions'><button class='primary' type='submit' "+(parents.isEmpty()||students.isEmpty()?"disabled":"")+">Vincular</button></div></form></section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Padre / tutor</th><th>Estudiante</th><th>Matrícula</th><th>Parentesco</th><th>Acción</th></tr></thead><tbody>"+rows+"</tbody></table></div></section>";
            return shell("Vincular padres e hijos",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Vincular padres e hijos",alert("No se pudieron cargar los vínculos. Verifica que exista la tabla estudiante_padre y la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static void createParentLink(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede crear vínculos."));return;}
        int parentId,studentId;
        try{parentId=Integer.parseInt(values.getOrDefault("padre",""));studentId=Integer.parseInt(values.getOrDefault("estudiante",""));}
        catch(NumberFormatException e){page(x,s,parentLinksAdminPage(s,null,"Selecciona un padre/tutor y un estudiante válidos."));return;}
        String relation=values.getOrDefault("parentesco","").trim();
        if(relation.length()>50){page(x,s,parentLinksAdminPage(s,null,"El parentesco admite máximo 50 caracteres."));return;}
        try{
            VinculacionDAO dao=new VinculacionDAO();
            boolean validParent=dao.listarPadres().stream().anyMatch(p->p.getIdPadre()==parentId);
            boolean validStudent=dao.listarEstudiantes().stream().anyMatch(e->e.getId()==studentId);
            if(!validParent||!validStudent){page(x,s,parentLinksAdminPage(s,null,"Las cuentas seleccionadas ya no están disponibles."));return;}
            boolean created=dao.vincular(parentId,studentId,relation);
            redirect(x,created?"/vinculaciones?resultado=creado":"/vinculaciones?resultado=duplicado");
        }catch(SQLException e){page(x,s,parentLinksAdminPage(s,null,"No se pudo crear el vínculo. Verifica la conexión y que exista la tabla estudiante_padre."));}
    }

    private static void deleteParentLink(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede quitar vínculos."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,parentLinksAdminPage(s,null,"Confirma la eliminación del vínculo."));return;}
        int parentId,studentId;try{parentId=Integer.parseInt(values.getOrDefault("padre",""));studentId=Integer.parseInt(values.getOrDefault("estudiante",""));}
        catch(NumberFormatException e){page(x,s,parentLinksAdminPage(s,null,"El vínculo seleccionado no es válido."));return;}
        try{new VinculacionDAO().desvincular(parentId,studentId);redirect(x,"/vinculaciones?resultado=eliminado");}
        catch(SQLException e){page(x,s,parentLinksAdminPage(s,null,"No se pudo quitar el vínculo. Verifica la conexión con MySQL."));}
    }

    private static String createTeacherPage(WebSession s,String query,String message){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Crear profesor",errorPage("Solo un administrador puede crear cuentas de profesor."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        boolean success=message==null&&query!=null&&query.contains("creado=1");
        if(success)message=query.contains("notificacion=ok")
                ? "Perfil creado y notificación enviada al correo personal del profesor."
                : query.contains("notificacion=omitido")
                    ? "Perfil creado. Se omitió el correo: por ahora solo se notifican cuentas personales Gmail, Outlook, Hotmail, Live o MSN."
                    : "Perfil creado, pero no se pudo enviar el correo. Revisa la configuración SMTP.";
        String body="<div class='page-heading'><div><div class='eyebrow'>ADMINISTRACIÓN</div><h1>Crear profesor</h1><p>Registra una cuenta docente con contraseña inicial.</p></div></div>"
                +(message==null?"":"<div class='"+(success?"success":"alert")+"' role='status'>"+esc(message)+"</div>")
                +"<section class='card subject-editor'><form method='post' action='/guardar-profesor'>"+hiddenCsrf(s)
                +"<div class='subject-fields'><label>Nombre<input name='nombre' maxlength='50' autocomplete='given-name' required></label><label>Apellido<input name='apellido' maxlength='50' autocomplete='family-name' required></label>"
                +"<label>Correo personal<input name='correo' type='email' maxlength='100' autocomplete='email' required></label><label>Código de profesor<input name='codigo' maxlength='10' required></label>"
                +"<label>Especialidad<input name='especialidad' maxlength='50' required></label><label>Contraseña inicial<input name='contrasena' type='password' minlength='8' autocomplete='new-password' required></label>"
                +"<label>Confirmar contraseña<input name='confirmar' type='password' minlength='8' autocomplete='new-password' required></label></div><p class='hint'>La cuenta deberá cambiar esta contraseña al iniciar sesión por primera vez.</p>"
                +"<div class='subject-actions'><button class='primary' type='submit'>Crear cuenta de profesor</button></div></form></section>";
        return shell("Crear profesor",body,true,s.user.getNombreCompleto());
    }

    private static void createTeacher(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede crear cuentas de profesor."));return;}
        String name=values.getOrDefault("nombre","").trim(),surname=values.getOrDefault("apellido","").trim();
        String email=values.getOrDefault("correo","").trim().toLowerCase(java.util.Locale.ROOT);
        String code=values.getOrDefault("codigo","").trim(),specialty=values.getOrDefault("especialidad","").trim();
        String password=values.getOrDefault("contrasena",""),confirm=values.getOrDefault("confirmar","");
        String error=null;
        if(name.isBlank()||surname.isBlank()||email.isBlank()||code.isBlank()||specialty.isBlank()||password.isBlank()||confirm.isBlank())error="Completa todos los campos.";
        else if(name.length()>50||surname.length()>50||email.length()>100||code.length()>20||specialty.length()>100)error="Revisa los límites: nombre/apellido 50, correo 100, código 20 y especialidad 100 caracteres.";
        else if(!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))error="Ingresa un correo electrónico válido.";
        else if(PoliticaContrasena.validar(password)!=null)error=PoliticaContrasena.validar(password);
        else if(!password.equals(confirm))error="Las contraseñas no coinciden.";
        if(error!=null){page(x,s,createTeacherPage(s,null,error));return;}
        try{
            if(USERS.existeCorreo(email)){page(x,s,createTeacherPage(s,null,"Ese correo ya está registrado."));return;}
            if(USERS.existeCodigoProfesor(code)){page(x,s,createTeacherPage(s,null,"Ese código de profesor ya está registrado."));return;}
            Profesor teacher=new Profesor(0,name,surname,email,null,code,specialty);
            USERS.registrarProfesorComoAdmin(teacher,password,s.user.getId());
            String aviso="fallida";
            if(!CorreoNotificaciones.aceptaDestinatario(teacher.getCorreo())){redirect(x,"/crear-profesor?creado=1&notificacion=omitido");return;}
            try {
                CorreoNotificaciones.enviarPerfilProfesor(teacher);
                aviso="ok";
            } catch (Exception envio) {
                System.err.println("No se pudo enviar la notificación de creación del perfil docente.");
            }
            redirect(x,"/crear-profesor?creado=1&notificacion="+aviso);
        }catch(PermisoDenegadoException e){page(x,s,createTeacherPage(s,null,e.getMessage()));}
        catch(java.sql.SQLIntegrityConstraintViolationException e){page(x,s,createTeacherPage(s,null,"El correo o código de profesor ya está en uso."));}
        catch(SQLException e){page(x,s,createTeacherPage(s,null,"No se pudo crear la cuenta. Verifica la conexión con MySQL."));}
    }

    private static String parentChildrenPage(WebSession s){
        if(s.user==null||s.user.getRol()!=Rol.PADRE)return shell("Mis hijos",errorPage("Esta página está disponible para padres y tutores."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            List<Estudiante> children=new VinculacionDAO().listarHijos(s.user.getId());
            StringBuilder cards=new StringBuilder();
            for(Estudiante child:children){
                String childId=Integer.toString(child.getId());
                cards.append("<article class='card child-card'><div class='child-card-heading'><div><span class='eyebrow'>ESTUDIANTE VINCULADO</span><h2>")
                        .append(esc(child.getNombreCompleto())).append("</h2></div><span class='role-badge'>").append(esc(child.getMatricula())).append("</span></div>")
                        .append("<div class='profile-fields'>").append(profileField("Grado",child.getGrado()))
                        .append(profileField("Promedio académico",String.format(java.util.Locale.ROOT,"%.2f",child.getPromedioAcademico())))
                        .append("</div><div class='child-links'><a href='/materias?estudiante=").append(childId)
                        .append("'>Materias</a><a href='/boleta?estudiante=").append(childId).append("'>Boleta</a><a href='/asistencia?estudiante=")
                        .append(childId).append("'>Asistencia</a></div></article>");
            }
            if(cards.isEmpty())cards.append("<section class='card'>").append(alert("Tu cuenta todavía no tiene estudiantes vinculados. Contacta al administrador de SIGE."))
                    .append("</section>");
            String body="<div class='page-heading'><div><div class='eyebrow'>FAMILIA</div><h1>Mis hijos</h1><p>Información de los estudiantes vinculados a tu cuenta.</p></div><span class='role-badge'>"+children.size()+" estudiante(s)</span></div>"
                    +"<div class='child-cards'>"+cards+"</div>";
            return shell("Mis hijos",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Mis hijos",alert("No se pudo cargar la lista. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static String sessionHistoryPage(WebSession s,String query){
        if(s.user==null||s.user.getRol()!=Rol.ADMIN)return shell("Historial de sesiones",errorPage("Solo un administrador puede consultar el historial de sesiones."),s.user!=null,s.user==null?"":s.user.getNombreCompleto());
        try{
            String search=queryValue(query,"q");if(search==null)search="";if(search.length()>120)search=search.substring(0,120);
            String state=queryValue(query,"estado");if(!"Activa".equals(state)&&!"Cerrada".equals(state))state="";
            java.time.LocalDate from=parseDate(queryValue(query,"desde"),null),to=parseDate(queryValue(query,"hasta"),null);
            if(from!=null&&to!=null&&from.isAfter(to))return shell("Historial de sesiones",errorPage("La fecha inicial no puede ser posterior a la fecha final."),true,s.user.getNombreCompleto());
            String fromValue=from==null?null:from.toString(),toValue=to==null?null:to.toString();
            List<SesionRegistrada> sessions=new SesionDAO().buscarRecientes(s.user.getId(),search,state,fromValue,toValue);
            StringBuilder rows=new StringBuilder();
            boolean justClosed="1".equals(queryValue(query,"cerrada"));
            for(SesionRegistrada row:sessions){rows.append("<tr><td>").append(row.idSesion()).append("</td><td><b>").append(esc(row.usuario()))
                    .append("</b></td><td>").append(esc(row.correo())).append("</td><td>").append(esc(row.inicio())).append("</td><td>")
                    .append(esc(row.cierre())).append("</td><td>").append(esc(row.direccionIP())).append("</td><td><span class='state-pill ")
                    .append("Activa".equals(row.estado())?"account-active":"account-disabled").append("'>").append(esc(row.estado())).append("</span></td><td>");
                if("Activa".equals(row.estado())&&!(s.login!=null&&s.login.getIdSesion()==row.idSesion()))rows.append("<form method='post' action='/cerrar-sesion-admin' class='inline-form'>").append(hiddenCsrf(s)).append("<input type='hidden' name='id' value='").append(row.idSesion()).append("'><label class='delete-confirm'><input type='checkbox' name='confirmar' value='si' required>Confirmar</label><button class='danger' type='submit'>Cerrar sesión</button></form>");
                else rows.append("—");
                rows.append("</td></tr>");
            }
            if(rows.isEmpty())rows.append("<tr><td colspan='8' class='empty'>No hay sesiones registradas.</td></tr>");
            String body="<div class='page-heading'><div><div class='eyebrow'>SEGURIDAD</div><h1>Historial de sesiones</h1><p>Hasta 500 accesos recientes registrados por SIGE.</p></div><a class='text-link' href='/historial-sesiones'>Actualizar</a></div>"
                    +(justClosed?"<div class='success' role='status'>La sesión fue cerrada.</div>":"")
                    +"<section class='card section-picker'><form method='get' action='/historial-sesiones' class='subject-fields'><label>Buscar usuario o correo<input name='q' maxlength='100' value='"+esc(search)+"' placeholder='Nombre, apellido o correo'></label><label>Estado<select name='estado'><option value=''>Todos</option><option value='Activa' "+("Activa".equals(state)?"selected":"")+">Activas</option><option value='Cerrada' "+("Cerrada".equals(state)?"selected":"")+">Cerradas</option></select></label><label>Desde<input type='date' name='desde' value='"+(from==null?"":from)+"'></label><label>Hasta<input type='date' name='hasta' value='"+(to==null?"":to)+"'></label><button class='primary' type='submit'>Filtrar</button><a class='secondary-link' href='/historial-sesiones'>Limpiar</a></form><a class='primary link-button' href='/historial-sesiones.csv?q="+java.net.URLEncoder.encode(search,StandardCharsets.UTF_8)+"&amp;estado="+java.net.URLEncoder.encode(state,StandardCharsets.UTF_8)+"&amp;desde="+(from==null?"":from)+"&amp;hasta="+(to==null?"":to)+"'>Exportar resultados CSV</a></section>"
                    +"<section class='metrics grade-summary'>"+metric("Sesiones mostradas",sessions.size())+metric("Activas",sessions.stream().filter(r->"Activa".equals(r.estado())).count())
                    +metric("Cerradas",sessions.stream().filter(r->"Cerrada".equals(r.estado())).count())+"</section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table session-table'><thead><tr><th>ID</th><th>Usuario</th><th>Correo</th><th>Inicio</th><th>Cierre</th><th>Dirección IP</th><th>Estado</th><th>Acción</th></tr></thead><tbody>"+rows+"</tbody></table></div></section>";
            return shell("Historial de sesiones",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Historial de sesiones",alert("No se pudo cargar el historial. Verifica la conexión y los permisos."),true,s.user.getNombreCompleto());}
    }

    private static void downloadSessionHistoryCsv(HttpExchange x,WebSession s,String query)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){send(x,403,"text/plain; charset=utf-8","Solo un administrador puede exportar el historial.");return;}
        String search=queryValue(query,"q");if(search==null)search="";if(search.length()>120)search=search.substring(0,120);
        String state=queryValue(query,"estado");if(!"Activa".equals(state)&&!"Cerrada".equals(state))state="";
        java.time.LocalDate from=parseDate(queryValue(query,"desde"),null),to=parseDate(queryValue(query,"hasta"),null);
        if(from!=null&&to!=null&&from.isAfter(to)){send(x,400,"text/plain; charset=utf-8","La fecha inicial no puede ser posterior a la fecha final.");return;}
        try{
            List<SesionRegistrada> sessions=new SesionDAO().buscarRecientes(s.user.getId(),search,state,from==null?null:from.toString(),to==null?null:to.toString());
            StringBuilder csv=new StringBuilder("\uFEFFID,Usuario,Correo,Inicio,Cierre,Dirección IP,Estado\r\n");
            for(SesionRegistrada row:sessions)csv.append(row.idSesion()).append(',').append(csv(row.usuario())).append(',').append(csv(row.correo())).append(',').append(csv(row.inicio())).append(',').append(csv(row.cierre())).append(',').append(csv(row.direccionIP())).append(',').append(csv(row.estado())).append("\r\n");
            x.getResponseHeaders().set("Content-Disposition","attachment; filename=\"historial-sesiones-sige.csv\"");send(x,200,"text/csv; charset=utf-8",csv.toString());
        }catch(SQLException e){send(x,500,"text/plain; charset=utf-8","No se pudo exportar el historial. Verifica la conexión con MySQL.");}
    }

    private static void closeSessionAsAdmin(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ADMIN){page(x,s,errorPage("Solo un administrador puede cerrar sesiones ajenas."));return;}
        if(!"si".equals(values.get("confirmar"))){page(x,s,sessionHistoryPage(s,null));return;}
        int sessionId;try{sessionId=Integer.parseInt(values.getOrDefault("id",""));}catch(NumberFormatException e){page(x,s,errorPage("El identificador de sesión no es válido."));return;}
        if(sessionId<1||s.login!=null&&s.login.getIdSesion()==sessionId){page(x,s,errorPage("No puedes cerrar la sesión que estás usando ahora."));return;}
        try{
            new SesionDAO().cerrarComoAdmin(s.user.getId(),sessionId);
            SESSIONS.entrySet().removeIf(entry->entry.getValue().login!=null&&entry.getValue().login.getIdSesion()==sessionId);
            SESIONES_WEB.eliminarPorLogin(sessionId);
            redirect(x,"/historial-sesiones?cerrada=1");
        }catch(PermisoDenegadoException e){page(x,s,sessionHistoryPage(s,null));}
        catch(SQLException e){page(x,s,errorPage("No se pudo cerrar la sesión. Verifica la conexión con MySQL."));}
    }

    private static void enroll(HttpExchange x,WebSession s,Map<String,String> values)throws IOException{
        if(s.user==null||s.user.getRol()!=Rol.ESTUDIANTE){page(x,s,errorPage("Solo los estudiantes pueden inscribirse desde esta página."));return;}
        int sectionId;String period=values.getOrDefault("periodo","").trim();
        try{sectionId=Integer.parseInt(values.getOrDefault("id_seccion",""));}
        catch(NumberFormatException e){page(x,s,enrollmentPage(s,null,"La sección seleccionada no es válida."));return;}
        if(sectionId<1||period.isBlank()||period.length()>20){page(x,s,enrollmentPage(s,null,"Indica un período válido (máximo 20 caracteres)."));return;}
        try{
            new InscripcionDAO().inscribir(s.user.getId(),sectionId,period);
            redirect(x,"/inscripciones?resultado=ok");
        }catch(InscripcionInvalidaException e){page(x,s,enrollmentPage(s,null,e.getMessage()));}
        catch(SQLException e){page(x,s,enrollmentPage(s,null,"No se pudo guardar la inscripción. Verifica la conexión con MySQL."));}
    }

    private static String attendancePage(WebSession s,String query){
        if(s.user==null)return shell("Asistencia",errorPage("Inicia sesión para consultar la asistencia."),false,"");
        try{
            int studentId=s.user.getId();String studentName=s.user.getNombreCompleto();List<Estudiante> children=List.of();
            if(s.user.getRol()==Rol.PADRE){
                children=new VinculacionDAO().listarHijos(s.user.getId());
                if(children.isEmpty())return shell("Asistencia",errorPage("Tu cuenta todavía no tiene estudiantes vinculados."),true,s.user.getNombreCompleto());
                Integer chosen=parseStudentId(query);
                if(chosen==null)return shell("Asistencia de mis hijos",childPicker(children,"/asistencia","Selecciona un hijo para consultar su asistencia."),true,s.user.getNombreCompleto());
                Estudiante linked=children.stream().filter(e->e.getId()==chosen).findFirst().orElse(null);
                if(linked==null)return shell("Asistencia",errorPage("Ese estudiante no está vinculado a tu cuenta."),true,s.user.getNombreCompleto());
                studentId=linked.getId();studentName=linked.getNombreCompleto();
            }else if(s.user.getRol()!=Rol.ESTUDIANTE){
                return shell("Asistencia",errorPage("La consulta web de asistencia está habilitada para estudiantes y padres/tutores."),true,s.user.getNombreCompleto());
            }
            AsistenciaDAO dao=new AsistenciaDAO();
            List<ResumenAsistencia> rows=s.user.getRol()==Rol.PADRE
                    ?dao.listarResumenParaPadre(s.user.getId(),studentId):dao.listarResumenPorEstudiante(studentId);
            int classes=rows.stream().mapToInt(ResumenAsistencia::clasesRegistradas).sum();
            int present=rows.stream().mapToInt(ResumenAsistencia::presentes).sum();
            int absent=rows.stream().mapToInt(ResumenAsistencia::ausentes).sum();
            double percent=classes==0?0:present*100.0/classes;
            StringBuilder table=new StringBuilder();
            for(ResumenAsistencia row:rows)table.append("<tr><td>").append(esc(row.periodo())).append("</td><td><b>")
                    .append(esc(row.codigoAsignatura())).append("</b><br>").append(esc(row.asignatura())).append("</td><td>")
                    .append(row.clasesRegistradas()).append("</td><td>").append(row.presentes()).append("</td><td>")
                    .append(row.ausentes()).append("</td><td><b>").append(String.format(java.util.Locale.ROOT,"%.1f%%",row.porcentaje()))
                    .append("</b></td></tr>");
            if(table.isEmpty())table.append("<tr><td colspan='6' class='empty'>Todavía no hay registros de asistencia para mostrar.</td></tr>");
            String picker=s.user.getRol()==Rol.PADRE?childPicker(children,"/asistencia","Estudiante: "+studentName):"";
            String body="<div class='page-heading'><div><div class='eyebrow'>CONSULTA ACADÉMICA</div><h1>Asistencia</h1><p>Registro de clases por período · "+esc(studentName)+".</p></div></div>"
                    +picker+"<section class='metrics grade-summary'>"+metric("Clases registradas",classes)+metric("Presentes",present)
                    +metric("Ausencias",absent)+metric("Asistencia",String.format(java.util.Locale.ROOT,"%.1f%%",percent))+"</section>"
                    +"<section class='card table-card'><div class='table-wrap'><table class='grade-table'><thead><tr><th>Período</th><th>Asignatura</th><th>Clases registradas</th><th>Presentes</th><th>Ausencias</th><th>Porcentaje</th></tr></thead><tbody>"
                    +table+"</tbody></table></div></section>";
            return shell("Asistencia",body,true,s.user.getNombreCompleto());
        }catch(SQLException e){return shell("Asistencia",alert("No se pudo consultar la asistencia. Verifica la conexión con MySQL."),true,s.user.getNombreCompleto());}
    }

    private static String shell(String title, String body, boolean logged, String name) {
        String stop = Boolean.TRUE.equals(LOCAL_REQUEST.get())&&Boolean.TRUE.equals(ADMIN_REQUEST.get())
                ? "<form method='post' action='/shutdown' class='stop-form'><input type='hidden' name='_csrf' value='"+esc(currentCsrf())+"'><button class='danger' type='submit'>Detener servidor</button></form>" : "";
        String role=ROLE_REQUEST.get();
        String academicLink="ESTUDIANTE".equals(role)?"<a href='/aula'>Aula y tareas</a><a href='/materias'>Mis materias</a><a href='/inscripciones'>Inscripciones</a><a href='/boleta'>Mi boleta</a><a href='/asistencia'>Mi asistencia</a>":"PADRE".equals(role)?"<a href='/mis-hijos'>Mis hijos</a><a href='/materias'>Materias de mis hijos</a><a href='/boleta'>Boleta de mis hijos</a><a href='/asistencia'>Asistencia de mis hijos</a>":"PROFESOR".equals(role)?"<a href='/aula'>Aula y tareas</a><a href='/calificaciones'>Calificaciones</a><a href='/asistencia-docente'>Tomar asistencia</a><a href='/reporte-asistencia'>Reporte de asistencia</a>":"ADMIN".equals(role)?"<a href='/usuarios'>Usuarios</a><a href='/solicitudes'>Solicitudes</a><a href='/cambiar-rol'>Cambiar rol</a><a href='/crear-profesor'>Crear profesor</a><a href='/vinculaciones'>Vincular padres</a><a href='/asignaturas'>Asignaturas</a><a href='/curriculo'>Grados y currículo</a><a href='/secciones'>Secciones</a><a href='/calificaciones'>Calificaciones</a><a href='/reporte-asistencia'>Reporte de asistencia</a><a href='/historial-sesiones'>Historial</a>":"";
        String nav = logged ? "<header class='topbar'><a class='brand' href='/'><img class='brand-logo' src='/logo.svg' alt='' width='36' height='36'>SIGE</a><button class='menu-toggle' type='button' aria-controls='main-navigation' aria-expanded='false' aria-label='Abrir menú'>☰</button><nav class='top-nav' id='main-navigation' aria-label='Navegación principal'><a href='/'>Inicio</a><a href='/perfil'>Mi perfil</a>"+academicLink+"<a href='/reporte-inscripciones'>Reporte</a></nav><div class='account'><span>"+esc(name)+"</span><form method='post' action='/logout'>"
                +"<input type='hidden' name='_csrf' value='"+esc(currentCsrf())+"'><button class='quiet' type='submit'>Cerrar sesión</button></form>"+stop+"</div></header>"
                : "<header class='topbar'><a class='brand' href='/'><img class='brand-logo' src='/logo.svg' alt='' width='36' height='36'>SIGE</a><span class='top-note'>Sistema de gestión educativa</span></header>";
        nav = Accesibilidad.marcarPaginaActual(nav, PATH_REQUEST.get());
        body = Accesibilidad.mejorar(body);
        return "<!doctype html><html lang='es'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                +"<meta name='theme-color' content='#17324d'><meta name='apple-mobile-web-app-capable' content='yes'><title>"+esc(title)+" · SIGE</title>"
                +"<link rel='manifest' href='/manifest.webmanifest'><link rel='icon' href='/icon.svg' type='image/svg+xml'><link rel='stylesheet' href='/styles.css'></head><body>"
                +"<a class='skip-link' href='#contenido'>Saltar al contenido</a>"
                +nav+"<main class='layout' id='contenido' tabindex='-1'>"+body+"</main><footer> SIGE · Sistema de Gestión Educativa</footer><script src='/pwa.js' defer></script></body></html>";
    }

    // The request thread holds the currently rendered CSRF token in this ThreadLocal.
    private static final ThreadLocal<String> RENDER_CSRF = new ThreadLocal<>();
    private static String currentCsrf() { String v=RENDER_CSRF.get(); return v == null ? "" : v; }
    private static String hiddenCsrf(WebSession s) { RENDER_CSRF.set(s.csrf); return "<input type='hidden' name='_csrf' value='"+esc(s.csrf)+"'>"; }
    private static String metric(String label, long value) { return "<article class='metric'><span>"+label+"</span><strong>"+value+"</strong></article>"; }
    private static String metric(String label,String value){return "<article class='metric'><span>"+esc(label)+"</span><strong>"+esc(value)+"</strong></article>";}
    private static String metricsFor(Usuario user,DashboardDAO.Indicadores i){
        return switch(user.getRol()){
            case ADMIN -> metric("Usuarios activos",i.usuariosActivos())+metric("Inscripciones",i.inscripciones())
                    +metric("Secciones",i.secciones())+metric("Evaluaciones",i.evaluaciones())
                    +metric("Promedio general",String.format(java.util.Locale.ROOT,"%.2f",i.promedio()))+metric("Asistencias registradas",i.asistencias());
            case PROFESOR -> metric("Estudiantes",i.estudiantes())+metric("Mis secciones",i.secciones())
                    +metric("Inscripciones",i.inscripciones())+metric("Evaluaciones",i.evaluaciones())
                    +metric("Promedio de notas",String.format(java.util.Locale.ROOT,"%.2f",i.promedio()))+metric("Asistencias registradas",i.asistencias());
            case ESTUDIANTE -> metric("Materias inscritas",i.inscripciones())+metric("Materias evaluadas",i.evaluaciones())
                    +metric("Mi promedio",String.format(java.util.Locale.ROOT,"%.2f",i.promedio()))+metric("Asistencias registradas",i.asistencias());
            case PADRE -> metric("Hijos vinculados",i.hijos())+metric("Inscripciones",i.inscripciones())
                    +metric("Secciones",i.secciones())+metric("Evaluaciones",i.evaluaciones())
                    +metric("Promedio académico",String.format(java.util.Locale.ROOT,"%.2f",i.promedio()))+metric("Asistencias registradas",i.asistencias());
        };
    }
    private static String profileField(String label,String value){return "<div class='profile-line'><span>"+esc(label)+"</span><b>"+esc(value==null||value.isBlank()?"No registrado":value)+"</b></div>";}
    private static String alert(String text) { return "<div class='alert' role='alert'>"+esc(text)+"</div>"; }
    private static String errorPage(String message) { return "<section class='card'>"+alert(message)+"<a href='/'>Volver al inicio</a></section>"; }

    private static void page(HttpExchange x, WebSession s, String html) throws IOException {
        RENDER_CSRF.set(s.csrf);
        Headers h=x.getResponseHeaders(); h.set("Cache-Control","no-store"); h.set("X-Content-Type-Options","nosniff");
        h.set("X-Frame-Options","DENY"); h.set("Referrer-Policy","same-origin");
        h.set("Content-Security-Policy","default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");
        send(x,200,"text/html; charset=utf-8",html);
        RENDER_CSRF.remove();
        LOCAL_REQUEST.remove();ADMIN_REQUEST.remove();
        ROLE_REQUEST.remove();
    }
    /** Ilustración del dashboard con el anillo de progreso ajustado al porcentaje real de materias aprobadas. */
    private static String heroArt(int pct, boolean hasData) {
        try (var in = WebApp.class.getResourceAsStream("/com/unev/sige/web/dashboard-hero.svg")) {
            if (in == null) return "";
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            double len = 2 * Math.PI * 32;
            String dash = String.format(java.util.Locale.ROOT, "%.1f %.1f", len * Math.max(0, Math.min(100, pct)) / 100.0, len);
            return svg.replace("stroke-dasharray=\"150 201\"", "stroke-dasharray=\"" + dash + "\"")
                    .replace(">75%<", ">" + (hasData ? pct + "%" : "–") + "<")
                    .replace("<svg ", "<svg class='hero-art' ");
        } catch (IOException e) {
            return "";
        }
    }

    private static void svgResource(HttpExchange x, String path) throws IOException {
        try (var in = WebApp.class.getResourceAsStream("/com/unev/sige/web" + path)) {
            if (in == null) { send(x, 404, "text/plain", "No encontrado"); return; }
            x.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
            send(x, 200, "image/svg+xml; charset=utf-8", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static void css(HttpExchange x) throws IOException {
        String css;
        try (var in=WebApp.class.getResourceAsStream("/com/unev/sige/web/styles.css")) {
            if(in==null){send(x,404,"text/plain","No se encontró la hoja de estilos");return;}
            css=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        send(x,200,"text/css; charset=utf-8",css);
    }
    private static String manifest(){return "{\"id\":\"/\",\"name\":\"SIGE - Sistema de Gestión Educativa\",\"short_name\":\"SIGE\",\"description\":\"Portal académico SIGE\",\"start_url\":\"/\",\"scope\":\"/\",\"display\":\"standalone\",\"background_color\":\"#f3f6fa\",\"theme_color\":\"#17324d\",\"icons\":[{\"src\":\"/icon.svg\",\"sizes\":\"any\",\"type\":\"image/svg+xml\",\"purpose\":\"any maskable\"}]}";}
    private static String serviceWorker(){return "const CACHE='sige-ui-v5';const ASSETS=['/styles.css','/icon.svg','/logo.svg','/login-hero.svg','/dashboard-hero.svg','/manifest.webmanifest'];self.addEventListener('install',e=>e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)).then(()=>self.skipWaiting())));self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));self.addEventListener('fetch',e=>{const u=new URL(e.request.url);if(e.request.method!=='GET'||u.origin!==location.origin||u.pathname==='/'||u.pathname==='/2fa'||u.pathname==='/password')return;e.respondWith(fetch(e.request).catch(()=>caches.match(e.request)));});";}
    private static String icon(){return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512' role='img' aria-label='SIGE'> <rect width='512' height='512' rx='112' fill='#17324d'/> <path d='M256 196C204 166 146 164 100 184v176c46-18 104-16 156 14z' fill='#ffffff'/> <path d='M256 196c52-30 110-32 156-12v176c-46-18-104-16-156 14z' fill='#d6ece9'/> <path d='M256 196v178' stroke='#17324d' stroke-width='9' stroke-linecap='round'/> <g stroke='#0f766e' stroke-width='12' stroke-linecap='round' fill='none'> <path d='M140 238c34-10 68-6 88 6'/><path d='M140 282c34-10 68-6 88 6'/> <path d='M372 238c-34-10-68-6-88 6'/><path d='M372 282c-34-10-68-6-88 6'/> </g> <path d='M256 74l14 38 38 14-38 14-14 38-14-38-38-14 38-14z' fill='#ef6530'/> </svg>";}
    private static void send(HttpExchange x,int status,String type,String body)throws IOException{
        byte[] data=body.getBytes(StandardCharsets.UTF_8); x.getResponseHeaders().set("Content-Type",type); if("HEAD".equalsIgnoreCase(x.getRequestMethod())){x.sendResponseHeaders(status,-1);return;}x.sendResponseHeaders(status,data.length);x.getResponseBody().write(data);
    }
    private static void redirect(HttpExchange x,String location)throws IOException{x.getResponseHeaders().set("Location",location);x.sendResponseHeaders(303,-1);}
    private static Map<String,String> form(HttpExchange x)throws IOException{
        byte[] bytes=x.getRequestBody().readNBytes(LimitesCampos.MAX_BYTES_FORMULARIO+1);
        if(bytes.length>LimitesCampos.MAX_BYTES_FORMULARIO)throw new FormTooLargeException();
        String raw=new String(bytes,StandardCharsets.UTF_8);Map<String,String> values=new java.util.HashMap<>();
        for(String part:raw.split("&")){int i=part.indexOf('=');if(i<0)continue;String key=URLDecoder.decode(part.substring(0,i),StandardCharsets.UTF_8),value=URLDecoder.decode(part.substring(i+1),StandardCharsets.UTF_8);if(key.equals("grados")&&values.containsKey(key))values.put(key,values.get(key)+","+value);else values.put(key,value);}return values;
    }
    private static WebSession sessionFor(HttpExchange x){
        String token=null;String header=x.getRequestHeaders().getFirst("Cookie");
        if(header!=null)for(String c:header.split(";")){String[] p=c.trim().split("=",2);if(p.length==2&&p[0].equals(COOKIE))token=p[1];}
        WebSession s=token==null?null:SESSIONS.get(token);if(s==null&&token!=null)s=restoreSession(x,token);if(s==null){s=new WebSession(randomToken());SESSIONS.put(s.id,s);setCookie(x,s.id);}s.last=Instant.now().getEpochSecond();touchSession(s);RENDER_CSRF.set(s.csrf);
        LOCAL_REQUEST.set(x.getRemoteAddress()!=null&&x.getRemoteAddress().getAddress().isLoopbackAddress());
        ADMIN_REQUEST.set(s.user!=null&&s.user.getRol()!=null&&"ADMIN".equals(s.user.getRol().name()));
        ROLE_REQUEST.set(s.user==null||s.user.getRol()==null?null:s.user.getRol().name());return s;
    }
    private static void rotateCookie(HttpExchange x,WebSession s){SESSIONS.remove(s.id);s.id=randomToken();s.csrf=randomToken();SESSIONS.put(s.id,s);setCookie(x,s.id);}
    /** Reconstruye una sesión guardada en MySQL (tras un reinicio del servidor); null si no existe o ya no vale. */
    private static WebSession restoreSession(HttpExchange x,String token){
        try{
            SesionWebDAO.Datos d=SESIONES_WEB.cargar(token,MAX_IDLE_SECONDS);
            if(d==null)return null;
            WebSession s=new WebSession(token);s.csrf=d.csrf();
            if(d.idUsuario()!=null&&d.idLogin()!=null){
                Usuario u=USERS.buscarPorId(d.idUsuario()).orElse(null);
                if(u==null||!u.isActivo()){SESIONES_WEB.eliminar(token);return null;}
                s.user=u;
                String ip=clientIp(x);
                s.login=new ControladorSesion(d.idLogin(),u.getId(),ip);
            }else if(d.idPendiente()!=null){
                Usuario u=USERS.buscarPorId(d.idPendiente()).orElse(null);
                Autenticacion2FADAO.Estado state=u==null?null:MFA.obtenerEstado(u.getId());
                if(u==null||!u.isActivo()||state==null||state.secretoCifrado()==null||state.secretoCifrado().isBlank()){SESIONES_WEB.eliminar(token);return null;}
                s.pending=u;s.passwordWasVerified=true;s.activeMfa=d.mfaActivo();
                s.secret=TOTP.descifrar(state.secretoCifrado(),requireEncryptionKey());
            }else{SESIONES_WEB.eliminar(token);return null;}
            s.saved=Instant.now().getEpochSecond();
            WebSession previous=SESSIONS.putIfAbsent(token,s);return previous!=null?previous:s;
        }catch(Exception e){return null;}
    }
    /** Guarda la sesión en MySQL cuando tiene un usuario o una verificación 2FA pendiente. */
    private static void persistSession(WebSession s){
        if(s.user==null&&s.pending==null)return;
        try{
            SESIONES_WEB.guardar(s.id,s.csrf,s.user==null?null:s.user.getId(),s.user==null?s.pending.getId():null,s.activeMfa,s.login==null?null:s.login.getIdSesion());
            s.saved=Instant.now().getEpochSecond();
        }catch(SQLException e){System.err.println("No se pudo guardar la sesión web: "+e.getMessage());}
    }
    /** Renueva el último acceso guardado, como máximo una vez por minuto por sesión. */
    private static void touchSession(WebSession s){
        if(s.user==null&&s.pending==null)return;
        long now=Instant.now().getEpochSecond();if(now-s.saved<60)return;s.saved=now;
        try{SESIONES_WEB.tocar(s.id);}catch(SQLException ignored){}
    }
    private static void setCookie(HttpExchange x,String id){String secure="true".equalsIgnoreCase(env("SIGE_WEB_SECURE_COOKIE","false"))?"; Secure":"";x.getResponseHeaders().add("Set-Cookie",COOKIE+"="+id+"; Path=/; HttpOnly; SameSite=Strict; Max-Age="+MAX_IDLE_SECONDS+secure);}
    private static void clearCookie(HttpExchange x){x.getResponseHeaders().add("Set-Cookie",COOKIE+"=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");}
    private static String randomToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static boolean constantEquals(String a,String b){return a!=null&&b!=null&&java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    private static String requireEncryptionKey()throws Configuracion2FAException{String key=ConexionBD.claveCifrado2FA();if(key==null||key.isBlank())throw new Configuracion2FAException("Falta configurar 2fa.encryption-key en el servidor.");return key;}
    private static String qrData(String text){
        try{var matrix=new QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,220,220);StringBuilder path=new StringBuilder();
            for(int y=0;y<matrix.getHeight();y++)for(int x=0;x<matrix.getWidth();x++)if(matrix.get(x,y))path.append('M').append(x).append(',').append(y).append("h1v1h-1z");
            String svg="<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 "+matrix.getWidth()+" "+matrix.getHeight()+"'><rect width='100%' height='100%' fill='white'/><path d='"+path+"' fill='black'/></svg>";
            return "data:image/svg+xml;base64,"+Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        }catch(WriterException e){return "";}
    }
    private static void accessLog(Integer id,String email,String result,String detail){try{MFA.registrarAcceso(id,email,result,detail);}catch(SQLException ignored){}}
    private static void cleanupSessions(){long now=Instant.now().getEpochSecond();SESSIONS.entrySet().removeIf(e->now-e.getValue().last>MAX_IDLE_SECONDS);if(now-ultimaPurgaSesiones>600){LOGIN_POR_IP.purgar();ultimaPurgaSesiones=now;try{SESIONES_WEB.purgarCaducadas(MAX_IDLE_SECONDS);}catch(SQLException ignored){}}}
    private static String env(String key,String fallback){String v=System.getenv(key);return v==null||v.isBlank()?fallback:v.trim();}
    private static String esc(String s){if(s==null)return "";return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}

    private static final class WebSession {
        private String id; private String csrf=randomToken(); private long last=Instant.now().getEpochSecond();
        private Usuario user; private Usuario pending; private String secret; private boolean activeMfa; private boolean passwordWasVerified;
        private ControladorSesion login;
        private long saved;
        private WebSession(String id){this.id=id;}
    }
}
