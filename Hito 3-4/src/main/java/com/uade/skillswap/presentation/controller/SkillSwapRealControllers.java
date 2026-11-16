package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;

/**
 * Implementacion real de UiControllers (reemplaza a MockSkillSwapControllers,
 * seccion 10: "Los equipos... deberan implementar los controllers reales y
 * conectarlos mediante interfaces Java"). Cada sub-controller delega en los
 * casos de uso de application.usecase.
 */
public class SkillSwapRealControllers implements UiControllers {

    private final BusquedaUiController busqueda;
    private final PublicacionUiController publicaciones;
    private final ContratacionUiController contrataciones;
    private final WalletUiController wallet;
    private final NotificacionUiController notificaciones;
    private final PerfilUiController perfiles;

    public SkillSwapRealControllers(BusquedaUiController busqueda, PublicacionUiController publicaciones,
                                     ContratacionUiController contrataciones, WalletUiController wallet,
                                     NotificacionUiController notificaciones, PerfilUiController perfiles) {
        this.busqueda = busqueda;
        this.publicaciones = publicaciones;
        this.contrataciones = contrataciones;
        this.wallet = wallet;
        this.notificaciones = notificaciones;
        this.perfiles = perfiles;
    }

    @Override
    public BusquedaController busqueda() {
        return busqueda;
    }

    @Override
    public PublicacionController publicaciones() {
        return publicaciones;
    }

    @Override
    public ContratacionController contrataciones() {
        return contrataciones;
    }

    @Override
    public WalletController wallet() {
        return wallet;
    }

    @Override
    public NotificacionController notificaciones() {
        return notificaciones;
    }

    @Override
    public PerfilController perfiles() {
        return perfiles;
    }
}
