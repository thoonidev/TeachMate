package com.tme.dao;

import java.sql.*;
import com.tme.modal.User;
import com.tme.security.Passwords;

public class UserDAO {
    private final Connection conn;
    public UserDAO(Connection conn) { this.conn = conn; }

    public boolean userRegister(User user) {
        try (PreparedStatement statement = conn.prepareStatement("insert into user_details(nombre,email,clave) values(?,?,?)")) {
            statement.setString(1, user.getNombre());
            statement.setString(2, user.getEmail());
            statement.setString(3, Passwords.hash(user.getClave()));
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw new IllegalStateException("No se pudo registrar el usuario."); }
    }

    public boolean isUserRegistered(String email) {
        try (PreparedStatement statement = conn.prepareStatement("select id from user_details where email=?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (SQLException exception) { throw new IllegalStateException("No se pudo consultar el usuario."); }
    }

    public User loginUser(String email, String password) {
        try (PreparedStatement statement = conn.prepareStatement("select id,nombre,email,clave from user_details where email=?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !Passwords.verify(password, result.getString("clave"))) return null;
                User user = new User();
                user.setId(result.getInt("id"));
                user.setNombre(result.getString("nombre"));
                user.setEmail(result.getString("email"));
                return user;
            }
        } catch (SQLException exception) { throw new IllegalStateException("No se pudo consultar el usuario."); }
    }

    public boolean checkOldPassword(int id, String password) {
        try (PreparedStatement statement = conn.prepareStatement("select clave from user_details where id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && Passwords.verify(password, result.getString("clave"));
            }
        } catch (SQLException exception) { throw new IllegalStateException("No se pudo consultar el usuario."); }
    }

    public boolean changePassword(int id, String password) {
        try (PreparedStatement statement = conn.prepareStatement("update user_details set clave=? where id=?")) {
            statement.setString(1, Passwords.hash(password));
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw new IllegalStateException("No se pudo actualizar la contraseña."); }
    }
}