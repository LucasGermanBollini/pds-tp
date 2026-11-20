package com.uade.skillswap.domain.model;

import java.time.LocalDateTime;

public class FranjaDisponibilidad {

    private final String id;
    private final LocalDateTime fechaHoraInicio;
    private final LocalDateTime fechaHoraFin;
    private final int cupoTotal;
    private int cupoDisponible;

    public FranjaDisponibilidad(String id, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, int cupoTotal) {
        if (fechaHoraInicio == null || fechaHoraFin == null || !fechaHoraFin.isAfter(fechaHoraInicio)) {
            throw new IllegalArgumentException("La franja debe tener fin posterior al inicio");
        }
        if (cupoTotal <= 0) {
            throw new IllegalArgumentException("El cupo debe ser mayor a cero");
        }
        this.id = id;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.cupoTotal = cupoTotal;
        this.cupoDisponible = cupoTotal;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public int getCupoDisponible() {
        return cupoDisponible;
    }

    /**
     * Reserva un lugar de forma atomica dentro del proceso (evita sobreventa,
     * seccion 15: "Evitar que dos aceptaciones consuman el ultimo cupo").
     * La version definitiva con optimistic locking llega en el Hito 3 (JPA).
     */
    public synchronized boolean reservarCupo() {
        if (cupoDisponible <= 0) {
            return false;
        }
        cupoDisponible--;
        return true;
    }

    public synchronized void liberarCupo() {
        if (cupoDisponible < cupoTotal) {
            cupoDisponible++;
        }
    }
}
