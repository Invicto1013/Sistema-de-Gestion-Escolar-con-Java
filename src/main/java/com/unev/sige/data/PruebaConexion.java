package com.unev.sige.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PruebaConexion {

    // Configuración de MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/sige_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root"; // Tu usuario de MySQL
    private static final String PASSWORD = "1234567890"; // Cambia esto por tu contraseña real de MySQL Workbench

    public static void main(String[] args) {
        System.out.println("Intentando conectar a la base de datos sige_db...");

        try {
            // Cargar el driver JDBC de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Intentar establecer la conexión
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                if (conn != null) {
                    System.out.println("--------------------------------------------------");
                    System.out.println("¡CONEXIÓN EXITOSA CON MYSQL WORKBENCH Y SIGE_DB!");
                    System.out.println("--------------------------------------------------");
                }
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver JDBC de MySQL. Asegúrate de tener la dependencia en tu pom.xml.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos MySQL. Verifica el usuario, contraseña o si MySQL está activo.");
            e.printStackTrace();
        }
    }
}