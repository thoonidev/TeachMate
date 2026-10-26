package com.tme.modal;

public class Tutoria {

	private int id;
	private int userId;
	private String nombre;
	private String genero;
	private String edad;
	private String ftutoria;
	private String email;
	private String telefono;
	private String experiencia;
	private int docenteId;
	private String direccion;
	private EstadoTutoria estado = EstadoTutoria.PENDIENTE_PAGO;
	private String comentario = "";
    private String nombreDocente;
	private java.time.LocalTime horaInicio;
	private Integer duracionMinutos;
	
	
	public Tutoria() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Tutoria(int id, int userId, String nombre, String genero, String edad, String ftutoria,
			String email, String telefono, String experiencia, int docenteId, String direccion, String estado) {
		super();
		this.id = id;
		this.userId = userId;
		this.nombre = nombre;
		this.genero = genero;
		this.edad = edad;
		this.ftutoria = ftutoria;
		this.email = email;
		this.telefono = telefono;
		this.experiencia = experiencia;
		this.docenteId = docenteId;
		this.direccion = direccion;
		setEstado(estado);
	}


	public Tutoria(int userId, String nombre, String genero, String edad, String ftutoria, String email,
			String telefono, String experiencia, int docenteId, String direccion, String estado) {
		super();
		this.userId = userId;
		this.nombre = nombre;
		this.genero = genero;
		this.edad = edad;
		this.ftutoria = ftutoria;
		this.email = email;
		this.telefono = telefono;
		this.experiencia = experiencia;
		this.docenteId = docenteId;
		this.direccion = direccion;
		setEstado(estado);
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getUserId() {
		return userId;
	}


	public void setUserId(int userId) {
		this.userId = userId;
	}


	public String getNombre() {
		return nombre;
	}


	public void setNombre(String nombre) {
		this.nombre = nombre;
	}


	public String getGenero() {
		return genero;
	}


	public void setGenero(String genero) {
		this.genero = genero;
	}


	public String getEdad() {
		return edad;
	}


	public void setEdad(String edad) {
		this.edad = edad;
	}


	public String getFtutoria() {
		return ftutoria;
	}


	public void setFtutoria(String ftutoria) {
		this.ftutoria = ftutoria;
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


	public String getExperiencia() {
		return experiencia;
	}


	public void setExperiencia(String experiencia) {
		this.experiencia = experiencia;
	}


	public int getDocenteId() {
		return docenteId;
	}


	public void setDocenteId(int docenteId) {
		this.docenteId = docenteId;
	}


	public String getDireccion() {
		return direccion;
	}


	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}


	public String getEstado() {
		return estado.getValor();
	}


	public void setEstado(String estado) {
		this.estado = EstadoTutoria.desdeValor(estado);
	}

    private String nivel="", enlaceClase="", asistencia="", motivoCancelacion="";
    private java.time.LocalDateTime creadaEn, venceRespuesta, vencePago;
    private Integer plazoPagoMinutos;
    private boolean cancelacionSolicitada;
    private boolean pagoRevision;
    public boolean isPagoRevision(){return pagoRevision;}
    public void setPagoRevision(boolean v){pagoRevision=v;}
    public String getNivel(){return nivel;}
    public void setNivel(String v){nivel=v==null?"":v;}
    public String getEnlaceClase(){return enlaceClase;}
    public void setEnlaceClase(String v){enlaceClase=v==null?"":v;}
    public String getAsistencia(){return asistencia;}
    public void setAsistencia(String v){asistencia=v==null?"":v;}
    public String getMotivoCancelacion(){return motivoCancelacion;}
    public void setMotivoCancelacion(String v){motivoCancelacion=v==null?"":v;}
    public boolean isCancelacionSolicitada(){return cancelacionSolicitada;}
    public void setCancelacionSolicitada(boolean v){cancelacionSolicitada=v;}
    public java.time.LocalDateTime getCreadaEn(){return creadaEn;}
    public void setCreadaEn(java.time.LocalDateTime v){creadaEn=v;}
    public java.time.LocalDateTime getVenceRespuesta(){return venceRespuesta;}
    public void setVenceRespuesta(java.time.LocalDateTime v){venceRespuesta=v;}
    public java.time.LocalDateTime getVencePago(){return vencePago;}
    public void setVencePago(java.time.LocalDateTime v){vencePago=v;}
    public Integer getPlazoPagoMinutos(){return plazoPagoMinutos;}
    public void setPlazoPagoMinutos(Integer v){plazoPagoMinutos=v;}
    private Integer cursoId;
    private Integer tarifaId;
    private String cursoNombre;
    private java.math.BigDecimal precio;
    private java.math.BigDecimal porcentajeDocente;
    public Integer getCursoId() { return cursoId; }
    public void setCursoId(Integer value) { cursoId = value; }
    public Integer getTarifaId() { return tarifaId; }
    public void setTarifaId(Integer value) { tarifaId = value; }
    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String value) { cursoNombre = value; }
    public java.math.BigDecimal getPrecio() { return precio; }
    public void setPrecio(java.math.BigDecimal value) { precio = value; }
    public java.math.BigDecimal getPorcentajeDocente() { return porcentajeDocente; }
    public void setPorcentajeDocente(java.math.BigDecimal value) { porcentajeDocente = value; }
    public String getComentario() { return comentario; }
    public String getNombreDocente() { return nombreDocente; }
    public void setNombreDocente(String nombreDocente) { this.nombreDocente = nombreDocente; }
    public void setComentario(String comentario) { this.comentario = comentario == null ? "" : comentario; }
    public java.time.LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(java.time.LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getHorario() {
        return horaInicio == null || duracionMinutos == null ? "Por definir"
                : horaInicio.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " · " + duracionMinutos + " min";
    }
	
	
	
	
	
	
}
