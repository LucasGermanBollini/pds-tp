package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.MovimientoCreditos;
import com.uade.skillswap.domain.repository.MovimientoCreditosRepository;

import java.util.List;

public class InMemoryMovimientoCreditosRepository extends AbstractInMemoryRepository<MovimientoCreditos, String> implements MovimientoCreditosRepository {

    public InMemoryMovimientoCreditosRepository() {
        super(MovimientoCreditos::getId);
    }

    @Override
    public List<MovimientoCreditos> buscarPorWallet(String walletId) {
        return listarTodos().stream().filter(m -> m.getWalletId().equals(walletId)).toList();
    }
}
