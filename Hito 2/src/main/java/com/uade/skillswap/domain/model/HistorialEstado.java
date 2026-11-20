package com.uade.skillswap.domain.model;

import java.time.LocalDateTime;

/**
 * Registro inmutable de una transicion de estado (Publicacion o Contratacion).
 * Seccion 5: "Toda transicion de estado registra fecha, actor y motivo en un
 * historial inmutable". Se completa via Observer, no por llamada directa.
 */
public final class HistorialEstado {

    private final String id;
    private final String entidadTipo;
    private final String entidadId;
    private final String estadoAnterior;
    private final String estadoNuevo;
    private final String actorUserId;
    private final String motivo;
    private final LocalDateTime fecha;

    public HistorialEstado(String id, String entidadTipo, String entidadId, String estadoAnterior,
                            String estadoNuevo, String actorUserId, String motivo, LocalDateTime fecha) {
        this.id = id;
        this.entidadTipo = entidadTipo;
        this.entidadId = entidadId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.actorUserId = actorUserId;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public String getId() {
        return id;
    }

    public String getEntidadTipo() {
        return entidadTipo;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
