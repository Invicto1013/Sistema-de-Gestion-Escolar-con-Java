package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SesionWebDAOTest {

    @Test
    @DisplayName("El hash coincide con el vector conocido de SHA-256")
    void hash_cadenaAbc_devuelveVectorConocido() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", SesionWebDAO.hash("abc"));
    }

    @Test
    @DisplayName("El hash es hexadecimal de 64 caracteres y no contiene el token")
    void hash_cualquierToken_tiene64CaracteresHex() {
        String token = "tokenDePruebaDeLaCookie_123";
        String resultado = SesionWebDAO.hash(token);
        assertEquals(64, resultado.length());
        assertTrue(resultado.matches("[0-9a-f]{64}"));
        assertTrue(!resultado.contains(token));
    }

    @Test
    @DisplayName("Tokens distintos producen hashes distintos y el mismo token siempre el mismo")
    void hash_tokensDistintos_devuelveValoresDistintos() {
        assertNotEquals(SesionWebDAO.hash("uno"), SesionWebDAO.hash("dos"));
        assertEquals(SesionWebDAO.hash("uno"), SesionWebDAO.hash("uno"));
    }
}
