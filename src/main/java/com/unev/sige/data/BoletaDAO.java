package com.unev.sige.data;

import com.unev.sige.model.Calificacion;
import com.unev.sige.model.LineaBoleta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consultas de boleta con autorización según el rol que la solicita. */
public class BoletaDAO {
    private static final String SELECT = """
            SELECT i.periodo_lectivo, a.codigo_asignatura, a.nombre AS asignatura,
                   c.nota_parcial1, c.nota_parcial2, c.nota_examen_final, c.nota_final,
                   c.calificacion_literal, d.estado_materia
            FROM inscripciones i
            JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
            JOIN secciones s ON s.id_seccion = d.id_seccion
            JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
            LEFT JOIN calificaciones c ON c.id_detalle_inscripcion = d.id_detalle_inscripcion
            """;

    public List<LineaBoleta> listarParaEstudiante(int idEstudiante) throws SQLException {
        return consultar(SELECT + " WHERE i.id_estudiante = ? ORDER BY i.periodo_lectivo DESC, a.codigo_asignatura",
                idEstudiante);
    }

    public List<LineaBoleta> listarParaPadre(int idUsuarioPadre, int idEstudiante) throws SQLException {
        String sql = """
                SELECT i.periodo_lectivo, a.codigo_asignatura, a.nombre AS asignatura,
                       c.nota_parcial1, c.nota_parcial2, c.nota_examen_final, c.nota_final,
                       c.calificacion_literal, d.estado_materia
                FROM estudiante_padre ep
                JOIN padres p ON p.id_padre = ep.id_padre
                JOIN inscripciones i ON i.id_estudiante = ep.id_estudiante
                JOIN detalle_inscripciones d ON d.id_inscripcion = i.id_inscripcion
                JOIN secciones s ON s.id_seccion = d.id_seccion
                JOIN asignaturas a ON a.id_asignatura = s.id_asignatura
                LEFT JOIN calificaciones c ON c.id_detalle_inscripcion = d.id_detalle_inscripcion
                WHERE p.id_usuario = ? AND ep.id_estudiante = ?
                ORDER BY i.periodo_lectivo DESC, a.codigo_asignatura
                """;
        return consultar(sql, idUsuarioPadre, idEstudiante);
    }

    public List<LineaBoleta> listarParaAdministrador(int idEstudiante) throws SQLException {
        return consultar(SELECT + " WHERE i.id_estudiante = ? ORDER BY i.periodo_lectivo DESC, a.codigo_asignatura",
                idEstudiante);
    }

    private List<LineaBoleta> consultar(String sql, int... parametros) throws SQLException {
        List<LineaBoleta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtener(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) ps.setInt(i + 1, parametros[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(new LineaBoleta(rs.getString("periodo_lectivo"),
                        rs.getString("codigo_asignatura"), rs.getString("asignatura"),
                        Calificacion.limitar(rs.getDouble("nota_parcial1")),
                        Calificacion.limitar(rs.getDouble("nota_parcial2")),
                        Calificacion.limitar(rs.getDouble("nota_examen_final")),
                        Calificacion.limitar(rs.getDouble("nota_final")), rs.getString("calificacion_literal"),
                        rs.getString("estado_materia")));
            }
        }
        return lista;
    }
}
