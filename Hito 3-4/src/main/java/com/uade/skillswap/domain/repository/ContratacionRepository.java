package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.Contratacion;

import java.util.List;

public interface ContratacionRepository extends Repositorio<Contratacion, String> {

    /** Contrataciones donde el usuario participa, como cliente o como autor de la publicacion. */
    List<Contratacion> buscarPorParticipante(String userId);
}
