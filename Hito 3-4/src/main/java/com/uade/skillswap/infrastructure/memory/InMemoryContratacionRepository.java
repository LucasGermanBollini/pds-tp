package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.repository.ContratacionRepository;

import java.util.List;

public class InMemoryContratacionRepository extends AbstractInMemoryRepository<Contratacion, String> implements ContratacionRepository {

    public InMemoryContratacionRepository() {
        super(Contratacion::getId);
    }

    @Override
    public List<Contratacion> buscarPorParticipante(String userId) {
        return listarTodos().stream()
                .filter(c -> c.getCliente().obtenerId().equals(userId)
                        || c.getPublicacion().getAutor().obtenerId().equals(userId))
                .toList();
    }
}
