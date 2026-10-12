package com.unev.sige.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/** TOTP compatible con RFC 6238: SHA-1, 30 segundos y seis dígitos. */
public final class TOTP {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TOTP() { }

    public static String nuevoSecreto() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32(bytes);
    }

    public static String codigoActual(String secreto) throws Exception {
        return codigo(secreto, Instant.now().getEpochSecond() / 30);
    }

    public static boolean verificar(String secreto, String codigo) throws Exception {
        if (codigo == null || !codigo.matches("\\d{6}")) return false;
        byte[] recibido = codigo.getBytes(StandardCharsets.US_ASCII);
        long paso = Instant.now().getEpochSecond() / 30;
        for (long delta = -1; delta <= 1; delta++) {
            byte[] esperado = codigo(secreto, paso + delta).getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(esperado, recibido)) return true;
        }
        return false;
    }

    public static String uri(String correo, String secreto) {
        String issuer = "SIGE";
        String etiqueta = issuer + ":" + correo;
        return "otpauth://totp/" + encode(etiqueta) + "?secret=" + secreto
                + "&issuer=" + encode(issuer) + "&algorithm=SHA1&digits=6&period=30";
    }

    public static String cifrar(String secreto, String claveBase64) throws Configuracion2FAException {
        try {
            byte[] clave = Base64.getDecoder().decode(claveBase64);
            if (clave.length != 32) throw new Configuracion2FAException(
                    "La clave 2FA debe ser Base64 y contener exactamente 32 bytes.");
            byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
            var cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"),
                    new javax.crypto.spec.GCMParameterSpec(128, nonce));
            byte[] cifrado = cipher.doFinal(secreto.getBytes(StandardCharsets.UTF_8));
            byte[] combinado = ByteBuffer.allocate(nonce.length + cifrado.length).put(nonce).put(cifrado).array();
            return "v1:" + Base64.getEncoder().encodeToString(combinado);
        } catch (Configuracion2FAException e) {
            throw e;
        } catch (Exception e) {
            throw new Configuracion2FAException("No se pudo cifrar el secreto de 2FA.", e);
        }
    }

    public static String descifrar(String texto, String claveBase64) throws Configuracion2FAException {
        try {
            if (texto == null || !texto.startsWith("v1:")) throw new Configuracion2FAException(
                    "El secreto de 2FA almacenado no tiene un formato compatible.");
            byte[] clave = Base64.getDecoder().decode(claveBase64);
            if (clave.length != 32) throw new Configuracion2FAException(
                    "La clave 2FA debe ser Base64 y contener exactamente 32 bytes.");
            byte[] combinado = Base64.getDecoder().decode(texto.substring(3));
            byte[] nonce = java.util.Arrays.copyOfRange(combinado, 0, 12);
            byte[] cifrado = java.util.Arrays.copyOfRange(combinado, 12, combinado.length);
            var cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"),
                    new javax.crypto.spec.GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Configuracion2FAException e) {
            throw e;
        } catch (Exception e) {
            throw new Configuracion2FAException("No se pudo descifrar el secreto de 2FA. Verifica la clave local.", e);
        }
    }

    private static String codigo(String secreto, long paso) throws Exception {
        byte[] key = decodificarBase32(secreto);
        byte[] contador = ByteBuffer.allocate(Long.BYTES).putLong(paso).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(contador);
        int offset = hash[hash.length - 1] & 0x0f;
        int binario = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
        return String.format(java.util.Locale.ROOT, "%06d", binario % 1_000_000);
    }

    private static String base32(byte[] bytes) {
        StringBuilder out = new StringBuilder(); int buffer = 0, bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff); bits += 8;
            while (bits >= 5) { out.append(BASE32.charAt((buffer >> (bits - 5)) & 31)); bits -= 5; }
        }
        if (bits > 0) out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return out.toString();
    }

    private static byte[] decodificarBase32(String texto) {
        String valor = texto.toUpperCase(java.util.Locale.ROOT).replace("=", "").replace(" ", "");
        byte[] resultado = new byte[valor.length() * 5 / 8]; int buffer = 0, bits = 0, indice = 0;
        for (char ch : valor.toCharArray()) {
            int n = BASE32.indexOf(ch);
            if (n < 0) throw new IllegalArgumentException("Secreto Base32 inválido.");
            buffer = (buffer << 5) | n; bits += 5;
            if (bits >= 8) { resultado[indice++] = (byte) ((buffer >> (bits - 8)) & 0xff); bits -= 8; }
        }
        return resultado;
    }

    private static String encode(String texto) {
        return java.net.URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
