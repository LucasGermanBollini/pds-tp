package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.dto.MovimientoDTO;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.WalletService;
import com.uade.skillswap.domain.repository.Pagina;

/** Seccion 10: "Wallet de SkillCoins: consultar saldo y movimientos mediante controller". */
public class WalletController extends ControllerBase {

    private final WalletService walletService;

    public WalletController(WalletService walletService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.walletService = walletService;
    }

    public Resultado<Long> consultarSaldo(String userId) {
        return consultar(() -> walletService.consultarSaldo(userId).valor());
    }

    public Resultado<Pagina<MovimientoDTO>> listarMovimientos(String userId, int pagina, int tamanio) {
        return consultar(() -> Pagina.de(walletService.listarMovimientos(userId), pagina, tamanio)
                .map(DtoMapper::movimiento));
    }

    /** Creditos promocionales para poder probar la demo (boton "+ Acreditar demo" del Mock UI Kit). */
    public Resultado<Long> acreditarDemo(String actorUserId, long importe) {
        return modificar(actorUserId, () -> walletService.acreditarDemo(actorUserId, importe).valor());
    }
}
