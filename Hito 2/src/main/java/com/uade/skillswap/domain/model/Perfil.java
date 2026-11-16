package com.uade.skillswap.domain.model;

import java.util.ArrayList;
import java.util.List;

public class Perfil implements Publicador {

    private final String id;
    private String nombreVisible;
    private String biografia;
    private String zonaHoraria;
    private final List<Habilidad> habilidades = new ArrayList<>();
    private double reputacionPromedio;
    private int cantidadCalificaciones;
    private boolean activo = true;

    public Perfil(String id, String nombreVisible) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El userId del perfil es obligatorio");
        }
        if (nombreVisible == null || nombreVisible.isBlank()) {
            throw new IllegalArgumentException("El nombre visible es obligatorio");
        }
        this.id = id;
        this.nombreVisible = nombreVisible;
    }

    @Override
    public String obtenerId() {
        return id;
    }

    @Override
    public String obtenerNombreVisible() {
        return nombreVisible;
    }

    public void actualizarDatos(String nombreVisible, String biografia, String zonaHoraria) {
        if (nombreVisible != null && !nombreVisible.isBlank()) {
            this.nombreVisible = nombreVisible;
        }
        this.biografia = biografia;
        this.zonaHoraria = zonaHoraria;
    }

    public void agregarHabilidad(Habilidad habilidad) {
        if (habilidad != null && !habilidades.contains(habilidad)) {
            habilidades.add(habilidad);
        }
    }

    public List<Habilidad> getHabilidades() {
        return List.copyOf(habilidades);
    }

    public void darBajaLogica() {
        this.activo = false;
    }

    public boolean isActivo() {
        return activo;
    }

    /**
     * La reputacion se recalcula a partir de calificaciones verificadas; nunca
     * se expone un setter directo (regla de negocio, seccion 4.1).
     */
    public void recalcularReputacion(List<Calificacion> calificacionesRecibidas) {
        if (calificacionesRecibidas == null || calificacionesRecibidas.isEmpty()) {
            this.reputacionPromedio = 0;
            this.cantidadCalificaciones = 0;
            return;
        }
        double suma = calificacionesRecibidas.stream().mapToInt(Calificacion::getPuntaje).sum();
        this.reputacionPromedio = suma / calificacionesRecibidas.size();
        this.cantidadCalificaciones = calificacionesRecibidas.size();
    }

    public double getReputacionPromedio() {
        return reputacionPromedio;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public String getBiografia() {
        return biografia;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }
}
