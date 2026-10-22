package com.tme.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.tme.modal.Especialidad;

public class EspecialidadDAO {
	
	private Connection conn;

	public EspecialidadDAO(Connection conn) {
		super();
		this.conn = conn;
	}
	
	public boolean addEspecialidad(String es) {
		
		boolean f = false;
		
		try {
			
			String sql = "insert into especialidad (especialidad_nombre) values(?)";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			
			pstmt.setString(1, es);
			
			f = pstmt.executeUpdate() == 1;
			
			
			
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}
		
		
		
		return f;
		
	}
	
	public List<Especialidad> getAllEspecialidad(){
		
		List<Especialidad> espList = new ArrayList<Especialidad>();
		
		Especialidad especialidadObj = null;
		
		try {
			
			String sql = "select * from especialidad";
			PreparedStatement pstmt = this.conn.prepareStatement(sql);
			ResultSet resultSet = pstmt.executeQuery();
			
			while(resultSet.next()) {
				
				//Crear objeto
				especialidadObj = new Especialidad();
				//especialidadObj.setId(resultSet.getInt("id"));
				//especialidadObj.setEspecialidadNombre(resultSet.getString("especialidad_nombre"));
				//encima de dos líneas igual que debajo de la línea
				especialidadObj.setId(resultSet.getInt(1));//número de índice de columna 1 -(id)
				especialidadObj.setEspecialidadNombre(resultSet.getString(2));//índice de columna número 2 -(especialidad_nombre)
				
				//ahora agregue el objeto especialidad a la Lista
				espList.add(especialidadObj);
			}
			
			
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo completar la operación de base de datos.");
		}
		
		
		return espList;
	}

}
