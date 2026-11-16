package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "destinatario_user_id", nullable = false)
    private String destinatarioUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCanalNotificacion canal;

    @Column(length = 1000)
    private String mensaje;

    private boolean leida;

    @Column(name = "evento_origen")
    private String eventoOrigen;

    /** Que paso (SOLICITUD_RECIBIDA, CAMBIO_ESTADO, REPROGRAMACION_...), para mostrar un titulo adecuado. */
    @Column(name = "tipo_evento", length = 40)
    private String tipoEvento;

    /** Id de la publicacion/contratacion a la que refiere (para poder navegar desde la bandeja). */
    @Column(name = "entidad_id", length = 36)
    private String entidadId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Notificacion() {
        // JPA
    }

    public Notificacion(String id, String destinatarioUserId, TipoCanalNotificacion canal,
                         String mensaje, String eventoOrigen, LocalDateTime fecha) {
        this.id = id;
        this.destinatarioUserId = destinatarioUserId;
        this.canal = canal;
        this.mensaje = mensaje;
        this.eventoOrigen = eventoOrigen;
        this.fecha = fecha;
        this.leida = false;
    }

    public Notificacion(String id, String destinatarioUserId, TipoCanalNotificacion canal, String mensaje,
                         String eventoOrigen, String tipoEvento, String entidadId, LocalDateTime fecha) {
        this(id, destinatarioUserId, canal, mensaje, eventoOrigen, fecha);
        this.tipoEvento = tipoEvento;
        this.entidadId = entidadId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getId() {
        return id;
    }

    public String getDestinatarioUserId() {
        return destinatarioUserId;
    }

    public TipoCanalNotificacion getCanal() {
        return canal;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public String getEventoOrigen() {
        return eventoOrigen;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
