package com.uade.skillswap.domain.model.observer;

import java.time.LocalDateTime;

/** DTO que viaja del Subject (Publicacion/Contratacion) a cada Observer. */
public class CambioEstadoEvento {

    private final String entidadTipo;
    private final String entidadId;
    private final String estadoAnterior;
    private final String estadoNuevo;
    private final String actorUserId;
    private final String motivo;
    private final LocalDateTime fecha;

    public CambioEstadoEvento(String entidadTipo, String entidadId, String estadoAnterior,
                               String estadoNuevo, String actorUserId, String motivo, LocalDateTime fecha) {
        this.entidadTipo = entidadTipo;
        this.entidadId = entidadId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.actorUserId = actorUserId;
        this.motivo = motivo;
        this.fecha = fecha;
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
