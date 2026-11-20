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
        return notificacionRepository.buscarPorDestinatario(destinatarioUserId).stream()
                .sorted(java.util.Comparator.comparing(Notificacion::getFecha).reversed())
                .toList();
    }

    public void marcarLeida(String destinatarioUserId, String notificacionId) {
        Notificacion notificacion = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion inexistente: " + notificacionId));
        if (!notificacion.getDestinatarioUserId().equals(destinatarioUserId)) {
            throw new com.uade.skillswap.domain.model.AccesoDenegadoException("La notificacion no es tuya");
        }
        notificacion.marcarLeida();
        notificacionRepository.guardar(notificacion);
    }

    public void marcarTodasLeidas(String destinatarioUserId) {
        listarNotificaciones(destinatarioUserId).stream().filter(n -> !n.isLeida()).forEach(n -> {
            n.marcarLeida();
            notificacionRepository.guardar(n);
        });
    }
}
