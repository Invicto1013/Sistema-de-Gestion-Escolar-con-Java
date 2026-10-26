package com.unev.sige.data;

import com.unev.sige.model.Rol;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas del aula virtual, limitadas a las secciones y matrículas del usuario. */
public final class AulaDAO {
    public record Material(int id,int idSeccion,String asignatura,String grado,String tipo,String titulo,String descripcion,
                           LocalDate fechaLimite,String fechaPublicacion,String archivoNombre,long archivoBytes,String storageKey,
                           String entregaNombre,long entregaBytes,String entregaFecha,int entregaId,String entregaEstudiante) { }
    public record Archivo(String nombre,String storageKey,String tipo) { }

    /** Guarda el contenido de un archivo en MySQL (tabla aula_archivos) y devuelve su clave. */
    public String guardarArchivo(byte[] datos) throws SQLException {
        String key=java.util.UUID.randomUUID().toString();
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("INSERT INTO aula_archivos(archivo_key,contenido) VALUES(?,?)")){
            ps.setString(1,key);ps.setBytes(2,datos);ps.executeUpdate();
        }
        return key;
    }

    /** Devuelve el contenido guardado para la clave, o null si no existe (por ejemplo, archivos antiguos en disco). */
    public byte[] leerArchivo(String key) throws SQLException {
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("SELECT contenido FROM aula_archivos WHERE archivo_key=?")){
            ps.setString(1,key);
            try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getBytes(1):null;}
        }
    }

    public void eliminarArchivo(String key) throws SQLException {
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("DELETE FROM aula_archivos WHERE archivo_key=?")){
            ps.setString(1,key);ps.executeUpdate();
        }
    }

    /** Borra contenidos que ya no pertenecen a ningún material ni entrega (tareas reemplazadas, secciones eliminadas). */
    public int limpiarArchivosHuerfanos() throws SQLException {
        String sql="DELETE FROM aula_archivos WHERE fecha<NOW()-INTERVAL 1 HOUR "
                +"AND archivo_key NOT IN(SELECT archivo_key FROM aula_materiales WHERE archivo_key IS NOT NULL) "
                +"AND archivo_key NOT IN(SELECT archivo_key FROM aula_entregas)";
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){return ps.executeUpdate();}
    }

    public List<Material> paraProfesor(int id) throws SQLException {
        String sql="SELECT m.*,a.nombre AS asignatura,g.nombre AS grado,e.id_entrega,e.archivo_nombre AS entrega_nombre,e.archivo_bytes AS entrega_bytes,DATE_FORMAT(e.fecha_entrega,'%Y-%m-%d %H:%i') AS entrega_fecha,CONCAT(eu.nombre,' ',eu.apellido) AS entrega_estudiante FROM aula_materiales m JOIN secciones s ON s.id_seccion=m.id_seccion JOIN asignaturas a ON a.id_asignatura=s.id_asignatura LEFT JOIN curriculo_grados g ON g.id=s.id_grado LEFT JOIN aula_entregas e ON e.id_material=m.id_material LEFT JOIN usuarios eu ON eu.id_usuario=e.id_estudiante WHERE m.id_profesor=? ORDER BY m.fecha_publicacion DESC,e.fecha_entrega DESC";
        return listar(sql,id);
    }
    public List<Material> paraEstudiante(int id) throws SQLException {
        String sql="SELECT m.*,a.nombre AS asignatura,g.nombre AS grado,e.id_entrega,e.archivo_nombre AS entrega_nombre,e.archivo_bytes AS entrega_bytes,DATE_FORMAT(e.fecha_entrega,'%Y-%m-%d %H:%i') AS entrega_fecha,CONCAT(eu.nombre,' ',eu.apellido) AS entrega_estudiante FROM aula_materiales m JOIN secciones s ON s.id_seccion=m.id_seccion JOIN asignaturas a ON a.id_asignatura=s.id_asignatura LEFT JOIN curriculo_grados g ON g.id=s.id_grado LEFT JOIN aula_entregas e ON e.id_material=m.id_material AND e.id_estudiante=? LEFT JOIN usuarios eu ON eu.id_usuario=e.id_estudiante WHERE EXISTS(SELECT 1 FROM detalle_inscripciones d JOIN inscripciones i ON i.id_inscripcion=d.id_inscripcion WHERE d.id_seccion=s.id_seccion AND i.id_estudiante=?) ORDER BY m.fecha_publicacion DESC";
        return listar(sql,id);
    }
    private List<Material> listar(String sql,int id) throws SQLException {
        List<Material> rows=new ArrayList<>();
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,id);if(sql.contains("aula_entregas e ON e.id_material=m.id_material AND e.id_estudiante=?"))ps.setInt(2,id);try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                Date due=rs.getDate("fecha_limite");
                rows.add(new Material(rs.getInt("id_material"),rs.getInt("id_seccion"),rs.getString("asignatura"),rs.getString("grado"),rs.getString("tipo"),rs.getString("titulo"),rs.getString("descripcion"),due==null?null:due.toLocalDate(),rs.getString("fecha_publicacion"),rs.getString("archivo_nombre"),rs.getLong("archivo_bytes"),rs.getString("archivo_key"),rs.getString("entrega_nombre"),rs.getLong("entrega_bytes"),rs.getString("entrega_fecha"),rs.getInt("id_entrega"),rs.getString("entrega_estudiante")));
            }}
        }return rows;
    }
    public int publicar(int profesor,int seccion,String tipo,String titulo,String descripcion,LocalDate limite,String key,String nombre,String mime,long bytes) throws SQLException {
        String sql="INSERT INTO aula_materiales(id_seccion,id_profesor,tipo,titulo,descripcion,fecha_limite,archivo_key,archivo_nombre,archivo_tipo,archivo_bytes) SELECT s.id_seccion,s.id_profesor,?,?,?,?,?,?,?,? FROM secciones s WHERE s.id_seccion=? AND s.id_profesor=?";
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1,tipo);ps.setString(2,titulo);ps.setString(3,descripcion);if(limite==null)ps.setNull(4,Types.DATE);else ps.setDate(4,java.sql.Date.valueOf(limite));ps.setString(5,key);ps.setString(6,nombre);ps.setString(7,mime);ps.setLong(8,bytes);ps.setInt(9,seccion);ps.setInt(10,profesor);
            if(ps.executeUpdate()!=1)throw new SQLException("La sección no pertenece al profesor actual.");try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())return rs.getInt(1);}throw new SQLException("MySQL no devolvió el identificador del material.");
        }
    }
    public void entregar(int estudiante,int material,String key,String nombre,String mime,long bytes) throws SQLException {
        String sql="INSERT INTO aula_entregas(id_material,id_estudiante,archivo_key,archivo_nombre,archivo_tipo,archivo_bytes) SELECT m.id_material,?, ?,?,?,? FROM aula_materiales m WHERE m.id_material=? AND m.tipo='TAREA' AND EXISTS(SELECT 1 FROM detalle_inscripciones d JOIN inscripciones i ON i.id_inscripcion=d.id_inscripcion WHERE d.id_seccion=m.id_seccion AND i.id_estudiante=?) ON DUPLICATE KEY UPDATE archivo_key=VALUES(archivo_key),archivo_nombre=VALUES(archivo_nombre),archivo_tipo=VALUES(archivo_tipo),archivo_bytes=VALUES(archivo_bytes),fecha_entrega=CURRENT_TIMESTAMP";
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,estudiante);ps.setString(2,key);ps.setString(3,nombre);ps.setString(4,mime);ps.setLong(5,bytes);ps.setInt(6,material);ps.setInt(7,estudiante);int changed=ps.executeUpdate();if(changed==0){try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM aula_entregas WHERE id_material=? AND id_estudiante=?")){check.setInt(1,material);check.setInt(2,estudiante);try(ResultSet rs=check.executeQuery()){if(!rs.next())throw new SQLException("La tarea no existe o el estudiante no está inscrito en su sección.");}}}}
    }
    public Archivo archivoMaterial(int user,Rol rol,int material) throws SQLException {
        String condition=rol==Rol.PROFESOR?"m.id_profesor=?":"EXISTS(SELECT 1 FROM detalle_inscripciones d JOIN inscripciones i ON i.id_inscripcion=d.id_inscripcion WHERE d.id_seccion=m.id_seccion AND i.id_estudiante=?)";
        String sql="SELECT m.archivo_nombre,m.archivo_key,m.archivo_tipo FROM aula_materiales m WHERE m.id_material=? AND m.archivo_key IS NOT NULL AND "+condition;
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,material);ps.setInt(2,user);try(ResultSet rs=ps.executeQuery()){return rs.next()?new Archivo(rs.getString(1),rs.getString(2),rs.getString(3)):null;}}
    }
    public Archivo archivoEntrega(int user,Rol rol,int entrega) throws SQLException {
        String owner=rol==Rol.PROFESOR?"m.id_profesor=?":"e.id_estudiante=?";String sql="SELECT e.archivo_nombre,e.archivo_key,e.archivo_tipo FROM aula_entregas e JOIN aula_materiales m ON m.id_material=e.id_material WHERE e.id_entrega=? AND "+owner;
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,entrega);ps.setInt(2,user);try(ResultSet rs=ps.executeQuery()){return rs.next()?new Archivo(rs.getString(1),rs.getString(2),rs.getString(3)):null;}}
    }
}
