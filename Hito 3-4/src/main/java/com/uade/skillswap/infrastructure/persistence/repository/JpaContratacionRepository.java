package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaContratacionRepository extends AbstractJpaRepository<Contratacion, String> implements ContratacionRepository {

    public JpaContratacionRepository() {
        super(Contratacion.class);
    }

    @Override
    public List<Contratacion> buscarPorParticipante(String userId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "SELECT c FROM Contratacion c WHERE c.cliente.id = :userId OR c.publicacion.autor.id = :userId",
                    Contratacion.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
