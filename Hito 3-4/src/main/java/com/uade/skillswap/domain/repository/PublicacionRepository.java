package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.Publicacion;

import java.util.List;

public interface PublicacionRepository extends Repositorio<Publicacion, String> {

    List<Publicacion> buscarPorAutor(String autorUserId);
}
