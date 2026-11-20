package com.uade.skillswap.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Value object: un rango horario semanal en el que el perfil esta
 * disponible (seccion 4.1: "Administrar ... disponibilidad"). Es
 * @Embeddable porque no tiene identidad propia: se guarda como coleccion de
 * valores del Perfil (tabla perfil_disponibilidad).
 */
@Embeddable
public class DisponibilidadSemanal {

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    @Column(name = "hora_desde", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_hasta", nullable = false)
    private LocalTime horaHasta;

    protected DisponibilidadSemanal() {
        // JPA
    }

    public DisponibilidadSemanal(DayOfWeek diaSemana, LocalTime horaDesde, LocalTime horaHasta) {
        if (diaSemana == null || horaDesde == null || horaHasta == null) {
            throw new IllegalArgumentException("Dia y horario de disponibilidad son obligatorios");
        }
        if (!horaHasta.isAfter(horaDesde)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio");
        }
        this.diaSemana = diaSemana;
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraDesde() {
        return horaDesde;
    }

    public LocalTime getHoraHasta() {
        return horaHasta;
    }

    public boolean seSuperponeCon(DisponibilidadSemanal otra) {
        return diaSemana == otra.diaSemana
                && horaDesde.isBefore(otra.horaHasta)
                && otra.horaDesde.isBefore(horaHasta);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisponibilidadSemanal d && d.diaSemana == diaSemana
                && d.horaDesde.equals(horaDesde) && d.horaHasta.equals(horaHasta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diaSemana, horaDesde, horaHasta);
    }

    @Override
    public String toString() {
        return diaSemana + " " + horaDesde + "-" + horaHasta;
    }
}
