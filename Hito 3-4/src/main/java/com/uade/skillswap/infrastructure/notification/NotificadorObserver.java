package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionFactory;
import com.uade.skillswap.application.port.CanalNotificacionPort;
import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.repository.NotificacionRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;

import java.util.UUID;

/**
 * Observer Pattern - Observer concreto, en Infraestructura (igual que
 * EmailPedidoObserver/FirebasePushPedidoObserver de la catedra, que
 * implementan PedidoObserver directamente desde infrastructure.notification,
 * no desde el dominio).
 *
 * <p>Seccion 4.6: por cada destinatario del evento consulta su canal
 * preferido (email, push o interna), le pide a la Simple Factory el Adapter
 * de ese canal y envia; ademas siempre deja constancia en la bandeja interna
 * (Notificacion), que es lo que muestra la pantalla de notificaciones.
 * Asi un mismo evento puede salir por dos canales distintos (uno por cada
 * parte), que es el caso de aceptacion de la seccion 15.
 */
public class NotificadorObserver implements ObservadorDeEstado {

    private final NotificacionRepository notificacionRepository;
    private final PerfilRepository perfilRepository;
    private final CanalNotificacionFactory canalNotificacionFactory;

    public NotificadorObserver(NotificacionRepository notificacionRepository,
                                PerfilRepository perfilRepository,
                                CanalNotificacionFactory canalNotificacionFactory) {
        this.notificacionRepository = notificacionRepository;
        this.perfilRepository = perfilRepository;
        this.canalNotificacionFactory = canalNotificacionFactory;
    }

    @Override
    public void actualizar(CambioEstadoEvento evento) {
        String mensaje = evento.getDescripcion() != null ? evento.getDescripcion()
                : String.format("%s %s paso de %s a %s", evento.getEntidadTipo(), evento.getEntidadId(),
                evento.getEstadoAnterior(), evento.getEstadoNuevo());
        for (String destinatarioUserId : evento.getDestinatarios()) {
            TipoCanalNotificacion canalPreferido = perfilRepository.buscarPorId(destinatarioUserId)
                    .map(Perfil::getCanalPreferido)
                    .orElse(TipoCanalNotificacion.INTERNA);
            CanalNotificacionPort canal = canalNotificacionFactory.crear(canalPreferido);
            canal.enviar(destinatarioUserId, mensaje);
            notificacionRepository.guardar(new Notificacion(UUID.randomUUID().toString(), destinatarioUserId,
                    canalPreferido, mensaje, evento.getEntidadTipo(), evento.getTipo().name(), evento.getEntidadId(),
                    evento.getFecha()));
        }
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
