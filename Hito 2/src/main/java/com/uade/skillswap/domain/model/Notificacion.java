package com.uade.skillswap.domain.model;

import java.time.LocalDateTime;

public class Notificacion {

    private final String id;
    private final String destinatarioUserId;
    private final TipoCanalNotificacion canal;
    private final String mensaje;
    private boolean leida;
    private final String eventoOrigen;
    private final LocalDateTime fecha;

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
