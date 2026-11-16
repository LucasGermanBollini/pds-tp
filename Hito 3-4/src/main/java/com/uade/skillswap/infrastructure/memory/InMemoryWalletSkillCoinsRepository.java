package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.WalletSkillCoins;
import com.uade.skillswap.domain.repository.WalletSkillCoinsRepository;

import java.util.Optional;

public class InMemoryWalletSkillCoinsRepository extends AbstractInMemoryRepository<WalletSkillCoins, String> implements WalletSkillCoinsRepository {

    public InMemoryWalletSkillCoinsRepository() {
        super(WalletSkillCoins::getId);
    }

    @Override
    public Optional<WalletSkillCoins> buscarPorTitular(String titularUserId) {
        return listarTodos().stream().filter(w -> w.getTitularUserId().equals(titularUserId)).findFirst();
    }
}
