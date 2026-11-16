package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.repository.PerfilRepository;

public class InMemoryPerfilRepository extends AbstractInMemoryRepository<Perfil, String> implements PerfilRepository {

    public InMemoryPerfilRepository() {
        super(Perfil::obtenerId);
    }
}
