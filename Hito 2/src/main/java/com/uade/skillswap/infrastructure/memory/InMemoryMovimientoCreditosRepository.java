package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.MovimientoCreditos;
import com.uade.skillswap.domain.repository.MovimientoCreditosRepository;

public class InMemoryMovimientoCreditosRepository extends AbstractInMemoryRepository<MovimientoCreditos, String> implements MovimientoCreditosRepository {

    public InMemoryMovimientoCreditosRepository() {
        super(MovimientoCreditos::getId);
    }
}
