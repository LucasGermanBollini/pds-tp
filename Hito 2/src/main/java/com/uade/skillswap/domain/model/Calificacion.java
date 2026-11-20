package com.uade.skillswap.domain.model;

import java.time.LocalDateTime;

public final class Calificacion {

    private final String id;
    private final String contratacionId;
    private final String autorUserId;
    private final String receptorUserId;
    private final int puntaje;
    private final String comentario;
    private final LocalDateTime fecha;

    public Calificacion(String id, String contratacionId, String autorUserId, String receptorUserId,
                         int puntaje, String comentario, LocalDateTime fecha) {
        if (autorUserId.equals(receptorUserId)) {
            throw new IllegalArgumentException("Una persona no puede calificarse a si misma (seccion 4.4)");
        }
        if (puntaje < 1 || puntaje > 5) {
            throw new IllegalArgumentException("El puntaje debe estar entre 1 y 5");
        }
        this.id = id;
        this.contratacionId = contratacionId;
        this.autorUserId = autorUserId;
        this.receptorUserId = receptorUserId;
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    public String getId() {
        return id;
    }

    public String getContratacionId() {
        return contratacionId;
    }

    public String getAutorUserId() {
        return autorUserId;
    }

    public String getReceptorUserId() {
        return receptorUserId;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
