package com.unev.sige.model;

/**
 * Representa la nota de un Estudiante en una Asignatura durante un periodo especifico.
 * Relaciona a un Estudiante con una Asignatura (muchos a muchos, resuelto por esta clase intermedia).
 */
public class Calificacion {

    private int id;
    private Estudiante estudiante;
    private Asignatura asignatura;
    private String periodo;
    private double valor;
    private TipoCalificacion tipo;

    public Calificacion() {
    }

    public Calificacion(int id, Estudiante estudiante, Asignatura asignatura, String periodo,
                         double valor, TipoCalificacion tipo) {
        this.id = id;
        this.estudiante = estudiante;
        this.asignatura = asignatura;
        this.periodo = periodo;
        this.valor = valor;
        this.tipo = tipo;
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public TipoCalificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoCalificacion tipo) {
        this.tipo = tipo;
    }

    // ----- Metodos propios -----

    /**
     * Indica si la calificacion es aprobatoria (>= 70, escala de 0 a 100).
     */
    public boolean esAprobatoria() {
        return valor >= 70.0;
    }

    @Override
    public String toString() {
        return "Calificacion{" + estudiante.getNombreCompleto() + " - " + asignatura.getNombre()
                + " (" + periodo + "): " + valor + "}";
    }
}
