package com.tme.modal;

/** Aceptada is retained only for sessions created before payment confirmation existed. */
public enum EstadoTutoria {
    PENDIENTE("Pendiente"), PENDIENTE_PAGO("Pendiente de pago"), CONFIRMADA("Confirmada"),
    ACEPTADA("Aceptada"), RECHAZADA("Rechazada"), CANCELADA("Cancelada"), COMPLETADA("Completada"),
    VENCIDA("Vencida"), INCIDENCIA("Incidencia");
    private final String valor;
    EstadoTutoria(String valor) { this.valor=valor; }
    public String getValor() { return valor; }
    public String getEtiqueta() { return this==ACEPTADA?"Aceptada (anterior)":valor; }
    public String getClase() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static EstadoTutoria desdeValor(String valor) {
        for(EstadoTutoria estado:values())if(estado.valor.equals(valor))return estado;
        throw new IllegalArgumentException("Estado de asesoría inválido.");
    }
}
