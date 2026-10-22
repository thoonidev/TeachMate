package com.tme.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** One lazily opened connection per HTTP request, released by DatabaseConnectionFilter. */
public final class DBConnection {
    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    private DBConnection() { }

    public static Connection getConn() {
        try {
            Connection connection = CURRENT.get();
            if (connection == null || connection.isClosed()) {
                String url = required("DB_URL");
                if (!url.startsWith("jdbc:mysql://")) {
                    throw new IllegalStateException("DB_URL debe ser una URL JDBC de MySQL.");
                }
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, required("DB_USER"), required("DB_PASSWORD"));
                CURRENT.set(connection);
            }
            return connection;
        } catch (SQLException | ClassNotFoundException exception) {
            // Do not expose connection strings or credentials in application errors.
            throw new IllegalStateException("No se pudo conectar con MySQL. Revise la configuración y disponibilidad.");
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de entorno " + name + ".");
        }
        return value;
    }

    public static void closeCurrentConnection() {
        Connection connection = CURRENT.get();
        CURRENT.remove();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException exception) {
                throw new IllegalStateException("No se pudo cerrar la conexión con MySQL.");
            }
        }
    }
}
