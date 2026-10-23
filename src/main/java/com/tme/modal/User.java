package com.tme.modal;

public class User {
	private int id;
	private String nombre;
	private String email;
	private String clave;
    private String telefono = "";
    public String getTelefono() { return telefono; }
    public void setTelefono(String value) { telefono = value == null ? "" : value; }
	
	
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}


	public User(int id, String nombre, String email, String clave) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.email = email;
		this.clave = clave;
	}


	public User(String nombre, String email, String clave) {
		super();
		this.nombre = nombre;
		this.email = email;
		this.clave = clave;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getNombre() {
		return nombre;
	}


	public void setNombre(String nombre) {
		this.nombre = nombre;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getClave() {
		return clave;
	}


	public void setClave(String clave) {
		this.clave = clave;
	}


	@Override
	public String toString() {
		return "User [id=" + id + ", nombre=" + nombre + ", email=" + email + ", clave=" + clave + "]";
	}
	
	
	
	
	
	
	
	
}
