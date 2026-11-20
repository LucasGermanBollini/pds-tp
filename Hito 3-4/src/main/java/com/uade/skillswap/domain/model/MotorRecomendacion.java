package com.uade.skillswap.domain.model;

import java.util.List;

/** Strategy Pattern - Context. No sabe que estrategias concretas existen. */
public class MotorRecomendacion {

    private RecomendacionStrategy estrategiaActual;

    public MotorRecomendacion(RecomendacionStrategy estrategiaInicial) {
        this.estrategiaActual = estrategiaInicial;
    }

    public void cambiarEstrategia(RecomendacionStrategy nuevaEstrategia) {
        if (nuevaEstrategia == null) {
            throw new IllegalArgumentException("La estrategia es obligatoria");
        }
        this.estrategiaActual = nuevaEstrategia;
    }

    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas) {
        return estrategiaActual.recomendar(perfil, candidatas);
    }

    public String getNombreEstrategiaActual() {
        return estrategiaActual.getNombre();
    }
}
