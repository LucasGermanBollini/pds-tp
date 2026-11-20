package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Calificacion;
import com.uade.skillswap.domain.repository.CalificacionRepository;

import java.util.List;

public class InMemoryCalificacionRepository extends AbstractInMemoryRepository<Calificacion, String> implements CalificacionRepository {

    public InMemoryCalificacionRepository() {
        super(Calificacion::getId);
    }

    @Override
    public List<Calificacion> buscarPorReceptor(String receptorUserId) {
        return listarTodos().stream().filter(c -> c.getReceptorUserId().equals(receptorUserId)).toList();
    }

    @Override
    public List<Calificacion> buscarPorContratacion(String contratacionId) {
        return listarTodos().stream().filter(c -> c.getContratacionId().equals(contratacionId)).toList();
    }
}
