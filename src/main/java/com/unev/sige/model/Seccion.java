package com.unev.sige.model;

/**
 * Seccion de una asignatura (tabla `secciones`): conecta la Asignatura con su Profesor,
 * aula, horario y cupo. Los estudiantes se inscriben en secciones (DetalleInscripcion).
 */
public class Seccion {

    private int id;
    private String aula;
    private String horario;
    private int cupoMaximo;
    private Asignatura asignatura;
    private Profesor profesor; // puede ser null (id_profesor es opcional en la BD)
    private int inscritos;     // cuantos estudiantes tiene inscritos (se calcula al listar)

    public Seccion() {
    }

    public Seccion(int id, String aula, String horario, int cupoMaximo, Asignatura asignatura, Profesor profesor) {
        this.id = id;
        this.aula = aula;
        this.horario = horario;
        this.cupoMaximo = cupoMaximo;
        this.asignatura = asignatura;
        this.profesor = profesor;
    }

    // ----- Getters y setters -----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public int getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(int cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public void setAsignatura(Asignatura asignatura) {
        this.asignatura = asignatura;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public void setProfesor(Profesor profesor) {
        this.profesor = profesor;
    }

    public int getInscritos() {
        return inscritos;
    }

    public void setInscritos(int inscritos) {
        this.inscritos = inscritos;
    }

    @Override
    public String toString() {
        return "Seccion{" + id + ", " + (asignatura != null ? asignatura.getNombre() : "sin asignatura")
                + ", aula='" + aula + "', horario='" + horario + "'}";
    }
}
