package com.tme.modal;

public class Docente {
	
	private int id;
	private String nombre;
	private String fnacim;
	private String calificacion;
	private String especialidad;
	private String email;
	private String telefono;
	private String clave;
	private boolean activo = true;
	
	
	public Docente() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Docente(String nombre, String fnacim, String calificacion, String especialidad, String email,
			String telefono, String clave) {
		super();
		this.nombre = nombre;
		this.fnacim = fnacim;
		this.calificacion = calificacion;
		this.especialidad = especialidad;
		this.email = email;
		this.telefono = telefono;
		this.clave = clave;
	}
	
	
	


	public Docente(int id, String nombre, String fnacim, String calificacion, String especialidad, String email,
			String telefono, String clave) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.fnacim = fnacim;
		this.calificacion = calificacion;
		this.especialidad = especialidad;
		this.email = email;
		this.telefono = telefono;
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


	public String getFnacim() {
		return fnacim;
	}


	public void setFnacim(String fnacim) {
		this.fnacim = fnacim;
	}


	public String getCalificacion() {
		return calificacion;
	}


	public void setCalificacion(String calificacion) {
		this.calificacion = calificacion;
	}


	public String getEspecialidad() {
		return especialidad;
	}


	public void setEspecialidad(String especialidad) {
		this.especialidad = especialidad;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getTelefono() {
		return telefono;
	}


	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}


	public String getClave() {
		return clave;
	}


	public void setClave(String clave) {
		this.clave = clave;
	}

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
	
	
	
	
	
}
