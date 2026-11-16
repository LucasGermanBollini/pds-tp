package com.uade.skillswap.domain.model.state;

/**
 * Simple Factory Pattern. Reconstruye el objeto State en memoria a partir
 * del nombre persistido en la columna `estado` (ver Publicacion.estado y
 * Clase 11: "Guarda PAGADO; al cargar, Java reconstruye EstadoPagado").
 */
public final class EstadoPublicacionFactory {

    private EstadoPublicacionFactory() {
    }

    public static EstadoPublicacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoPublicacionBorrador();
        }
        return switch (nombre) {
            case "BORRADOR" -> new EstadoPublicacionBorrador();
            case "PUBLICADA" -> new EstadoPublicacionPublicada();
            case "PAUSADA" -> new EstadoPublicacionPausada();
            case "CERRADA" -> new EstadoPublicacionCerrada();
            case "CANCELADA" -> new EstadoPublicacionCancelada();
            default -> throw new IllegalArgumentException("Estado de publicacion desconocido: " + nombre);
        };
    }
}
