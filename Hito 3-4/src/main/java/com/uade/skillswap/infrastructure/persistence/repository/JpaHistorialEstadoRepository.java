package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.HistorialEstado;
import com.uade.skillswap.domain.repository.HistorialEstadoRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaHistorialEstadoRepository extends AbstractJpaRepository<HistorialEstado, String> implements HistorialEstadoRepository {

    public JpaHistorialEstadoRepository() {
        super(HistorialEstado.class);
    }

    @Override
    public List<HistorialEstado> buscarPorEntidad(String entidadTipo, String entidadId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "FROM HistorialEstado h WHERE h.entidadTipo = :tipo AND h.entidadId = :id ORDER BY h.fecha",
                    HistorialEstado.class)
                    .setParameter("tipo", entidadTipo)
                    .setParameter("id", entidadId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
