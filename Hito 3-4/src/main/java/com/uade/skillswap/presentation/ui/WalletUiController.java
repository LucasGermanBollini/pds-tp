package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.WalletController;

import java.util.List;

/** Adapter (contrato del Mock UI Kit -> WalletController). */
public class WalletUiController implements UiControllers.WalletController {

    private final WalletController walletController;

    public WalletUiController(WalletController walletController) {
        this.walletController = walletController;
    }

    @Override
    public int consultarSaldo(String userId) {
        return walletController.consultarSaldo(userId).valorOLanzar().intValue();
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        return walletController.listarMovimientos(userId, 0, Pagina.TAMANIO_MAXIMO).valorOLanzar().contenido().stream()
                .map(UiMappers::aMovimientoUi).toList();
    }

    @Override
    public int acreditarDemo(String userId, int importe) {
        return walletController.acreditarDemo(userId, importe).valorOLanzar().intValue();
    }
}
