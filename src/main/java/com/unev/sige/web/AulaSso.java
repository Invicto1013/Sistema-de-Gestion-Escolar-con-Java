package com.unev.sige.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Inicio de sesión único hacia el Aula Virtual (subsistema aparte, sige-aula).
 * Firma un token de 180 s con un secreto compartido. El formato es el mismo que verifica el Aula en Sso.java.
 * Se activa con SIGE_AULA_URL y SIGE_AULA_SSO_SECRET; sin ellas, SIGE sigue usando el aula integrada.
 */
public final class AulaSso {
    public static final int TTL_SEGUNDOS = 180;
    private static final SecureRandom AZAR = new SecureRandom();

    private AulaSso() { }

    public static boolean configurado() {
        return urlBase(System.getenv("SIGE_AULA_URL")) != null && secreto(System.getenv("SIGE_AULA_SSO_SECRET")) != null;
    }

    /** URL a la que se redirige al usuario para entrar al Aula ya autenticado. */
    public static String urlDeAcceso(int id, String rol, String nombre) {
        String base = urlBase(System.getenv("SIGE_AULA_URL")), secreto = secreto(System.getenv("SIGE_AULA_SSO_SECRET"));
        if (base == null || secreto == null) throw new IllegalStateException("El Aula no está configurada.");
        return urlDeAcceso(base, secreto, id, rol, nombre, System.currentTimeMillis() / 1000);
    }

    static String urlDeAcceso(String base, String secreto, int id, String rol, String nombre, long ahoraSeg) {
        byte[] azar = new byte[12];
        AZAR.nextBytes(azar);
        String token = crear(secreto, id, rol, nombre, ahoraSeg, Base64.getUrlEncoder().withoutPadding().encodeToString(azar));
        return base + "/sso?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    static String crear(String secreto, int id, String rol, String nombre, long ahoraSeg, String jti) {
        String carga = "id=" + id + "&rol=" + enc(rol) + "&nombre=" + enc(nombre) + "&exp=" + (ahoraSeg + TTL_SEGUNDOS) + "&jti=" + enc(jti);
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(carga.getBytes(StandardCharsets.UTF_8));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "v1." + b64 + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(b64.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }

    static String urlBase(String v) {
        if (v == null || v.isBlank()) return null;
        String u = v.trim();
        if (!(u.startsWith("https://") || u.startsWith("http://localhost") || u.startsWith("http://127.0.0.1"))) return null;
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    static String secreto(String v) { return v != null && v.trim().length() >= 32 ? v.trim() : null; }
}
