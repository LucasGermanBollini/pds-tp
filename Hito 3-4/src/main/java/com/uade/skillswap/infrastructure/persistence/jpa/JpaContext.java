package com.uade.skillswap.infrastructure.persistence.jpa;

import jakarta.persistence.EntityManager;

/**
 * Sostiene, por hilo, el EntityManager de la transaccion ambiente abierta por
 * JpaUnitOfWork (si hay una). AbstractJpaRepository lo consulta: si hay una
 * transaccion ambiente, participa en ella (no abre ni cierra nada); si no,
 * se comporta como SimpleJpaRepository de la catedra (abre/cierra la suya).
 */
public final class JpaContext {

    private static final ThreadLocal<EntityManager> ACTUAL = new ThreadLocal<>();

    private JpaContext() {
    }

    static void bind(EntityManager em) {
        ACTUAL.set(em);
    }

    static void unbind() {
        ACTUAL.remove();
    }

    public static EntityManager actual() {
        return ACTUAL.get();
    }

    public static boolean hayTransaccionAmbiente() {
        return ACTUAL.get() != null;
    }
}
