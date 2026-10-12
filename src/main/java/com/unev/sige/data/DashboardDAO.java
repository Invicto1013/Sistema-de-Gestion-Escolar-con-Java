package com.unev.sige.data;

import com.unev.sige.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consultas agregadas del dashboard; el alcance depende del rol autenticado. */
public final class DashboardDAO {
    public record Indicadores(long inscripciones, long estudiantes, long secciones,
                              long evaluaciones, double promedio, long asistencias,
                              long hijos, long usuariosActivos) { }
    public record DatoGrafico(String etiqueta, double valor) { }
    public record Movimiento(String fecha, String estudiante, String asignatura, String periodo) { }
    public record Datos(Indicadores indicadores, List<DatoGrafico> porAsignatura,
                        List<DatoGrafico> porEstado, List<Movimiento> recientes) { }

    private record Alcance(String joins, String filtro, boolean parametro) { }

    public Datos cargar(Rol rol, int idUsuario) throws SQLException {
        Alcance a=alcance(rol);
        String base=" FROM inscripciones i JOIN detalle_inscripciones d ON d.id_inscripcion=i.id_inscripcion "
                +"JOIN secciones s ON s.id_seccion=d.id_seccion JOIN asignaturas m ON m.id_asignatura=s.id_asignatura "
                +"JOIN estudiantes e ON e.id_estudiante=i.id_estudiante "
                +"LEFT JOIN calificaciones c ON c.id_detalle_inscripcion=d.id_detalle_inscripcion "
                +a.joins();
        Indicadores indicadores;
        String sql="SELECT COUNT(DISTINCT d.id_detalle_inscripcion) inscripciones, "
                +"COUNT(DISTINCT i.id_estudiante) estudiantes, COUNT(DISTINCT s.id_seccion) secciones, "
                +"COUNT(DISTINCT CASE WHEN c.nota_final>0 THEN d.id_detalle_inscripcion END) evaluaciones, "
                +"COALESCE(AVG(NULLIF(c.nota_final,0)),0) promedio "+base
                +a.filtro();
        try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(sql)) {
            bind(p,a,idUsuario);try(ResultSet r=p.executeQuery()) {r.next();
                indicadores=new Indicadores(r.getLong("inscripciones"),r.getLong("estudiantes"),
                        r.getLong("secciones"),r.getLong("evaluaciones"),r.getDouble("promedio"),0,0,0);
            }
        }
        long asistencias=0;
        String sqlAsistencias="SELECT COUNT(DISTINCT x.id_asistencia) "+base+
                "LEFT JOIN asistencias x ON x.id_detalle_inscripcion=d.id_detalle_inscripcion "+a.filtro();
        try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(sqlAsistencias)) {
            bind(p,a,idUsuario);try(ResultSet r=p.executeQuery()){if(r.next())asistencias=r.getLong(1);}
        }
        long hijos=0, usuarios=0;
        if(rol==Rol.PADRE) {
            try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(
                    "SELECT COUNT(DISTINCT ep.id_estudiante) FROM estudiante_padre ep JOIN padres pa ON pa.id_padre=ep.id_padre WHERE pa.id_usuario=?")) {
                p.setInt(1,idUsuario);try(ResultSet r=p.executeQuery()){if(r.next())hijos=r.getLong(1);}
            }
        }
        if(rol==Rol.ADMIN) {
            try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(
                    "SELECT COUNT(*) FROM usuarios WHERE activo=1");ResultSet r=p.executeQuery()) {if(r.next())usuarios=r.getLong(1);}
        }
        indicadores=new Indicadores(indicadores.inscripciones(),indicadores.estudiantes(),indicadores.secciones(),
                indicadores.evaluaciones(),indicadores.promedio(),asistencias,hijos,usuarios);

        List<DatoGrafico> materias=new ArrayList<>();
        String sqlMaterias="SELECT m.nombre etiqueta, COUNT(DISTINCT d.id_detalle_inscripcion) valor "+base+
                a.filtro()+" GROUP BY m.id_asignatura,m.nombre ORDER BY valor DESC,m.nombre LIMIT 8";
        try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(sqlMaterias)) {
            bind(p,a,idUsuario);try(ResultSet r=p.executeQuery()){while(r.next())materias.add(new DatoGrafico(r.getString(1),r.getDouble(2)));}
        }
        List<DatoGrafico> estados=new ArrayList<>();
        String sqlEstado="SELECT COALESCE(d.estado_materia,'Sin estado') etiqueta,COUNT(DISTINCT d.id_detalle_inscripcion) valor "+base+
                a.filtro()+" GROUP BY d.estado_materia ORDER BY valor DESC";
        try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(sqlEstado)) {
            bind(p,a,idUsuario);try(ResultSet r=p.executeQuery()){while(r.next())estados.add(new DatoGrafico(r.getString(1),r.getDouble(2)));}
        }
        List<Movimiento> movimientos=new ArrayList<>();
        String recientes="SELECT i.fecha_inscripcion fecha, CONCAT(u.nombre,' ',u.apellido) estudiante, "
                +"m.nombre asignatura,i.periodo_lectivo periodo "+base+"JOIN usuarios u ON u.id_usuario=e.id_estudiante "+a.filtro()
                +" GROUP BY i.id_inscripcion,d.id_detalle_inscripcion,i.fecha_inscripcion,u.nombre,u.apellido,m.nombre,i.periodo_lectivo "
                +"ORDER BY i.fecha_inscripcion DESC,i.id_inscripcion DESC LIMIT 10";
        try(Connection c=ConexionBD.obtener();PreparedStatement p=c.prepareStatement(recientes)) {
            bind(p,a,idUsuario);try(ResultSet r=p.executeQuery()){while(r.next())movimientos.add(new Movimiento(
                    String.valueOf(r.getDate("fecha")),r.getString("estudiante"),r.getString("asignatura"),r.getString("periodo")));}
        }
        return new Datos(indicadores,List.copyOf(materias),List.copyOf(estados),List.copyOf(movimientos));
    }

    private Alcance alcance(Rol rol) {
        return switch(rol) {
            case ADMIN -> new Alcance("","",false);
            case PROFESOR -> new Alcance(""," WHERE s.id_profesor=?",true);
            case ESTUDIANTE -> new Alcance(""," WHERE i.id_estudiante=?",true);
            case PADRE -> new Alcance("JOIN estudiante_padre ep ON ep.id_estudiante=i.id_estudiante JOIN padres pa ON pa.id_padre=ep.id_padre",
                    " WHERE pa.id_usuario=?",true);
        };
    }
    private void bind(PreparedStatement p,Alcance a,int id) throws SQLException {if(a.parametro())p.setInt(1,id);}
}
