package com.unev.sige.model;

/**
 * Notas de una materia inscrita (tabla `calificaciones`, 1 a 1 con `detalle_inscripciones`).
 * Las notas van de 0 a 100.
 */
public class Calificacion {

    private int id;
    private int idDetalleInscripcion;
    private double notaParcial1;
    private double notaParcial2;
    private double notaExamenFinal;
    private double notaFinal;
    private String calificacionLiteral;

    public Calificacion() {
        this.calificacionLiteral = "";
    }

    public Calificacion(int id, int idDetalleInscripcion, double notaParcial1, double notaParcial2,
                        double notaExamenFinal, double notaFinal, String calificacionLiteral) {
        this.id = id;
        this.idDetalleInscripcion = idDetalleInscripcion;
        this.notaParcial1 = validar(notaParcial1);
        this.notaParcial2 = validar(notaParcial2);
        this.notaExamenFinal = validar(notaExamenFinal);
        this.notaFinal = validar(notaFinal);
        this.calificacionLiteral = calificacionLiteral;
    }

    private static double validar(double nota) {
        if (Double.isNaN(nota) || nota < 0.0 || nota > 100.0) {
            throw new IllegalArgumentException("La nota debe estar entre 0 y 100: " + nota);
        }
        return nota;
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdDetalleInscripcion() {
        return idDetalleInscripcion;
    }

    public void setIdDetalleInscripcion(int idDetalleInscripcion) {
        this.idDetalleInscripcion = idDetalleInscripcion;
    }

    public double getNotaParcial1() {
        return notaParcial1;
    }

    public void setNotaParcial1(double notaParcial1) {
        this.notaParcial1 = validar(notaParcial1);
    }

    public double getNotaParcial2() {
        return notaParcial2;
    }

    public void setNotaParcial2(double notaParcial2) {
        this.notaParcial2 = validar(notaParcial2);
    }

    public double getNotaExamenFinal() {
        return notaExamenFinal;
    }

    public void setNotaExamenFinal(double notaExamenFinal) {
        this.notaExamenFinal = validar(notaExamenFinal);
    }

    public double getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(double notaFinal) {
        this.notaFinal = validar(notaFinal);
    }

    public String getCalificacionLiteral() {
        return calificacionLiteral;
    }

    public void setCalificacionLiteral(String calificacionLiteral) {
        this.calificacionLiteral = calificacionLiteral;
    }

    // ----- Metodos propios -----

    /**
     * Indica si la nota final es aprobatoria (la nota minima esta en EscalaCalificaciones).
     */
    public boolean esAprobatoria() {
        return EscalaCalificaciones.aprueba(notaFinal);
    }

    /**
     * Recalcula la nota final y la letra a partir de los dos parciales y el examen final,
     * segun las reglas de EscalaCalificaciones.
     */
    public void recalcular() {
        this.notaFinal = EscalaCalificaciones.calcularNotaFinal(notaParcial1, notaParcial2, notaExamenFinal);
        this.calificacionLiteral = EscalaCalificaciones.literal(notaFinal);
    }

    /**
     * Ajusta un valor leido de la base de datos al rango 0-100 (un NaN pasa a 0), para que
     * un dato raro guardado a mano no impida abrir la pantalla.
     */
    public static double limitar(double nota) {
        if (Double.isNaN(nota)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, nota));
    }

    @Override
    public String toString() {
        return "Calificacion{detalle=" + idDetalleInscripcion + ", final=" + notaFinal
                + ", literal='" + calificacionLiteral + "'}";
    }
}
