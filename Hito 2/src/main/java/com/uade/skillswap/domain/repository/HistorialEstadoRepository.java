package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.HistorialEstado;

import java.util.List;

public interface HistorialEstadoRepository extends Repositorio<HistorialEstado, String> {

    List<HistorialEstado> buscarPorEntidad(String entidadTipo, String entidadId);
}
