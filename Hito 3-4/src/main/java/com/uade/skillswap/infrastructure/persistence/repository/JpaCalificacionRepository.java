package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.Calificacion;
import com.uade.skillswap.domain.repository.CalificacionRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaCalificacionRepository extends AbstractJpaRepository<Calificacion, String> implements CalificacionRepository {

    public JpaCalificacionRepository() {
        super(Calificacion.class);
    }

    @Override
    public List<Calificacion> buscarPorReceptor(String receptorUserId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery("FROM Calificacion c WHERE c.receptorUserId = :receptorUserId", Calificacion.class)
                    .setParameter("receptorUserId", receptorUserId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    @Override
    public List<Calificacion> buscarPorContratacion(String contratacionId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery("FROM Calificacion c WHERE c.contratacionId = :contratacionId", Calificacion.class)
                    .setParameter("contratacionId", contratacionId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
