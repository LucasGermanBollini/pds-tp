package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.HistorialEstado;
import com.uade.skillswap.domain.repository.HistorialEstadoRepository;

import java.util.List;

public class InMemoryHistorialEstadoRepository extends AbstractInMemoryRepository<HistorialEstado, String> implements HistorialEstadoRepository {

    public InMemoryHistorialEstadoRepository() {
        super(HistorialEstado::getId);
    }

    @Override
    public List<HistorialEstado> buscarPorEntidad(String entidadTipo, String entidadId) {
        return listarTodos().stream()
                .filter(h -> h.getEntidadTipo().equals(entidadTipo) && h.getEntidadId().equals(entidadId))
                .toList();
    }
}
