package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.repository.Repositorio;
import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaContext;
import com.uade.skillswap.application.excepcion.ConflictoConcurrenteException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import java.util.List;
import java.util.Optional;

/**
 * Equivalente a SimpleJpaRepository de la catedra, pero transaction-aware:
 * si JpaUnitOfWork ya abrio una transaccion en este hilo, la reutiliza (no
 * hace commit/close, eso lo decide el UnitOfWork); si no hay ninguna, abre
 * y cierra la suya, igual que el ejemplo original (uso standalone).
 */
public abstract class AbstractJpaRepository<T, ID> implements Repositorio<T, ID> {

    private final Class<T> entityClass;

    protected AbstractJpaRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T guardar(T entidad) {
        if (JpaContext.hayTransaccionAmbiente()) {
            return JpaContext.actual().merge(entidad);
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            T guardado = em.merge(entidad);
            em.getTransaction().commit();
            return guardado;
        } catch (OptimisticLockException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new ConflictoConcurrenteException("La entidad fue modificada concurrentemente, reintentar la operacion", ex);
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<T> buscarPorId(ID id) {
        EntityManager em = emParaLectura();
        try {
            return Optional.ofNullable(em.find(entityClass, id));
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    @Override
    public List<T> listarTodos() {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery("FROM " + entityClass.getSimpleName(), entityClass).getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    protected EntityManager emParaLectura() {
        return JpaContext.hayTransaccionAmbiente() ? JpaContext.actual() : JPAUtil.getEntityManager();
    }

    protected void cerrarSiEsPropio(EntityManager em) {
        if (!JpaContext.hayTransaccionAmbiente()) {
            em.close();
        }
    }
}
