package com.uade.skillswap.domain.model;

import java.util.List;

/** Strategy Pattern - Strategy. */
public interface RecomendacionStrategy {

    String getNombre();

    List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas);
}
