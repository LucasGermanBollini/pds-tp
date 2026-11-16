package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.WalletService;
import com.uade.skillswap.domain.model.MovimientoCreditos;

import java.util.Comparator;
import java.util.List;

public class WalletUiController implements UiControllers.WalletController {

    private final WalletService walletService;
    private final IdentidadProviderPort identidadProviderPort;

    public WalletUiController(WalletService walletService, IdentidadProviderPort identidadProviderPort) {
        this.walletService = walletService;
        this.identidadProviderPort = identidadProviderPort;
    }

    @Override
    public int consultarSaldo(String userId) {
        return (int) walletService.consultarSaldo(userId).valor();
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return walletService.listarMovimientos(userId).stream()
                .sorted(Comparator.comparing(MovimientoCreditos::getFecha).reversed())
                .map(UiMappers::aMovimientoUi)
                .toList();
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        identidadProviderPort.validarUsuarioActivo(userId);
        return (int) walletService.acreditarDemo(userId, importe).valor();
    }
}
