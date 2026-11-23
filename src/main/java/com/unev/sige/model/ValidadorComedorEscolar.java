package com.unev.sige.model;

import java.util.Set;

/**
 * Gestor del Servicio de Comedor Escolar, Menú y Alergias Alimentarias.
 * Garantiza que la cocina escolar respete las restricciones nutricionales de cada alumno.
 */
public final class ValidadorComedorEscolar {

    public enum PlanAlimentario {
        REGULAR,
        VEGETARIANO,
        CELIACO_SIN_GLUTEN,
        HIPOCALORICO,
        PERSONALIZADO
    }

    public record FichaNutricional(
            PlanAlimentario plan,
            Set<String> alergiasAlimentarias,
            boolean requiereSupervision
    ) { }

    private ValidadorComedorEscolar() {
    }

    public static boolean esAptoMenu(FichaNutricional ficha, Set<String> ingredientesPlato) {
        if (ficha == null || ingredientesPlato == null) return true;
        if (ficha.alergiasAlimentarias() == null || ficha.alergiasAlimentarias().isEmpty()) return true;

        for (String ingrediente : ingredientesPlato) {
            if (ingrediente == null) continue;
            for (String alergia : ficha.alergiasAlimentarias()) {
                // Por seguridad se detecta el alérgeno aunque venga dentro de un nombre compuesto (SALSA_CON_MANI contiene MANI).
                if (alergia != null && !alergia.isBlank()
                        && ingrediente.trim().toUpperCase().contains(alergia.trim().toUpperCase())) {
                    return false; // Alérgeno detectado en el plato
                }
            }
        }
        return true;
    }
}
