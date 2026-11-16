package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.WalletSkillCoins;
import com.uade.skillswap.domain.repository.WalletSkillCoinsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.Optional;

public class JpaWalletSkillCoinsRepository extends AbstractJpaRepository<WalletSkillCoins, String> implements WalletSkillCoinsRepository {

    public JpaWalletSkillCoinsRepository() {
        super(WalletSkillCoins.class);
    }

    @Override
    public Optional<WalletSkillCoins> buscarPorTitular(String titularUserId) {
        EntityManager em = emParaLectura();
        try {
            return Optional.of(em.createQuery(
                    "FROM WalletSkillCoins w WHERE w.titularUserId = :titularUserId", WalletSkillCoins.class)
                    .setParameter("titularUserId", titularUserId)
                    .getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
