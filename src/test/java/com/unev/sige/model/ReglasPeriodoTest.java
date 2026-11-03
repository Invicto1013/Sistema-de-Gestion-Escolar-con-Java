package com.unev.sige.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReglasPeriodoTest {

    private static Calificacion notas(double p1, double p2, double examen) {
        Calificacion c = new Calificacion(0, 1, p1, p2, examen, 0, "");
        c.recalcular();
        return c;
    }

    // ----- Nombre y fechas del periodo -----

    @Test
    void aceptaNombresConFormatoAnioGuionNumero() {
        assertEquals("2026-1", ReglasPeriodo.validarNombre("2026-1"));
        assertEquals("2026-2", ReglasPeriodo.validarNombre("  2026-2  "));
        assertEquals("2027-V", ReglasPeriodo.validarNombre("2027-V"));
    }

    @Test
    void rechazaNombresQueNoSiguenElFormato() {
        for (String malo : new String[] {null, "", "   ", "2026", "26-1", "2026-", "2026-123456789", "2026 1", "2026-1; DROP", "abcd-1"}) {
            assertThrows(IllegalArgumentException.class, () -> ReglasPeriodo.validarNombre(malo), "nombre: " + malo);
        }
    }

    @Test
    void rechazaFechaDeInicioPosteriorAlFin() {
        assertThrows(IllegalArgumentException.class,
                () -> ReglasPeriodo.validarFechas(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)));
        ReglasPeriodo.validarFechas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1));
        ReglasPeriodo.validarFechas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        ReglasPeriodo.validarFechas(null, LocalDate.of(2026, 1, 1));
        ReglasPeriodo.validarFechas(null, null);
    }

    // ----- Motivo obligatorio -----

    @Test
    void elMotivoEsObligatorioYAcotado() {
        for (String malo : new String[] {null, "", "   ", "corto", "123456789"}) {
            assertThrows(IllegalArgumentException.class, () -> ReglasPeriodo.validarMotivo(malo), "motivo: " + malo);
        }
        assertThrows(IllegalArgumentException.class, () -> ReglasPeriodo.validarMotivo("x".repeat(301)));
        assertEquals("1234567890", ReglasPeriodo.validarMotivo("1234567890"));
        assertEquals(300, ReglasPeriodo.validarMotivo("x".repeat(300)).length());
    }

    @Test
    void elMotivoSeNormalizaSinEspaciosSobrantes() {
        assertEquals("Error de digitación en el examen", ReglasPeriodo.validarMotivo("  Error  de   digitación\nen el examen  "));
    }

    // ----- Quien puede tocar las notas -----

    @Test
    void conElPeriodoAbiertoEditanProfesoresYAdministradores() {
        assertTrue(ReglasPeriodo.puedeEditarNotas(Rol.PROFESOR, EstadoPeriodo.ABIERTO));
        assertTrue(ReglasPeriodo.puedeEditarNotas(Rol.ADMIN, EstadoPeriodo.ABIERTO));
        assertFalse(ReglasPeriodo.puedeEditarNotas(Rol.ESTUDIANTE, EstadoPeriodo.ABIERTO));
        assertFalse(ReglasPeriodo.puedeEditarNotas(Rol.PADRE, EstadoPeriodo.ABIERTO));
    }

    @Test
    void conElPeriodoCerradoNadieEditaPorLaViaNormal() {
        for (Rol rol : Rol.values()) {
            assertFalse(ReglasPeriodo.puedeEditarNotas(rol, EstadoPeriodo.CERRADO), "rol: " + rol);
        }
    }

    @Test
    void laCorreccionExcepcionalEsSoloDelAdministradorYSoloConElPeriodoCerrado() {
        assertTrue(ReglasPeriodo.puedeCorregir(Rol.ADMIN, EstadoPeriodo.CERRADO));
        assertFalse(ReglasPeriodo.puedeCorregir(Rol.ADMIN, EstadoPeriodo.ABIERTO));
        assertFalse(ReglasPeriodo.puedeCorregir(Rol.PROFESOR, EstadoPeriodo.CERRADO));
        assertFalse(ReglasPeriodo.puedeCorregir(Rol.ESTUDIANTE, EstadoPeriodo.CERRADO));
        assertFalse(ReglasPeriodo.puedeCorregir(Rol.PADRE, EstadoPeriodo.CERRADO));
    }

    // ----- Resultado del estudiante al cerrar -----

    @Test
    void resultadoDelEstudianteSegunEstadoDeSusMaterias() {
        assertEquals("APROBADO", ReglasPeriodo.resultado(5, 5, 0));
        assertEquals("REPROBADO", ReglasPeriodo.resultado(5, 4, 1));
        assertEquals("REPROBADO", ReglasPeriodo.resultado(1, 0, 1));
        assertEquals("INCOMPLETO", ReglasPeriodo.resultado(5, 3, 1));
        assertEquals("INCOMPLETO", ReglasPeriodo.resultado(5, 0, 0));
        assertEquals("INCOMPLETO", ReglasPeriodo.resultado(0, 0, 0));
    }

    // ----- Texto de auditoria -----

    @Test
    void describeSoloLasNotasQueCambiaron() {
        Calificacion antes = notas(70, 70, 70);
        Calificacion despues = notas(70, 80, 70);
        String texto = ReglasPeriodo.describirCambio(antes, despues);
        assertTrue(texto.contains("Parcial 2 70.00 -> 80.00"), texto);
        assertTrue(texto.contains("Final 70.00 -> 73.00"), texto);
        assertFalse(texto.contains("Parcial 1"), texto);
        assertFalse(texto.contains("Examen"), texto);
    }

    @Test
    void sinNotaPreviaSeMuestraGuionComoValorAnterior() {
        String texto = ReglasPeriodo.describirCambio(null, notas(90, 90, 90));
        assertTrue(texto.contains("Parcial 1 - -> 90.00"), texto);
        assertTrue(texto.contains("Final - -> 90.00"), texto);
    }

    @Test
    void detectaSiHayCambiosQueAuditar() {
        assertTrue(ReglasPeriodo.hayCambios(null, notas(0, 0, 0)));
        assertFalse(ReglasPeriodo.hayCambios(notas(80, 80, 80), notas(80, 80, 80)));
        assertTrue(ReglasPeriodo.hayCambios(notas(80, 80, 80), notas(80, 80, 81)));
        assertTrue(ReglasPeriodo.describirCambio(notas(80, 80, 80), notas(80, 80, 80)).startsWith("Sin cambios"));
    }

    @Test
    void recortaTextosLargosSinFallar() {
        assertNull(ReglasPeriodo.recortar(null, 10));
        assertEquals("corto", ReglasPeriodo.recortar("corto", 10));
        String recortado = ReglasPeriodo.recortar("x".repeat(50), 10);
        assertEquals(10, recortado.length());
        assertTrue(recortado.endsWith("..."));
    }

    // ----- EstadoPeriodo -----

    @Test
    void elEstadoDeUnPeriodoSinRegistroCuentaComoAbierto() {
        assertEquals(EstadoPeriodo.ABIERTO, EstadoPeriodo.desde(null));
        assertEquals(EstadoPeriodo.ABIERTO, EstadoPeriodo.desde(" "));
        assertEquals(EstadoPeriodo.CERRADO, EstadoPeriodo.desde("cerrado"));
        assertEquals(EstadoPeriodo.ABIERTO, EstadoPeriodo.desde("ABIERTO"));
        assertThrows(IllegalArgumentException.class, () -> EstadoPeriodo.desde("OTRO"));
    }
}
