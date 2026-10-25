package com.unev.sige.data;

import com.unev.sige.model.GradoCurricular;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas del catalogo curricular nacional cargado por sql/fase_curriculo_grados.sql. */
public final class CurriculoDAO {
    public List<GradoCurricular> listarGrados() throws SQLException {
        String sql = "SELECT g.id,g.codigo,g.nivel,g.ciclo,g.nombre,a.nombre AS area,ga.horas_semanales "
                + "FROM curriculo_grados g LEFT JOIN curriculo_grado_area ga ON ga.id_grado=g.id "
                + "LEFT JOIN areas_curriculares a ON a.id=ga.id_area ORDER BY g.orden,a.nombre";
        Map<String, GradoCurricular> grades = new LinkedHashMap<>();
        Map<String, List<String>> areas = new LinkedHashMap<>();
        try (Connection connection = ConexionBD.obtener();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String code = result.getString("codigo");
                grades.putIfAbsent(code, new GradoCurricular(result.getInt("id"), code, result.getString("nivel"),
                        result.getString("ciclo"), result.getString("nombre"), List.of()));
                String area = result.getString("area");
                if (area != null) {
                    String hours = result.getString("horas_semanales");
                    areas.computeIfAbsent(code, ignored -> new ArrayList<>())
                            .add(area + (hours == null ? "" : " · " + hours + " h/sem"));
                }
            }
        }
        List<GradoCurricular> result = new ArrayList<>();
        grades.forEach((code, grade) -> result.add(new GradoCurricular(grade.id(), grade.codigo(), grade.nivel(),
                grade.ciclo(), grade.nombre(), areas.getOrDefault(code, List.of()))));
        return result;
    }
    public Map<String,String> listarAreas() throws SQLException {
        Map<String,String> areas=new LinkedHashMap<>();
        try(Connection c=ConexionBD.obtener();PreparedStatement ps=c.prepareStatement("SELECT codigo,nombre FROM areas_curriculares ORDER BY nombre");ResultSet rs=ps.executeQuery()){
            while(rs.next())areas.put(rs.getString("codigo"),rs.getString("nombre"));
        }
        return areas;
    }
}
