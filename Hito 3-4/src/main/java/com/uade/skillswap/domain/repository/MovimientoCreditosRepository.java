package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.MovimientoCreditos;

import java.util.List;

public interface MovimientoCreditosRepository extends Repositorio<MovimientoCreditos, String> {

    List<MovimientoCreditos> buscarPorWallet(String walletId);
}
