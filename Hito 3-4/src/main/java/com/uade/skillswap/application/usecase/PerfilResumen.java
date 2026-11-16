package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.Perfil;

/** Agrega datos de Perfil con cosas que viven en otros agregados (section 4.1: "perfil publico con... historial resumido"). */
public record PerfilResumen(Perfil perfil, int proyectosCompletados) {
}
