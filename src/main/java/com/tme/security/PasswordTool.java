package com.tme.security;

import com.tme.db.DBConnection;
import java.io.Console;
import java.sql.*;
import java.util.Arrays;

/** Offline administration tool; never exposed as a servlet. */
public final class PasswordTool {
    private PasswordTool() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Uso: PasswordTool hash | migrate");
        if (args[0].equals("hash")) {
            Console console = System.console();
            if (console == null) throw new IllegalStateException("Ejecute desde una terminal interactiva.");
            char[] password = console.readPassword("Nueva contraseña (12 a 128 caracteres): ");
            char[] confirmation = console.readPassword("Repita la contraseña: ");
            try {
                if (password == null || !Arrays.equals(password, confirmation)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
                console.printf("%s%n", Passwords.hash(new String(password)));
            } finally {
                if (password != null) Arrays.fill(password, '\0');
                if (confirmation != null) Arrays.fill(confirmation, '\0');
            }
        } else if (args[0].equals("migrate")) {
            try {
                int count = migrate(DBConnection.getConn());
                System.out.println("Contraseñas migradas: " + count);
            } finally { DBConnection.closeCurrentConnection(); }
        } else throw new IllegalArgumentException("Opción desconocida.");
    }

    static int migrate(Connection connection) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        int count = 0;
        try {
            for (String table : new String[] {"user_details", "docente"}) {
                try (PreparedStatement select = connection.prepareStatement("select id,clave from " + table + " for update");
                     ResultSet result = select.executeQuery();
                     PreparedStatement update = connection.prepareStatement("update " + table + " set clave=? where id=?")) {
                    while (result.next()) {
                        String stored = result.getString("clave");
                        if (Passwords.isEncoded(stored)) continue;
                        if (stored != null && stored.startsWith("pbkdf2_sha256$")) {
                            throw new IllegalStateException("Hash existente inválido; requiere revisión antes de migrar.");
                        }
                        update.setString(1, Passwords.hashForMigration(stored));
                        update.setInt(2, result.getInt("id"));
                        count += update.executeUpdate();
                    }
                }
            }
            connection.commit();
            return count;
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally { connection.setAutoCommit(autoCommit); }
    }
}
