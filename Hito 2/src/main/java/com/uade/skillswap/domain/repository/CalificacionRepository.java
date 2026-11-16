package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.Calificacion;

import java.util.List;

public interface CalificacionRepository extends Repositorio<Calificacion, String> {

    List<Calificacion> buscarPorReceptor(String receptorUserId);

    List<Calificacion> buscarPorContratacion(String contratacionId);
}
