package com.tme.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.tme.modal.Tutoria;

public class TutoriaDAO {

	private Connection conn;

	public TutoriaDAO(Connection conn) {
		super();
		this.conn = conn;
	}

	//para crear tutoria
	public boolean addTutoria(Tutoria tutoria) {

		boolean f = false;

		try {

			String sql = "insert into tutoria(userId, nombre, genero, edad, ftutoria, email, telefono, experiencia, docenteId, direccion, estado) values(?,?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, tutoria.getUserId());
			pstmt.setString(2, tutoria.getNombre());
			pstmt.setString(3, tutoria.getGenero());
			pstmt.setString(4, tutoria.getEdad());
			pstmt.setString(5, tutoria.getFtutoria());
			pstmt.setString(6, tutoria.getEmail());
			pstmt.setString(7, tutoria.getTelefono());
			pstmt.setString(8, tutoria.getExperiencia());
			pstmt.setInt(9, tutoria.getDocenteId());
			pstmt.setString(10, tutoria.getDireccion());
			pstmt.setString(11, tutoria.getEstado());

			f = pstmt.executeUpdate() == 1;

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return f;
	}

	// obtener una lista de tutorias para un usuario específico que ha iniciado sesión 
	//mostrar lista de tutorias para panel de usuario específico
	public List<Tutoria> getAllTutoriaByLoginUser(int userId) {
		List<Tutoria> tutList = new ArrayList<Tutoria>();

		Tutoria tutoria = null;

		try {

			String sql = "select * from tutoria where userId=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, userId);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				tutoria = new Tutoria();

				tutoria.setId(resultSet.getInt(1));// nombrar id
				tutoria.setUserId(resultSet.getInt(2));// userId
				tutoria.setNombre(resultSet.getString(3));
				tutoria.setGenero(resultSet.getString(4));
				tutoria.setEdad(resultSet.getString(5));
				tutoria.setFtutoria(resultSet.getString(6));
				tutoria.setEmail(resultSet.getString(7));
				tutoria.setTelefono(resultSet.getString(8));
				tutoria.setExperiencia(resultSet.getString(9));
				tutoria.setDocenteId(resultSet.getInt(10));
				tutoria.setDireccion(resultSet.getString(11));
				tutoria.setEstado(resultSet.getString(12));
				tutList.add(tutoria);

			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return tutList;

	}

	// obtener lista de tutorias de alumnos para un docente específico.
	//mostrar lista de tutorias en panel docente específico 
	public List<Tutoria> getAllTutoriaByLoginDocente(int docenteId) {
		List<Tutoria> tutList = new ArrayList<Tutoria>();

		Tutoria tutoria = null;

		try {

			String sql = "select * from tutoria where docenteId=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, docenteId);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				tutoria = new Tutoria();

				tutoria.setId(resultSet.getInt(1));// nombrar id
				tutoria.setUserId(resultSet.getInt(2));// userId
				tutoria.setNombre(resultSet.getString(3));
				tutoria.setGenero(resultSet.getString(4));
				tutoria.setEdad(resultSet.getString(5));
				tutoria.setFtutoria(resultSet.getString(6));
				tutoria.setEmail(resultSet.getString(7));
				tutoria.setTelefono(resultSet.getString(8));
				tutoria.setExperiencia(resultSet.getString(9));
				tutoria.setDocenteId(resultSet.getInt(10));
				tutoria.setDireccion(resultSet.getString(11));
				tutoria.setEstado(resultSet.getString(12));
				tutList.add(tutoria);

			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return tutList;

	}

	// para el comentario del docente se necesita id tutoria específica
	public Tutoria getTutoriaById(int id) {

		Tutoria tutoria = null;

		try {

			String sql = "select * from tutoria where id=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			pstmt.setInt(1, id);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				tutoria = new Tutoria();

				tutoria.setId(resultSet.getInt(1));// nombrar id
				tutoria.setUserId(resultSet.getInt(2));// userId
				tutoria.setNombre(resultSet.getString(3));
				tutoria.setGenero(resultSet.getString(4));
				tutoria.setEdad(resultSet.getString(5));
				tutoria.setFtutoria(resultSet.getString(6));
				tutoria.setEmail(resultSet.getString(7));
				tutoria.setTelefono(resultSet.getString(8));
				tutoria.setExperiencia(resultSet.getString(9));
				tutoria.setDocenteId(resultSet.getInt(10));
				tutoria.setDireccion(resultSet.getString(11));
				tutoria.setEstado(resultSet.getString(12));

			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return tutoria;

	}

	// para actualizar el estado del comentario
	public boolean updatePfTutoriaComentarioEstado(int tuttId, int docId, String comentario) {

		boolean f = false;

		try {

			String sql = "update tutoria set estado=? where id=? and docenteId=?";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			pstmt.setString(1, comentario);
			pstmt.setInt(2, tuttId);
			pstmt.setInt(3, docId);

			f = pstmt.executeUpdate() == 1;

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return f;
	}

	// obtener todas las tutorias en el panel de administración
	public List<Tutoria> getAllTutoria() {
		List<Tutoria> tutList = new ArrayList<Tutoria>();
		Tutoria tutoria = null;

		try {

			String sql = "select * from tutoria order by id desc";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				tutoria = new Tutoria();

				tutoria.setId(resultSet.getInt(1));// nombrar id
				tutoria.setUserId(resultSet.getInt(2));// userId
				tutoria.setNombre(resultSet.getString(3));
				tutoria.setGenero(resultSet.getString(4));
				tutoria.setEdad(resultSet.getString(5));
				tutoria.setFtutoria(resultSet.getString(6));
				tutoria.setEmail(resultSet.getString(7));
				tutoria.setTelefono(resultSet.getString(8));
				tutoria.setExperiencia(resultSet.getString(9));
				tutoria.setDocenteId(resultSet.getInt(10));
				tutoria.setDireccion(resultSet.getString(11));
				tutoria.setEstado(resultSet.getString(12));
				tutList.add(tutoria);

			}

		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}

		return tutList;
	}

}
