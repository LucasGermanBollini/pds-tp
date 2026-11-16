package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Referencia contratacion/autor/receptor por id (estilo log, sin relacion JPA). */
@Entity
@Table(name = "calificaciones")
public class Calificacion {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "contratacion_id", nullable = false, length = 36)
    private String contratacionId;

    @Column(name = "autor_user_id", nullable = false)
    private String autorUserId;

    @Column(name = "receptor_user_id", nullable = false)
    private String receptorUserId;

    @Column(nullable = false)
    private int puntaje;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Calificacion() {
        // JPA
    }

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
