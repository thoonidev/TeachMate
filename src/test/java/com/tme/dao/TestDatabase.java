package com.tme.dao;

import java.sql.*;
import java.util.UUID;

/** Disposable databases only. MySQL opt-in is restricted to a local port and a generated schema. */
final class TestDatabase implements AutoCloseable {
    final Connection connection;
    final String url;
    final String user;
    final String password;
    final boolean mysql;
    private final String serverUrl;
    private final String schema;

    TestDatabase() throws SQLException {
        String port = System.getProperty("teachmate.test.mysql.port");
        mysql = port != null;
        schema = "teachmate_validation_" + UUID.randomUUID().toString().replace("-", "");
        if (mysql) {
            int number;
            try { number = Integer.parseInt(port); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("Puerto de pruebas MySQL inválido."); }
            if (number < 1024 || number > 65535) throw new IllegalArgumentException("Puerto de pruebas MySQL inválido.");
            String options = "?sslMode=REQUIRED&connectionTimeZone=UTC&connectTimeout=3000&socketTimeout=10000";
            serverUrl = "jdbc:mysql://127.0.0.1:" + number + "/" + options;
            url = "jdbc:mysql://127.0.0.1:" + number + "/" + schema + options;
            user = "root";
            password = System.getenv().getOrDefault("TEACHMATE_TEST_MYSQL_PASSWORD", "");
            try (Connection server = DriverManager.getConnection(serverUrl, user, password);
                 Statement statement = server.createStatement()) {
                statement.executeUpdate("create database " + schema + " character set utf8mb4");
            }
        } else {
            serverUrl = null;
            url = "jdbc:h2:mem:" + schema + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=5000";
            user = "sa";
            password = "";
        }
        connection = DriverManager.getConnection(url, user, password);
    }

    void clearTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String table : new String[] {"tutoria", "docente", "especialidad", "user_details"})
                statement.executeUpdate("drop table if exists " + table);
        }
    }

    @Override public void close() throws SQLException {
        try { connection.close(); }
        finally {
            if (mysql) {
                try (Connection server = DriverManager.getConnection(serverUrl, user, password);
                     Statement statement = server.createStatement()) {
                    statement.executeUpdate("drop database " + schema);
                }
            }
        }
    }
}
