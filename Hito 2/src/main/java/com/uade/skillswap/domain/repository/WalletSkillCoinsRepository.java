package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.WalletSkillCoins;

import java.util.Optional;

public interface WalletSkillCoinsRepository extends Repositorio<WalletSkillCoins, String> {

    Optional<WalletSkillCoins> buscarPorTitular(String titularUserId);
}
