package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.MovimientoCreditos;
import com.uade.skillswap.domain.model.SkillCoins;
import com.uade.skillswap.domain.model.TipoMovimientoCreditos;
import com.uade.skillswap.domain.model.WalletSkillCoins;
import com.uade.skillswap.domain.repository.MovimientoCreditosRepository;
import com.uade.skillswap.domain.repository.WalletSkillCoinsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Casos de uso de la wallet: consultar saldo, movimientos y acreditar creditos de demo. */
public class WalletService {

    private final WalletSkillCoinsRepository walletSkillCoinsRepository;
    private final MovimientoCreditosRepository movimientoCreditosRepository;

    public WalletService(WalletSkillCoinsRepository walletSkillCoinsRepository,
                          MovimientoCreditosRepository movimientoCreditosRepository) {
        this.walletSkillCoinsRepository = walletSkillCoinsRepository;
        this.movimientoCreditosRepository = movimientoCreditosRepository;
    }

    public SkillCoins consultarSaldo(String titularUserId) {
        return obtenerWallet(titularUserId).getSaldo();
    }

    public List<MovimientoCreditos> listarMovimientos(String titularUserId) {
        return movimientoCreditosRepository.buscarPorWallet(obtenerWallet(titularUserId).getId()).stream()
                .sorted(java.util.Comparator.comparing(MovimientoCreditos::getFecha).reversed())
                .toList();
    }

    /** Seccion 10 (Mock UI Kit): "Wallet de SkillCoins... créditos promocionales" para poder probar la demo. */
    public SkillCoins acreditarDemo(String titularUserId, long importe) {
        if (importe <= 0) {
            throw new IllegalArgumentException("El importe a acreditar debe ser positivo");
        }
        WalletSkillCoins wallet = obtenerWallet(titularUserId);
        SkillCoins monto = SkillCoins.de(importe);
        wallet.acreditar(monto);
        walletSkillCoinsRepository.guardar(wallet);
        movimientoCreditosRepository.guardar(new MovimientoCreditos(UUID.randomUUID().toString(), wallet.getId(),
                TipoMovimientoCreditos.CREDITO, monto, "Creditos promocionales", null, LocalDateTime.now(), wallet.getSaldo()));
        return wallet.getSaldo();
    }

    private WalletSkillCoins obtenerWallet(String titularUserId) {
        return walletSkillCoinsRepository.buscarPorTitular(titularUserId)
                .orElseThrow(() -> new IllegalStateException("El usuario " + titularUserId + " no tiene wallet de SkillCoins"));
    }
}
