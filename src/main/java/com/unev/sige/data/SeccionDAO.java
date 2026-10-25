package com.unev.sige.data;

import com.unev.sige.model.Asignatura;
import com.unev.sige.model.GradoCurricular;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Seccion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acceso MySQL a secciones con asignatura, grado, profesor y cupo. */
public class SeccionDAO {
    private static final String SQL_BASE = """
            SELECT s.id_seccion,s.aula,s.horario,s.cupo_maximo,g.id AS grado_id,g.codigo AS grado_codigo,
                   g.nivel AS grado_nivel,g.ciclo AS grado_ciclo,g.nombre AS grado_nombre,
                   (SELECT COUNT(*) FROM detalle_inscripciones d WHERE d.id_seccion=s.id_seccion) AS inscritos,
                   a.id_asignatura,a.codigo_asignatura,a.nombre AS asignatura_nombre,a.creditos,a.descripcion,
                   p.id_profesor,p.codigo_profesor,p.especialidad,
                   u.nombre AS profesor_nombre,u.apellido AS profesor_apellido,u.correo AS profesor_correo
            FROM secciones s
            JOIN asignaturas a ON a.id_asignatura=s.id_asignatura
            LEFT JOIN curriculo_grados g ON g.id=s.id_grado
            LEFT JOIN profesores p ON p.id_profesor=s.id_profesor
            LEFT JOIN usuarios u ON u.id_usuario=p.id_profesor
            """;
    private static final String SQL_ORDEN=" ORDER BY a.codigo_asignatura,s.id_seccion";

    public List<Seccion> listarTodas() throws SQLException { return consultar(SQL_BASE+SQL_ORDEN,null); }
    public List<Seccion> listarPorProfesor(int idProfesor) throws SQLException { return consultar(SQL_BASE+" WHERE s.id_profesor=?"+SQL_ORDEN,idProfesor); }

    public void insertar(Seccion s) throws SQLException {
        String sql="INSERT INTO secciones (aula,horario,cupo_maximo,id_asignatura,id_profesor,id_grado) VALUES (?,?,?,?,?,?)";
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            asignarCampos(ps,s);asignarGrado(ps,6,s);ps.executeUpdate();
            try(ResultSet keys=ps.getGeneratedKeys()){if(keys.next())s.setId(keys.getInt(1));}
        }
    }

    public void actualizar(Seccion s) throws SQLException {
        String sql="UPDATE secciones SET aula=?,horario=?,cupo_maximo=?,id_asignatura=?,id_profesor=?,id_grado=? WHERE id_seccion=?";
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){
            asignarCampos(ps,s);asignarGrado(ps,6,s);ps.setInt(7,s.getId());ps.executeUpdate();
        }
    }

    public int contarInscritos(int idSeccion) throws SQLException {
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM detalle_inscripciones WHERE id_seccion=?")){
            ps.setInt(1,idSeccion);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):0;}
        }
    }
    public void eliminar(int idSeccion) throws SQLException {
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("DELETE FROM secciones WHERE id_seccion=?")){ps.setInt(1,idSeccion);ps.executeUpdate();}
    }

    private List<Seccion> consultar(String sql,Integer parametro)throws SQLException{
        List<Seccion> lista=new ArrayList<>();
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){
            if(parametro!=null)ps.setInt(1,parametro);
            try(ResultSet rs=ps.executeQuery()){while(rs.next())lista.add(mapear(rs));}
        }
        return lista;
    }
    private Seccion mapear(ResultSet rs)throws SQLException{
        Asignatura asignatura=new Asignatura(rs.getInt("id_asignatura"),rs.getString("codigo_asignatura"),rs.getString("asignatura_nombre"),rs.getInt("creditos"),rs.getString("descripcion"));
        Profesor profesor=null;int idProfesor=rs.getInt("id_profesor");
        if(!rs.wasNull())profesor=new Profesor(idProfesor,rs.getString("profesor_nombre"),rs.getString("profesor_apellido"),rs.getString("profesor_correo"),null,rs.getString("codigo_profesor"),rs.getString("especialidad"));
        Seccion seccion=new Seccion(rs.getInt("id_seccion"),rs.getString("aula"),rs.getString("horario"),rs.getInt("cupo_maximo"),asignatura,profesor);
        int idGrado=rs.getInt("grado_id");
        if(!rs.wasNull())seccion.setGrado(new GradoCurricular(idGrado,rs.getString("grado_codigo"),rs.getString("grado_nivel"),rs.getString("grado_ciclo"),rs.getString("grado_nombre"),List.of()));
        seccion.setInscritos(rs.getInt("inscritos"));return seccion;
    }
    private void asignarCampos(PreparedStatement ps,Seccion s)throws SQLException{
        ps.setString(1,s.getAula());ps.setString(2,s.getHorario());ps.setInt(3,s.getCupoMaximo());ps.setInt(4,s.getAsignatura().getId());
        if(s.getProfesor()==null)ps.setNull(5,Types.INTEGER);else ps.setInt(5,s.getProfesor().getId());
    }
    private void asignarGrado(PreparedStatement ps,int index,Seccion s)throws SQLException{
        if(s.getGrado()==null)ps.setNull(index,Types.INTEGER);else ps.setInt(index,s.getGrado().id());
    }
}
