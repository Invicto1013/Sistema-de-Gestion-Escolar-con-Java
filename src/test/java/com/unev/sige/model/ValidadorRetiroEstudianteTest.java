package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorRetiroEstudianteTest {

    @Test
    @DisplayName("Autorización de retiro con PIN correcto y estado activo")
    void puedeRetirar_exito() {
        ValidadorRetiroEstudiante.AutorizadoRetiro aut = new ValidadorRetiroEstudiante.AutorizadoRetiro(
                "Carmen Martínez", "Tía", "001-1234567-8", "4589", true
        );
        assertTrue(ValidadorRetiroEstudiante.puedeRetirar(aut, "4589"));
        assertFalse(ValidadorRetiroEstudiante.puedeRetirar(aut, "0000")); // PIN erróneo
    }

    @Test
    @DisplayName("Persona inactiva no puede retirar al estudiante aún con PIN correcto")
    void puedeRetirar_inactivo() {
        ValidadorRetiroEstudiante.AutorizadoRetiro aut = new ValidadorRetiroEstudiante.AutorizadoRetiro(
                "Juan Pérez", "Tío", "001-1234567-9", "4589", false
        );
        assertFalse(ValidadorRetiroEstudiante.puedeRetirar(aut, "4589"));
    }
}
