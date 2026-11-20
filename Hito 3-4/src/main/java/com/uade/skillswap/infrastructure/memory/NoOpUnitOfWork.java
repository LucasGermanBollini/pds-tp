package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.application.port.UnitOfWork;

import java.util.function.Supplier;

/** Para los repositorios en memoria (Hito 2): no hay nada transaccional que coordinar. */
public class NoOpUnitOfWork implements UnitOfWork {

    @Override
    public <T> T ejecutar(Supplier<T> accion) {
        return accion.get();
    }
}
