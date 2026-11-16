package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.MovimientoCreditos;
import com.uade.skillswap.domain.repository.MovimientoCreditosRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaMovimientoCreditosRepository extends AbstractJpaRepository<MovimientoCreditos, String> implements MovimientoCreditosRepository {

    public JpaMovimientoCreditosRepository() {
        super(MovimientoCreditos.class);
    }

    @Override
    public List<MovimientoCreditos> buscarPorWallet(String walletId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "FROM MovimientoCreditos m WHERE m.walletId = :walletId ORDER BY m.fecha DESC", MovimientoCreditos.class)
                    .setParameter("walletId", walletId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
