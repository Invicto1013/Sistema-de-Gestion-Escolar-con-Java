package com.unev.sige.data;

import com.unev.sige.model.Asignatura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Acceso a MySQL para la tabla `asignaturas`.
 */
public class AsignaturaDAO {

    public List<Asignatura> listarTodas() throws SQLException {
        String sql = "SELECT a.id_asignatura,a.codigo_asignatura,a.nombre,a.creditos,a.descripcion,"
                + "(SELECT GROUP_CONCAT(DISTINCT ar.nombre ORDER BY ar.nombre SEPARATOR ', ') FROM asignatura_grado_area aga JOIN areas_curriculares ar ON ar.id=aga.id_area WHERE aga.id_asignatura=a.id_asignatura) AS areas, (SELECT MIN(ar.codigo) FROM asignatura_grado_area aga JOIN areas_curriculares ar ON ar.id=aga.id_area WHERE aga.id_asignatura=a.id_asignatura) AS area_codigo, (SELECT GROUP_CONCAT(aga.id_grado) FROM asignatura_grado_area aga WHERE aga.id_asignatura=a.id_asignatura) AS grado_ids,"
                + "(SELECT GROUP_CONCAT(DISTINCT g.nombre ORDER BY g.orden SEPARATOR ', ') FROM asignatura_grado_area aga JOIN curriculo_grados g ON g.id=aga.id_grado WHERE aga.id_asignatura=a.id_asignatura) AS grados "
                + "FROM asignaturas a ORDER BY a.codigo_asignatura";
        List<Asignatura> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Asignatura asignatura=new Asignatura(rs.getInt("id_asignatura"),rs.getString("codigo_asignatura"),rs.getString("nombre"),rs.getInt("creditos"),rs.getString("descripcion"));
                asignatura.setAreaCurricular(rs.getString("areas")); asignatura.setGradosCurriculares(rs.getString("grados")); asignatura.setCodigoArea(rs.getString("area_codigo")); asignatura.setIdsGrados(rs.getString("grado_ids")); lista.add(asignatura);
            }
        }
        return lista;
    }

    public boolean puedeOfrecerse(int idAsignatura,int idGrado) throws SQLException {
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("SELECT 1 FROM asignatura_grado_area WHERE id_asignatura=? AND id_grado=? LIMIT 1")){
            ps.setInt(1,idAsignatura);ps.setInt(2,idGrado);try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }
    }

    public void guardarCurriculo(int idAsignatura, String codigoArea, List<Integer> grados) throws SQLException {
        String findArea="SELECT id FROM areas_curriculares WHERE codigo=?";
        String delete="DELETE FROM asignatura_grado_area WHERE id_asignatura=?";
        try(Connection c=ConexionBD.obtener()){
            c.setAutoCommit(false);
            try(PreparedStatement area=c.prepareStatement(findArea);PreparedStatement borrar=c.prepareStatement(delete)){
                area.setString(1,codigoArea);int idArea;
                try(ResultSet rs=area.executeQuery()){if(!rs.next())throw new SQLException("Área curricular desconocida.");idArea=rs.getInt(1);}
                borrar.setInt(1,idAsignatura);borrar.executeUpdate();
                try(PreparedStatement add=c.prepareStatement("INSERT INTO asignatura_grado_area(id_asignatura,id_grado,id_area) VALUES(?,?,?)")){
                    for(int idGrado:grados){add.setInt(1,idAsignatura);add.setInt(2,idGrado);add.setInt(3,idArea);add.addBatch();} add.executeBatch();
                }
                c.commit();
            }catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
        }
    }
    /**
     * Indica si ya existe otra asignatura con ese codigo.
     *
     * @param idExcluido id de la asignatura que se esta editando (0 si es nueva)
     */
    public boolean existeCodigo(String codigo, int idExcluido) throws SQLException {
        String sql = "SELECT 1 FROM asignaturas WHERE codigo_asignatura = ? AND id_asignatura <> ? LIMIT 1";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserta la asignatura y deja en el objeto el id generado.
     */
    public void insertar(Asignatura a) throws SQLException {
        String sql = "INSERT INTO asignaturas (codigo_asignatura, nombre, creditos, descripcion) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getCodigo());
            ps.setString(2, a.getNombre());
            ps.setInt(3, a.getCreditos());
            ps.setString(4, a.getDescripcion());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getInt(1));
                    asignarAreaPorNombre(conn,a);
                }
            }
        }
    }

    private void asignarAreaPorNombre(Connection conn,Asignatura asignatura)throws SQLException{
        String nombre=Normalizer.normalize(asignatura.getNombre(),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        String codigo=nombre.contains("matem")?"MATEMATICA":nombre.contains("espanol")||nombre.contains("lengua")?"LENGUA":nombre.contains("social")?"SOCIALES":nombre.contains("natur")?"NATURALES":nombre.contains("ingles")?"INGLES":nombre.contains("frances")?"FRANCES":nombre.contains("relig")?"FHR":nombre.contains("fisica")?"ED_FISICA":nombre.contains("art")?"ARTISTICA":null;
        if(codigo==null)return;
        String sql="INSERT IGNORE INTO asignatura_grado_area(id_asignatura,id_grado,id_area) SELECT ?,cg.id_grado,a.id FROM areas_curriculares a JOIN curriculo_grado_area cg ON cg.id_area=a.id WHERE a.codigo=?";
        try(PreparedStatement ps=conn.prepareStatement(sql)){ps.setInt(1,asignatura.getId());ps.setString(2,codigo);ps.executeUpdate();}
    }

    public void actualizar(Asignatura a) throws SQLException {
        String sql = "UPDATE asignaturas SET codigo_asignatura = ?, nombre = ?, creditos = ?, descripcion = ? "
                + "WHERE id_asignatura = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getCodigo());
            ps.setString(2, a.getNombre());
            ps.setInt(3, a.getCreditos());
            ps.setString(4, a.getDescripcion());
            ps.setInt(5, a.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Indica si la asignatura tiene secciones. La pantalla lo consulta antes de eliminar,
     * porque en la base de datos borrar una asignatura borraria en cascada sus secciones,
     * y con ellas las inscripciones y calificaciones.
     */
    public boolean tieneSecciones(int idAsignatura) throws SQLException {
        String sql = "SELECT 1 FROM secciones WHERE id_asignatura = ? LIMIT 1";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAsignatura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Elimina la asignatura. Llamar solo si no tiene secciones (ver tieneSecciones).
     */
    public void eliminar(int idAsignatura) throws SQLException {
        String sql = "DELETE FROM asignaturas WHERE id_asignatura = ?";
        try (Connection conn = ConexionBD.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAsignatura);
            ps.executeUpdate();
        }
    }
}
