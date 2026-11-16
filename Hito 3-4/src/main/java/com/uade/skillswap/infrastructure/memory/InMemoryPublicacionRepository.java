package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.util.List;

public class InMemoryPublicacionRepository extends AbstractInMemoryRepository<Publicacion, String> implements PublicacionRepository {

    public InMemoryPublicacionRepository() {
        super(Publicacion::getId);
    }

    @Override
    public List<Publicacion> buscarPorAutor(String autorUserId) {
        return listarTodos().stream().filter(p -> p.getAutor().obtenerId().equals(autorUserId)).toList();
    }
}
