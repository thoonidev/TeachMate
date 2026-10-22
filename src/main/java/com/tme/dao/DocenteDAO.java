package com.tme.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

//import javax.security.auth.message.callback.PrivateKeyCallback.Request;
import javax.servlet.http.HttpSession;

import com.tme.modal.Docente;
import com.tme.security.Passwords;

public class DocenteDAO {

	private Connection conn;

	public DocenteDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	public boolean registerDocente(Docente docente) {

		boolean f = false;

		try {

			String sql = "insert into docente(nombre,fnacim,calificacion,especialidad,email,telefono,clave) values(?,?,?,?,?,?,?)";

			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, docente.getNombre());
			pstmt.setString(2, docente.getFnacim());
			pstmt.setString(3, docente.getCalificacion());
			pstmt.setString(4, docente.getEspecialidad());
			pstmt.setString(5, docente.getEmail());
			pstmt.setString(6, docente.getTelefono());
			pstmt.setString(7, Passwords.hash(docente.getClave()));

			f = pstmt.executeUpdate() == 1;

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return f;
	}

	// getAllDocente list
	public List<Docente> getAllDocente() {

		Docente docente = null;
		List<Docente> docList = new ArrayList<Docente>();

		try {

			String sql = "select * from docente order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				docente = new Docente();

				docente.setId(resultSet.getInt("id"));
				docente.setNombre(resultSet.getString("nombre"));
				docente.setFnacim(resultSet.getString("fnacim"));
				docente.setCalificacion(resultSet.getString("calificacion"));
				docente.setEspecialidad(resultSet.getString("especialidad"));
				docente.setEmail(resultSet.getString("email"));
				docente.setTelefono(resultSet.getString("telefono"));
				docList.add(docente);
			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return docList;
	}

	// obtener docente por id
	public Docente getDocenteById(int id) {

		Docente docente = null;

		try {

			String sql = "select * from docente where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, id);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {
				docente = new Docente();

				docente.setId(resultSet.getInt("id"));
				docente.setNombre(resultSet.getString("nombre"));
				docente.setFnacim(resultSet.getString("fnacim"));
				docente.setCalificacion(resultSet.getString("calificacion"));
				docente.setEspecialidad(resultSet.getString("especialidad"));
				docente.setEmail(resultSet.getString("email"));
				docente.setTelefono(resultSet.getString("telefono"));

			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return docente;
	}

	// actualizar docente por id
	public boolean updateDocente(Docente docente) {
        boolean reset = docente.getClave() != null && !docente.getClave().isEmpty();
        String sql = "update docente set nombre=?,fnacim=?,calificacion=?,especialidad=?,email=?,telefono=?"
                + (reset ? ",clave=?" : "") + " where id=?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, docente.getNombre());
            statement.setString(2, docente.getFnacim());
            statement.setString(3, docente.getCalificacion());
            statement.setString(4, docente.getEspecialidad());
            statement.setString(5, docente.getEmail());
            statement.setString(6, docente.getTelefono());
            if (reset) statement.setString(7, Passwords.hash(docente.getClave()));
            statement.setInt(reset ? 8 : 7, docente.getId());
            return statement.executeUpdate() == 1;
        } catch (java.sql.SQLException exception) { throw new IllegalStateException("No se pudo actualizar el docente."); }
    }

	// eliminar docentes por id
	public boolean deleteDocenteById(int id) {

		boolean f = false;

		try {

			String sql = "delete from docente where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, id);

			f = pstmt.executeUpdate() == 1;

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return f;
	}

	// inicio de sesión docente
	public Docente loginDocente(String email, String password) {
        try (PreparedStatement statement = conn.prepareStatement("select * from docente where email=?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !Passwords.verify(password, result.getString("clave"))) return null;
                Docente docente = new Docente();
                docente.setId(result.getInt("id"));
                docente.setNombre(result.getString("nombre"));
                docente.setFnacim(result.getString("fnacim"));
                docente.setCalificacion(result.getString("calificacion"));
                docente.setEspecialidad(result.getString("especialidad"));
                docente.setEmail(result.getString("email"));
                docente.setTelefono(result.getString("telefono"));
                return docente;
            }
        } catch (java.sql.SQLException exception) { throw new IllegalStateException("No se pudo consultar el docente."); }
    }

	// mostrar el número total de valores dinámicos en el panel de administración

	// método de recuento aquí para reducir la línea de código...
	// Contar el número total de docente
	public int countTotalDocente() {

		int i = 0;

		try {

			String sql = "select * from docente";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			while (resultSet.next()) {

				i++;
			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return i;
	}

	// Contar el número total de tutorias.
	public int countTotalTutoria() {

		int i = 0;

		try {

			String sql = "select * from tutoria";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			while (resultSet.next()) {

				i++;
			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return i;
	}

	// el número total de tutorias para un docente específico.
	public int countTotalTutoriaByDocenteId(int docenteId) {

		int i = 0;

		try {

			String sql = "select * from tutoria where docenteId=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setInt(1, docenteId);

			ResultSet resultSet = pstmt.executeQuery();
			while (resultSet.next()) {

				i++;
			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return i;
	}

	// Contar el número total de usuarios
	public int countTotalUser() {

		int i = 0;

		try {

			String sql = "select * from user_details";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			while (resultSet.next()) {

				i++;
			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return i;
	}

	// Contar el número total de especialidades
	public int countTotalEspecialidad() {

		int i = 0;

		try {

			String sql = "select * from especialidad";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();
			while (resultSet.next()) {

				i++;
			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return i;
	}

	// comprobar la contraseña anterior
	public boolean checkOldPassword(int id, String password) {
        try (PreparedStatement statement = conn.prepareStatement("select clave from docente where id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && Passwords.verify(password, result.getString("clave"));
            }
        } catch (java.sql.SQLException exception) { throw new IllegalStateException("No se pudo consultar el docente."); }
    }

	// cambiar la contraseña
	public boolean changePassword(int id, String password) {
        try (PreparedStatement statement = conn.prepareStatement("update docente set clave=? where id=?")) {
            statement.setString(1, Passwords.hash(password));
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        } catch (java.sql.SQLException exception) { throw new IllegalStateException("No se pudo actualizar la contraseña."); }
    }

	// editar perfil de docente en el panel de docentes editar perfil.
	public boolean editDocenteProfile(Docente docente) {

		boolean f = false;

		try {

			//String sql = "update docente set nombre=?,fnacim=?,calificacion=?,especialidad=?,email=?,telefono=?,clave=? where id=?";
			String sql = "update docente set nombre=?,fnacim=?,calificacion=?,especialidad=?,email=?,telefono=? where id=?";

			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, docente.getNombre());
			pstmt.setString(2, docente.getFnacim());
			pstmt.setString(3, docente.getCalificacion());
			pstmt.setString(4, docente.getEspecialidad());
			pstmt.setString(5, docente.getEmail());
			pstmt.setString(6, docente.getTelefono());
			//pstmt.setString(7, docente.getPassword());
			// Es necesario configurar id también para la actualización.
			pstmt.setInt(7, docente.getId());

			f = pstmt.executeUpdate() == 1;

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return f;
	}

}
