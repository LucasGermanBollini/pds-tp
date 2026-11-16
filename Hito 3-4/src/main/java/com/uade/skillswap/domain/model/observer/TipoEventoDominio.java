package com.uade.skillswap.domain.model.observer;

/**
 * Seccion 4.6: los eventos relevantes que deben notificarse. La mayoria son
 * cambios de estado; la solicitud recibida y la reprogramacion no cambian el
 * estado de la entidad, pero igual interesan a las partes.
 */
public enum TipoEventoDominio {
    SOLICITUD_RECIBIDA,
    CAMBIO_ESTADO,
    REPROGRAMACION_PROPUESTA,
    REPROGRAMACION_ACEPTADA,
    REPROGRAMACION_RECHAZADA;

    /** Los que forman parte del historial inmutable de estados (seccion 5). */
    public boolean registraHistorial() {
        return this == SOLICITUD_RECIBIDA || this == CAMBIO_ESTADO;
    }
}
