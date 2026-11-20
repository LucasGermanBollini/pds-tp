package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.repository.ContratacionRepository;

public class InMemoryContratacionRepository extends AbstractInMemoryRepository<Contratacion, String> implements ContratacionRepository {

    public InMemoryContratacionRepository() {
        super(Contratacion::getId);
    }
}
