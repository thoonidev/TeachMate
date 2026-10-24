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
        try (PreparedStatement statement = conn.prepareStatement("select id,nombre,email,clave,telefono from user_details where email=?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !Passwords.verify(password, result.getString("clave"))) return null;
                User user = new User();
                user.setId(result.getInt("id"));
                user.setNombre(result.getString("nombre"));
                user.setEmail(result.getString("email"));
                user.setTelefono(result.getString("telefono"));
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
    public User getById(int id) {
        try (PreparedStatement s=conn.prepareStatement("select id,nombre,email,telefono from user_details where id=?")) {
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("La cuenta no existe.");User u=new User();u.setId(id);u.setNombre(r.getString("nombre"));u.setEmail(r.getString("email"));u.setTelefono(r.getString("telefono"));return u;}
        }catch(SQLException e){throw new IllegalStateException("No se pudo consultar el perfil.");}
    }
    public void guardarPerfil(int id,String nombre,String telefono) {
        nombre=OfertaDAO.texto(nombre,45,"Nombre");telefono=OfertaDAO.texto(telefono,45,"Teléfono");
        try(PreparedStatement s=conn.prepareStatement("update user_details set nombre=?,telefono=? where id=?")){s.setString(1,nombre);s.setString(2,telefono);s.setInt(3,id);if(s.executeUpdate()!=1)throw new IllegalArgumentException("La cuenta no existe.");}
        catch(SQLException e){throw new IllegalStateException("No se pudo actualizar el perfil.");}
    }
}