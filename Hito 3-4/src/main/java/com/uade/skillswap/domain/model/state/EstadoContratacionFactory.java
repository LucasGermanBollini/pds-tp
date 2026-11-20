package com.uade.skillswap.domain.model.state;

/** Simple Factory Pattern. Reconstruye el EstadoContratacion desde su nombre persistido. */
public final class EstadoContratacionFactory {

    private EstadoContratacionFactory() {
    }

    public static EstadoContratacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoContratacionPendiente();
        }
        return switch (nombre) {
            case "PENDIENTE" -> new EstadoContratacionPendiente();
            case "ACEPTADA" -> new EstadoContratacionAceptada();
            case "CONFIRMADA" -> new EstadoContratacionConfirmada();
            case "EN_CURSO" -> new EstadoContratacionEnCurso();
            case "COMPLETADA" -> new EstadoContratacionCompletada();
            case "RECHAZADA" -> new EstadoContratacionRechazada();
            case "CANCELADA" -> new EstadoContratacionCancelada();
            default -> throw new IllegalArgumentException("Estado de contratacion desconocido: " + nombre);
        };
    }
}
