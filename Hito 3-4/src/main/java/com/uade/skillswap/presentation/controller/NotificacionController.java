package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.dto.NotificacionDTO;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.NotificacionService;
import com.uade.skillswap.domain.repository.Pagina;

/** Seccion 9: NotificacionController.listar(usuario), mas marcar como leidas (seccion 10). */
public class NotificacionController extends ControllerBase {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.notificacionService = notificacionService;
    }

    public Resultado<Pagina<NotificacionDTO>> listar(String userId, int pagina, int tamanio) {
        return consultar(() -> Pagina.de(notificacionService.listarNotificaciones(userId), pagina, tamanio)
                .map(DtoMapper::notificacion));
    }

    public Resultado<Void> marcarLeida(String actorUserId, String notificacionId) {
        return modificarSinValor(actorUserId, () -> notificacionService.marcarLeida(actorUserId, notificacionId));
    }

    public Resultado<Void> marcarTodasLeidas(String actorUserId) {
        return modificarSinValor(actorUserId, () -> notificacionService.marcarTodasLeidas(actorUserId));
    }
}
