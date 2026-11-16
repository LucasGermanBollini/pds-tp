package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionFactory;
import com.uade.skillswap.application.port.CanalNotificacionPort;
import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import com.uade.skillswap.domain.repository.NotificacionRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.util.List;
import java.util.UUID;

/**
 * Observer Pattern - Observer concreto, en Infraestructura (igual que
 * EmailPedidoObserver/FirebasePushPedidoObserver de la catedra, que
 * implementan PedidoObserver directamente desde infrastructure.notification,
 * no desde el dominio). Resuelve a quien avisar, consulta el canal preferido
 * de cada destinatario (seccion 4.6) y usa la Simple Factory para conseguir
 * el Adapter correspondiente; siempre deja ademas constancia en la bandeja
 * interna (Notificacion), sea cual sea el canal elegido para el envio.
 */
public class NotificadorObserver implements ObservadorDeEstado {

    private final PublicacionRepository publicacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final PerfilRepository perfilRepository;
    private final CanalNotificacionFactory canalNotificacionFactory;

    public NotificadorObserver(PublicacionRepository publicacionRepository,
                                ContratacionRepository contratacionRepository,
                                NotificacionRepository notificacionRepository,
                                PerfilRepository perfilRepository,
                                CanalNotificacionFactory canalNotificacionFactory) {
        this.publicacionRepository = publicacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.perfilRepository = perfilRepository;
        this.canalNotificacionFactory = canalNotificacionFactory;
    }

    @Override
    public void actualizar(CambioEstadoEvento evento) {
        String mensaje = String.format("%s %s paso de %s a %s", evento.getEntidadTipo(), evento.getEntidadId(),
                evento.getEstadoAnterior(), evento.getEstadoNuevo());
        for (String destinatarioUserId : resolverDestinatarios(evento)) {
            TipoCanalNotificacion canalPreferido = perfilRepository.buscarPorId(destinatarioUserId)
                    .map(Perfil::getCanalPreferido)
                    .orElse(TipoCanalNotificacion.INTERNA);

            CanalNotificacionPort canal = canalNotificacionFactory.crear(canalPreferido);
            canal.enviar(destinatarioUserId, mensaje);

            notificacionRepository.guardar(new Notificacion(UUID.randomUUID().toString(), destinatarioUserId,
                    canalPreferido, mensaje, evento.getEntidadTipo(), evento.getFecha()));
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

    /** Por clase, no por instancia: ver el comentario en RegistradorHistorialObserver.equals(). */
    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
