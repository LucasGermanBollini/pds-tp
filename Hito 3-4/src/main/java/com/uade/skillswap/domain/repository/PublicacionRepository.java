package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.Publicacion;

import java.util.List;

public interface PublicacionRepository extends Repositorio<Publicacion, String> {

    List<Publicacion> buscarPorAutor(String autorUserId);

    /** Seccion 4.3: buscar, filtrar, ordenar y paginar publicaciones PUBLICADAS. */
    Pagina<Publicacion> buscar(CriteriosBusqueda criterios);

    /** Todas las PUBLICADAS (candidatas para las estrategias de recomendacion). */
    List<Publicacion> buscarPublicadas();
}
