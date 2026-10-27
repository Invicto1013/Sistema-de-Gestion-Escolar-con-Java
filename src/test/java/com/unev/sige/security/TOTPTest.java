package com.unev.sige.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class TOTPTest {
    private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTRA_CLAVE = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void secretoNuevoEsBase32DeVeinteBytes() {
        String secreto = TOTP.nuevoSecreto();
        assertEquals(32, secreto.length());
        assertTrue(secreto.matches("[A-Z2-7]+"));
        assertNotEquals(secreto, TOTP.nuevoSecreto());
    }

    @Test
    void aceptaElCodigoActualYRechazaFormatosInvalidos() throws Exception {
        String secreto = TOTP.nuevoSecreto();
        assertTrue(TOTP.verificar(secreto, TOTP.codigoActual(secreto)));
        assertFalse(TOTP.verificar(secreto, null));
        assertFalse(TOTP.verificar(secreto, "12345"));
        assertFalse(TOTP.verificar(secreto, "abcdef"));
        assertFalse(TOTP.verificar(secreto, "1234567"));
    }

    @Test
    void elCodigoTieneSeisDigitos() throws Exception {
        assertTrue(TOTP.codigoActual(TOTP.nuevoSecreto()).matches("\\d{6}"));
    }

    @Test
    void cifraYDescifraElSecreto() throws Exception {
        String secreto = TOTP.nuevoSecreto();
        String cifrado = TOTP.cifrar(secreto, CLAVE);
        assertTrue(cifrado.startsWith("v1:"));
        assertFalse(cifrado.contains(secreto));
        assertEquals(secreto, TOTP.descifrar(cifrado, CLAVE));
        assertNotEquals(cifrado, TOTP.cifrar(secreto, CLAVE));
    }

    @Test
    void noDescifraConOtraClaveNiConFormatoInvalido() throws Exception {
        String cifrado = TOTP.cifrar("JBSWY3DPEHPK3PXP", CLAVE);
        assertThrows(Configuracion2FAException.class, () -> TOTP.descifrar(cifrado, OTRA_CLAVE));
        assertThrows(Configuracion2FAException.class, () -> TOTP.descifrar("sin-version", CLAVE));
        assertThrows(Configuracion2FAException.class, () -> TOTP.descifrar(null, CLAVE));
    }

    @Test
    void exigeUnaClaveDeTreintaYDosBytes() {
        String corta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(Configuracion2FAException.class, () -> TOTP.cifrar("JBSWY3DPEHPK3PXP", corta));
        assertThrows(Configuracion2FAException.class, () -> TOTP.descifrar("v1:AAAA", corta));
    }

    @Test
    void construyeLaUriParaAppsDeAutenticacion() {
        String uri = TOTP.uri("ana@sige.edu.do", "JBSWY3DPEHPK3PXP");
        assertTrue(uri.startsWith("otpauth://totp/SIGE%3Aana%40sige.edu.do?"));
        assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"));
        assertTrue(uri.contains("issuer=SIGE"));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }
}
