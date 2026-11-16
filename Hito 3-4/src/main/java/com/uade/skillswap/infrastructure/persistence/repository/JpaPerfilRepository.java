package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.repository.PerfilRepository;

public class JpaPerfilRepository extends AbstractJpaRepository<Perfil, String> implements PerfilRepository {

    public JpaPerfilRepository() {
        super(Perfil.class);
    }
}
