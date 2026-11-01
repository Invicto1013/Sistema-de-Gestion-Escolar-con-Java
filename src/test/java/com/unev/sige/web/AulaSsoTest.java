package com.unev.sige.web;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AulaSsoTest {
    static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres!";

    @Test void vectorFijoIgualAlDelAula() {
        // Mismo vector que SsoTest.vectorFijoCompatibleConSige en el proyecto sige-aula.
        assertEquals("v1.aWQ9NyZyb2w9RVNUVURJQU5URSZub21icmU9THVpcytHJUMzJUIzbWV6JmV4cD0xODAwMDAwMTgwJmp0aT1qdGktZmlqbw.x_x_H3NLjukIbrMM_Qq1Rotuy0ZMOyWU4VsS8drUaHI",
                AulaSso.crear(SECRETO, 7, "ESTUDIANTE", "Luis Gómez", 1_800_000_000L, "jti-fijo"));
    }

    @Test void urlDeAccesoApuntaAlSsoDelAulaYNoRepiteToken() {
        String a = AulaSso.urlDeAcceso("https://aula.example", SECRETO, 1, "PROFESOR", "Ana", 1_800_000_000L);
        String b = AulaSso.urlDeAcceso("https://aula.example", SECRETO, 1, "PROFESOR", "Ana", 1_800_000_000L);
        assertTrue(a.startsWith("https://aula.example/sso?token=v1."));
        assertNotEquals(a, b);
    }

    @Test void urlBaseSoloAceptaHttpsOLocal() {
        assertEquals("https://aula.example", AulaSso.urlBase("https://aula.example/"));
        assertEquals("http://localhost:8081", AulaSso.urlBase("http://localhost:8081"));
        assertNull(AulaSso.urlBase("http://aula.example"));
        assertNull(AulaSso.urlBase("javascript:alert(1)"));
        assertNull(AulaSso.urlBase(" "));
        assertNull(AulaSso.urlBase(null));
    }

    @Test void secretoCortoNoActiva() {
        assertNull(AulaSso.secreto("corto"));
        assertNull(AulaSso.secreto(null));
        assertNotNull(AulaSso.secreto(SECRETO));
    }
}
