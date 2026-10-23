package com.tme.modal;

/** States stored in MySQL; terminal states cannot be reopened. */
public enum EstadoTutoria {
    PENDIENTE("Pendiente"), ACEPTADA("Aceptada"), RECHAZADA("Rechazada"),
    CANCELADA("Cancelada"), COMPLETADA("Completada");

    private final String valor;
    EstadoTutoria(String valor) { this.valor = valor; }
    public String getValor() { return valor; }
    public String getAccion() {
        return switch (this) {
            case ACEPTADA -> "Aceptar"; case RECHAZADA -> "Rechazar";
            case CANCELADA -> "Cancelar"; case COMPLETADA -> "Completar";
            default -> "Actualizar";
        };
    }

    public boolean puedeCambiarA(EstadoTutoria siguiente) {
        if (siguiente == null) return false;
        return switch (this) {
            case PENDIENTE -> siguiente == ACEPTADA || siguiente == RECHAZADA || siguiente == CANCELADA;
            case ACEPTADA -> siguiente == COMPLETADA || siguiente == CANCELADA;
            default -> false;
        };
    }

    public static EstadoTutoria desdeValor(String valor) {
        for (EstadoTutoria estado : values()) if (estado.valor.equals(valor)) return estado;
        throw new IllegalArgumentException("Estado de tutoría inválido.");
    }
}
