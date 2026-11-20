package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.CategoriaHabilidad;
import com.uade.skillswap.domain.model.Habilidad;
import com.uade.skillswap.domain.model.ElementoCatalogoBase;
import com.uade.skillswap.domain.repository.ElementoCatalogoRepository;

import java.util.List;
import java.util.Optional;

public class InMemoryElementoCatalogoRepository extends AbstractInMemoryRepository<ElementoCatalogoBase, String> implements ElementoCatalogoRepository {

    public InMemoryElementoCatalogoRepository() {
        super(ElementoCatalogoBase::getId);
    }

    @Override
    public Optional<CategoriaHabilidad> buscarCategoriaPorNombre(String nombre) {
        return listarTodos().stream()
                .filter(e -> e instanceof CategoriaHabilidad && e.obtenerNombre().equalsIgnoreCase(nombre))
                .map(e -> (CategoriaHabilidad) e)
                .findFirst();
    }

    @Override
    public List<CategoriaHabilidad> listarCategorias() {
        return listarTodos().stream()
                .filter(e -> e instanceof CategoriaHabilidad)
                .map(e -> (CategoriaHabilidad) e)
                .toList();
    }

    @Override
    public Optional<Habilidad> buscarHabilidadPorNombre(String nombre) {
        return listarTodos().stream()
                .filter(e -> e instanceof Habilidad && e.obtenerNombre().equalsIgnoreCase(nombre))
                .map(e -> (Habilidad) e)
                .findFirst();
    }
}
