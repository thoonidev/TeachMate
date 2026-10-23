package com.tme.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.tme.modal.Docente;
import com.tme.security.Passwords;

public class DocenteDAO {
    private final Connection conn;
    public DocenteDAO(Connection conn) { this.conn = conn; }

    public boolean registerDocente(Docente docente) {
        try (PreparedStatement statement = conn.prepareStatement("insert into docente(nombre,fnacim,calificacion,email,telefono,presentacion,niveles,clave) values(?,?,?,?,?,?,?,?)")) {
            profile(statement, docente);
            statement.setString(8, Passwords.hash(docente.getClave()));
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    public List<Docente> getAllDocente() { return list(false); }
    public List<Docente> getActiveDocentes() { return list(true); }
    private List<Docente> list(boolean activeOnly) {
        List<Docente> list = new ArrayList<>();
        String sql = "select d.*, coalesce((select group_concat(e.especialidad_nombre order by e.especialidad_nombre separator ', ') from docente_curso dc join especialidad e on e.id=dc.curso_id where dc.docente_id=d.id),'') as especialidad from docente d" + (activeOnly ? " where activo=true" : "") + " order by id desc";
        try (PreparedStatement statement = conn.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) list.add(read(result));
            return list;
        } catch (SQLException exception) { throw failure(); }
    }

    public Docente getDocenteById(int id) {
        try (PreparedStatement statement = conn.prepareStatement("select d.*, coalesce((select group_concat(e.especialidad_nombre order by e.especialidad_nombre separator ', ') from docente_curso dc join especialidad e on e.id=dc.curso_id where dc.docente_id=d.id),'') as especialidad from docente d where id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? read(result) : null; }
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean isActiveDocente(int id) {
        try (PreparedStatement statement = conn.prepareStatement("select activo from docente where id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next() && result.getBoolean(1); }
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean updateDocente(Docente docente) {
        boolean reset = docente.getClave() != null && !docente.getClave().isEmpty();
        String sql = "update docente set nombre=?,fnacim=?,calificacion=?,email=?,telefono=?,presentacion=?,niveles=?"
                + (reset ? ",clave=?" : "") + " where id=?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            profile(statement, docente);
            if (reset) statement.setString(8, Passwords.hash(docente.getClave()));
            statement.setInt(reset ? 9 : 8, docente.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean deactivateDocenteById(int id) {
        try (PreparedStatement statement = conn.prepareStatement("update docente set activo=false where id=? and activo=true")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean reactivateDocenteById(int id) {
        try (PreparedStatement statement = conn.prepareStatement("update docente set activo=true where id=? and activo=false")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    public Docente loginDocente(String email, String password) {
        try (PreparedStatement statement = conn.prepareStatement("select d.*, coalesce((select group_concat(e.especialidad_nombre order by e.especialidad_nombre separator ', ') from docente_curso dc join especialidad e on e.id=dc.curso_id where dc.docente_id=d.id),'') as especialidad from docente d where email=? and activo=true")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && Passwords.verify(password, result.getString("clave")) ? read(result) : null;
            }
        } catch (SQLException exception) { throw failure(); }
    }

    public int countTotalDocente() { return count("select count(*) from docente", null); }
    public int countActiveDocente() { return count("select count(*) from docente where activo=true", null); }
    public int countTotalTutoria() { return count("select count(*) from tutoria", null); }
    public int countTotalUser() { return count("select count(*) from user_details", null); }
    public int countTotalEspecialidad() { return count("select count(*) from especialidad", null); }
    public int countTotalTutoriaByDocenteId(int id) { return count("select count(*) from tutoria where docenteId=?", id); }
    private int count(String sql, Integer id) {
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            if (id != null) statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); }
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean checkOldPassword(int id, String password) {
        try (PreparedStatement statement = conn.prepareStatement("select clave from docente where id=? and activo=true")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { return result.next() && Passwords.verify(password, result.getString(1)); }
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean changePassword(int id, String password) {
        try (PreparedStatement statement = conn.prepareStatement("update docente set clave=? where id=? and activo=true")) {
            statement.setString(1, Passwords.hash(password));
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    public boolean editDocenteProfile(Docente docente) {
        try (PreparedStatement statement = conn.prepareStatement("update docente set nombre=?,fnacim=?,calificacion=?,email=?,telefono=?,presentacion=?,niveles=? where id=? and activo=true")) {
            profile(statement, docente);
            statement.setInt(8, docente.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw failure(); }
    }

    private void profile(PreparedStatement statement, Docente docente) throws SQLException {
        statement.setString(1, docente.getNombre());
        statement.setString(2, docente.getFnacim());
        statement.setString(3, docente.getCalificacion());
        statement.setString(4, docente.getEmail());
        statement.setString(5, docente.getTelefono());
        statement.setString(6, docente.getPresentacion());
        statement.setString(7, docente.getNiveles());
    }
    private Docente read(ResultSet result) throws SQLException {
        Docente docente = new Docente(result.getInt("id"), result.getString("nombre"), result.getString("fnacim"),
                result.getString("calificacion"), result.getString("especialidad"), result.getString("email"), result.getString("telefono"), null);
        docente.setActivo(result.getBoolean("activo"));
        docente.setPresentacion(result.getString("presentacion"));
        docente.setNiveles(result.getString("niveles"));
        return docente;
    }
    private IllegalStateException failure() { return new IllegalStateException("No se pudo completar la operación de docentes."); }
}
