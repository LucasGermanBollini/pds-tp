package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.repository.PublicacionRepository;

public class InMemoryPublicacionRepository extends AbstractInMemoryRepository<Publicacion, String> implements PublicacionRepository {

    public InMemoryPublicacionRepository() {
        super(Publicacion::getId);
    }
}
