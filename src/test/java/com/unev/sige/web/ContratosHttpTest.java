package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Contrato HTTP real: arranca el servidor web en otra JVM (puerto libre, solo 127.0.0.1) y comprueba cabeceras,
 * cookie de sesion y que las rutas protegidas no se abren sin iniciar sesion.
 * Si el servidor no puede arrancar en el entorno (sin base de datos, sin red local), las pruebas se OMITEN, no fallan.
 */
class ContratosHttpTest {

    private static Process proceso;
    private static String base;
    private static HttpClient cliente;

    @BeforeAll
    static void arrancar() throws Exception {
        int puerto;
        try (ServerSocket s = new ServerSocket(0)) {
            puerto = s.getLocalPort();
        }
        String java = System.getProperty("java.home") + "/bin/java";
        ProcessBuilder pb = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), "com.unev.sige.web.WebApp");
        pb.environment().put("SIGE_WEB_PORT", String.valueOf(puerto));
        pb.environment().put("SIGE_WEB_HOST", "127.0.0.1");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        proceso = pb.start();
        base = "http://127.0.0.1:" + puerto;
        cliente = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(2)).build();
        boolean arriba = false;
        for (int i = 0; i < 30 && proceso.isAlive() && !arriba; i++) {
            try {
                cliente.send(HttpRequest.newBuilder(URI.create(base + "/health")).timeout(Duration.ofSeconds(2)).build(),
                        HttpResponse.BodyHandlers.discarding());
                arriba = true;
            } catch (IOException e) {
                Thread.sleep(500);
            }
        }
        assumeTrue(arriba, "El servidor web no arranco en este entorno; contratos HTTP omitidos.");
    }

    @AfterAll
    static void detener() {
        if (proceso != null) {
            proceso.destroyForcibly();
        }
    }

    private static HttpResponse<String> get(String ruta) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void laPaginaDeEntradaLlevaCabecerasDeSeguridad() throws Exception {
        HttpResponse<String> r = get("/");
        assertEquals("DENY", r.headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("no-store", r.headers().firstValue("Cache-Control").orElse(""));
        assertTrue(r.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"));
    }

    @Test
    void laCookieDeSesionEsHttpOnlyYSameSite() throws Exception {
        List<String> cookies = get("/").headers().allValues("Set-Cookie");
        assertTrue(!cookies.isEmpty(), "se esperaba una cookie de sesion");
        String sesion = cookies.get(0);
        assertTrue(sesion.contains("HttpOnly"), sesion);
        assertTrue(sesion.contains("SameSite=Lax"), sesion);
    }

    @Test
    void unaRutaProtegidaSinSesionNoMuestraDatos() throws Exception {
        HttpResponse<String> r = get("/usuarios");
        String cuerpo = r.body().toLowerCase();
        boolean redirige = r.statusCode() >= 300 && r.statusCode() < 400;
        boolean denegada = r.statusCode() == 401 || r.statusCode() == 403;
        // La app responde 200 con un aviso de acceso denegado, sin listar cuentas ni mostrar el formulario de borrado.
        boolean avisoSinDatos = r.statusCode() == 200 && cuerpo.contains("solo un administrador")
                && !cuerpo.contains("user-delete") && !cuerpo.contains("@");
        assertTrue(redirige || denegada || avisoSinDatos, "estado inesperado: " + r.statusCode());
    }

    @Test
    void lasHerramientasEscolaresPidenIniciarSesion() throws Exception {
        for (String ruta : new String[] {"/herramientas", "/herramienta?id=excusa"}) {
            HttpResponse<String> r = get(ruta);
            assertEquals(200, r.statusCode(), ruta);
            assertTrue(r.body().contains("Inicia sesión"), ruta);
            assertTrue(!r.body().contains("Calcular"), ruta);
        }
    }

    @Test
    void elCarnetDelPersonalExigeSesion() throws Exception {
        for (String ruta : new String[] {"/carnet-personal", "/carnet-personal?usuario=1", "/personal-carnets"}) {
            HttpResponse<String> r = get(ruta);
            assertTrue(r.body().contains("Inicia sesión"), ruta);
            assertTrue(!r.body().contains("Carnet de Personal") && !r.body().contains("ID SIGE"), ruta);
        }
    }

    @Test
    void elEstadoDeSaludRespondeSinSesion() throws Exception {
        assertEquals(200, get("/health").statusCode());
    }
}
