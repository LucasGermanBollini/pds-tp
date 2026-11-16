package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.usecase.PerfilService;

public class PerfilUiController implements UiControllers.PerfilController {

    private final PerfilService perfilService;

    public PerfilUiController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @Override
    public Models.Perfil verPerfil(String userId) {
        return UiMappers.aPerfilUi(perfilService.verPerfil(userId));
    }
}
