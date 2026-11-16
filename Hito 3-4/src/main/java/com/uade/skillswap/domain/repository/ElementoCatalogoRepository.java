package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.CategoriaHabilidad;
import com.uade.skillswap.domain.model.ElementoCatalogoBase;

import java.util.List;
import java.util.Optional;

public interface ElementoCatalogoRepository extends Repositorio<ElementoCatalogoBase, String> {

    Optional<CategoriaHabilidad> buscarCategoriaPorNombre(String nombre);

    List<CategoriaHabilidad> listarCategorias();
}
