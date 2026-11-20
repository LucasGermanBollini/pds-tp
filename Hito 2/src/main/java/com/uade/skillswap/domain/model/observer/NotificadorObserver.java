package com.uade.skillswap.domain.model.observer;

import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import com.uade.skillswap.domain.repository.NotificacionRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.util.List;
import java.util.UUID;

/**
 * Observer Pattern - Observer concreto.
 * Resuelve a quien avisar segun la entidad que cambio de estado y deja
 * constancia en Notificacion (bandeja interna). Siguiendo el ejemplo real de
 * la catedra (EmailPedidoObserver/FirebasePushPedidoObserver implementan
 * PedidoObserver directamente, sin una capa de "canal" intermedia), el
 * Adapter hacia email/push real NO se agrega aca: en el Hito 3 se suman
 * EmailNotificadorObserver / PushNotificadorObserver como observers propios,
 * cada uno adaptando su SDK externo, elegidos por una Simple Factory segun
 * la preferencia del usuario. Este observer es el canal INTERNA.
 */
public class NotificadorObserver implements ObservadorDeEstado {

    private final PublicacionRepository publicacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final NotificacionRepository notificacionRepository;

    public NotificadorObserver(PublicacionRepository publicacionRepository,
                                ContratacionRepository contratacionRepository,
                                NotificacionRepository notificacionRepository) {
        this.publicacionRepository = publicacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public void actualizar(CambioEstadoEvento evento) {
        String mensaje = String.format("%s %s paso de %s a %s", evento.getEntidadTipo(), evento.getEntidadId(),
                evento.getEstadoAnterior(), evento.getEstadoNuevo());
        for (String destinatarioUserId : resolverDestinatarios(evento)) {
            notificacionRepository.guardar(new Notificacion(UUID.randomUUID().toString(), destinatarioUserId,
                    TipoCanalNotificacion.INTERNA, mensaje, evento.getEntidadTipo(), evento.getFecha()));
        }
    }

    private List<String> resolverDestinatarios(CambioEstadoEvento evento) {
        if ("PUBLICACION".equals(evento.getEntidadTipo())) {
            return publicacionRepository.buscarPorId(evento.getEntidadId())
                    .map(p -> List.of(p.getAutor().obtenerId()))
                    .orElse(List.of());
        }
        if ("CONTRATACION".equals(evento.getEntidadTipo())) {
            return contratacionRepository.buscarPorId(evento.getEntidadId())
                    .map(c -> List.of(c.getCliente().obtenerId(), c.getPublicacion().getAutor().obtenerId()))
                    .orElse(List.of());
        }
        return List.of();
    }
}
