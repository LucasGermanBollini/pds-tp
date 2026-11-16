package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;

/** Casos de uso de Perfil: ver el perfil publico (seccion 4.1). */
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final ContratacionRepository contratacionRepository;

    public PerfilService(PerfilRepository perfilRepository, ContratacionRepository contratacionRepository) {
        this.perfilRepository = perfilRepository;
        this.contratacionRepository = contratacionRepository;
    }

    public PerfilResumen verPerfil(String userId) {
        Perfil perfil = perfilRepository.buscarPorId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + userId));
        long completadas = contratacionRepository.buscarPorParticipante(userId).stream()
                .filter(c -> "COMPLETADA".equals(c.getEstadoNombre()))
                .count();
        return new PerfilResumen(perfil, (int) completadas);
    }
}
