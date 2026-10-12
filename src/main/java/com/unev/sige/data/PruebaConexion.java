package com.unev.sige.data;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Prueba rapida de conexion a sige_db usando la misma configuracion que el resto de la app
 * (variables de entorno o db.properties).
 */
public class PruebaConexion {

    public static void main(String[] args) {
        System.out.println("Intentando conectar a la base de datos sige_db...");

        try (Connection conn = ConexionBD.obtener()) {
            if (conn != null && conn.isValid(3)) {
                System.out.println("--------------------------------------------------");
                System.out.println("¡CONEXIÓN EXITOSA CON MYSQL Y SIGE_DB!");
                System.out.println("--------------------------------------------------");
            }
        } catch (SQLException e) {
            System.err.println("Error al conectar a MySQL. Verifica db.properties, el usuario, la contraseña, "
                    + "que MySQL esté activo y que el driver esté en el pom.xml.");
            e.printStackTrace();
        }
    }
}
