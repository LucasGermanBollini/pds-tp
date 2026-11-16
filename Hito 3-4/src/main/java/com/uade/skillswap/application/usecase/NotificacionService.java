package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.repository.NotificacionRepository;

import java.util.List;

/** Casos de uso de la bandeja de notificaciones. */
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public List<Notificacion> listarNotificaciones(String destinatarioUserId) {
        return notificacionRepository.buscarPorDestinatario(destinatarioUserId);
    }

    public void marcarLeida(String destinatarioUserId, String notificacionId) {
        notificacionRepository.buscarPorId(notificacionId)
                .filter(n -> n.getDestinatarioUserId().equals(destinatarioUserId))
                .ifPresent(n -> {
                    n.marcarLeida();
                    notificacionRepository.guardar(n);
                });
    }

    public void marcarTodasLeidas(String destinatarioUserId) {
        listarNotificaciones(destinatarioUserId).forEach(n -> {
            n.marcarLeida();
            notificacionRepository.guardar(n);
        });
    }
}
