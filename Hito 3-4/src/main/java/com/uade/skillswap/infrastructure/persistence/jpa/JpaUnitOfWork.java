package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.application.port.UnitOfWork;
import jakarta.persistence.EntityManager;

import java.util.function.Supplier;

/**
 * Implementacion JPA del limite transaccional: abre un EntityManager, lo
 * publica en JpaContext para que los repositorios lo reutilicen durante la
 * ejecucion del bloque, y hace commit/rollback de todo junto al final.
 */
public class JpaUnitOfWork implements UnitOfWork {

    @Override
    public <T> T ejecutar(Supplier<T> accion) {
        if (JpaContext.hayTransaccionAmbiente()) {
            // ya hay una transaccion en curso (llamada anidada): se reusa, no se anida otra.
            return accion.get();
        }

        EntityManager em = JPAUtil.getEntityManager();
        JpaContext.bind(em);
        try {
            em.getTransaction().begin();
            T resultado = accion.get();
            em.getTransaction().commit();
            return resultado;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            JpaContext.unbind();
            em.close();
        }
    }
}
