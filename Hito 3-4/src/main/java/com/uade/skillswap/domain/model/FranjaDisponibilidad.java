package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "franjas_disponibilidad")
public class FranjaDisponibilidad {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicacion_id")
    private Publicacion publicacion;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(name = "cupo_total", nullable = false)
    private int cupoTotal;

    @Column(name = "cupo_disponible", nullable = false)
    private int cupoDisponible;

    /**
     * Optimistic locking (seccion 8): evita que dos aceptaciones concurrentes
     * consuman el mismo ultimo cupo. Hibernate incrementa esta columna en
     * cada UPDATE y rechaza el segundo commit si la version ya cambio.
     */
    @Version
    private Long version;

    protected FranjaDisponibilidad() {
        // JPA
    }

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

    /** Lado inverso de la relacion; lo llama Publicacion.agregarFranja(). */
    void asignarPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
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

    public int getCupoTotal() {
        return cupoTotal;
    }

    public boolean tieneCupo() {
        return cupoDisponible > 0;
    }

    /** Seccion 5: "No puede reservarse una franja ocupada". Dos franjas chocan si sus intervalos se pisan. */
    public boolean seSuperponeCon(FranjaDisponibilidad otra) {
        return otra != null && fechaHoraInicio.isBefore(otra.fechaHoraFin) && otra.fechaHoraInicio.isBefore(fechaHoraFin);
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    /**
     * Reserva un lugar. El `synchronized` protege contra una carrera dentro
     * del mismo proceso; la columna `@Version` protege contra dos procesos
     * (o dos transacciones) concurrentes pisandose vía optimistic locking
     * cuando el repositorio JPA hace flush/commit (seccion 15).
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

    /**
     * Por id: sin esto, publicacion.getFranjas().contains(franja) compara por
     * referencia y falla cuando la franja viene de una carga JPA distinta a
     * la que se guardo (dos instancias Java para la misma fila).
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof FranjaDisponibilidad f && id != null && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
