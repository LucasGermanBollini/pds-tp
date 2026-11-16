package com.uade.skillswap.application.port;

import java.util.function.Supplier;

/**
 * Puerto de limite transaccional (seccion 8: "Definir limites transaccionales
 * para aceptacion, reserva, pago y cancelacion"). Vive en Aplicacion, igual
 * que ProcesadorPago en el ejemplo de Strategy+Adapter de la catedra: un
 * caso de uso que toca varias entidades (ej. debitar una wallet, acreditar
 * otra, guardar dos movimientos y la contratacion) pide que todo eso corra
 * dentro de una unica transaccion.
 */
public interface UnitOfWork {

    <T> T ejecutar(Supplier<T> accion);

    default void ejecutar(Runnable accion) {
        ejecutar(() -> {
            accion.run();
            return null;
        });
    }
}
