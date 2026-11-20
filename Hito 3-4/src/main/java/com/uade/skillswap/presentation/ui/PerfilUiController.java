package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.presentation.controller.PerfilController;

/** Adapter (contrato del Mock UI Kit -> PerfilController, que usa la Facade del perfil publico). */
public class PerfilUiController implements UiControllers.PerfilController {

    private final PerfilController perfilController;

    public PerfilUiController(PerfilController perfilController) {
        this.perfilController = perfilController;
    }

    @Override
    public Models.Perfil verPerfil(String userId) {
        return UiMappers.aPerfilUi(perfilController.verPerfil(userId).valorOLanzar());
    }
}
