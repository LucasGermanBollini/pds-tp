package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.repository.NotificacionRepository;

import java.util.List;

public class InMemoryNotificacionRepository extends AbstractInMemoryRepository<Notificacion, String> implements NotificacionRepository {

    public InMemoryNotificacionRepository() {
        super(Notificacion::getId);
    }

    @Override
    public List<Notificacion> buscarPorDestinatario(String destinatarioUserId) {
        return listarTodos().stream().filter(n -> n.getDestinatarioUserId().equals(destinatarioUserId)).toList();
    }
}
