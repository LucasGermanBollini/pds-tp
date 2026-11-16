package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro inmutable de una transicion de estado (Publicacion o Contratacion).
 * Seccion 5: "Toda transicion de estado registra fecha, actor y motivo en un
 * historial inmutable". Se completa via Observer, no por llamada directa.
 */
@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "entidad_tipo", nullable = false)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false, length = 36)
    private String entidadId;

    @Column(name = "estado_anterior")
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false)
    private String estadoNuevo;

    @Column(name = "actor_user_id")
    private String actorUserId;

    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected HistorialEstado() {
        // JPA
    }

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
