package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.usecase.NotificacionService;
import com.uade.skillswap.domain.model.Notificacion;

import java.util.Comparator;
import java.util.List;

public class NotificacionUiController implements UiControllers.NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionUiController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        return notificacionService.listarNotificaciones(userId).stream()
                .sorted(Comparator.comparing(Notificacion::getFecha).reversed())
                .map(UiMappers::aNotificacionUi)
                .toList();
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        notificacionService.marcarLeida(userId, notificacionId);
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        notificacionService.marcarTodasLeidas(userId);
    }
}
