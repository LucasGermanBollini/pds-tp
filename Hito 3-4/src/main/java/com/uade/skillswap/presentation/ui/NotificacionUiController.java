package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.NotificacionController;

import java.util.List;

/** Adapter (contrato del Mock UI Kit -> NotificacionController). */
public class NotificacionUiController implements UiControllers.NotificacionController {

    private final NotificacionController notificacionController;

    public NotificacionUiController(NotificacionController notificacionController) {
        this.notificacionController = notificacionController;
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return notificacionController.listar(userId, 0, Pagina.TAMANIO_MAXIMO).valorOLanzar().contenido().stream()
                .map(UiMappers::aNotificacionUi).toList();
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        notificacionController.marcarLeida(userId, notificacionId).valorOLanzar();
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        notificacionController.marcarTodasLeidas(userId).valorOLanzar();
    }
}
