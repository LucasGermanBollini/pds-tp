package com.uade.skillswap.domain.model.observer;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento que viaja del Subject (Publicacion/Contratacion) a cada Observer.
 * Lleva todo lo que un observer necesita para reaccionar sin tener que
 * volver a consultar la base: el tipo de evento, la transicion, quien la
 * hizo, el motivo, una descripcion legible y a quienes les interesa
 * (destinatarios). Asi el notificador no depende de los repositorios de
 * Publicacion/Contratacion (bajo acoplamiento).
 */
public class CambioEstadoEvento {

    private final TipoEventoDominio tipo;
    private final String entidadTipo;
    private final String entidadId;
    private final String estadoAnterior;
    private final String estadoNuevo;
    private final String actorUserId;
    private final String motivo;
    private final String descripcion;
    private final List<String> destinatarios;
    private final LocalDateTime fecha;

    public CambioEstadoEvento(TipoEventoDominio tipo, String entidadTipo, String entidadId, String estadoAnterior,
                               String estadoNuevo, String actorUserId, String motivo, String descripcion,
                               List<String> destinatarios, LocalDateTime fecha) {
        this.tipo = tipo;
        this.entidadTipo = entidadTipo;
        this.entidadId = entidadId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.actorUserId = actorUserId;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.destinatarios = destinatarios == null ? List.of() : List.copyOf(destinatarios);
        this.fecha = fecha;
    }

    public TipoEventoDominio getTipo() {
        return tipo;
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

    public String getDescripcion() {
        return descripcion;
    }

    public List<String> getDestinatarios() {
        return destinatarios;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
