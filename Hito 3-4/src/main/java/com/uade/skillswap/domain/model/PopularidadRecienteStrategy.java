package com.uade.skillswap.domain.model;

import java.util.Comparator;
import java.util.List;

/** Strategy Pattern - Concrete Strategy. Ordena por cantidad de solicitudes recientes. */
public class PopularidadRecienteStrategy implements RecomendacionStrategy {

    @Override
    public String getNombre() {
        return "POPULARIDAD_RECIENTE";
    }

    @Override
    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas) {
        return candidatas.stream()
                .sorted(Comparator.comparingInt(Publicacion::getContadorSolicitudes).reversed())
                .toList();
    }
}
