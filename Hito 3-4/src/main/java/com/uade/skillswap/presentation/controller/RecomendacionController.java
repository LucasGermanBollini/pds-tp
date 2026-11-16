package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.repository.Pagina;

/**
 * Seccion 9: RecomendacionController.recomendar(usuario). La estrategia
 * (AFINIDAD_HABILIDADES o POPULARIDAD_RECIENTE) llega por nombre y la
 * resuelve la Simple Factory: cambiarla no toca ni este controller ni el caso
 * de uso (seccion 15).
 */
public class RecomendacionController extends ControllerBase {

    public static final String ESTRATEGIA_POR_DEFECTO = "AFINIDAD_HABILIDADES";

    private final PublicacionService publicacionService;

    public RecomendacionController(PublicacionService publicacionService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.publicacionService = publicacionService;
    }

    public Resultado<Pagina<PublicacionDTO>> recomendar(String userId, String estrategia, int pagina, int tamanio) {
        return consultar(() -> Pagina.de(publicacionService.recomendar(userId,
                        estrategia == null ? ESTRATEGIA_POR_DEFECTO : estrategia), pagina, tamanio)
                .map(DtoMapper::publicacion));
    }

    public Resultado<Pagina<PublicacionDTO>> recomendar(String userId) {
        return recomendar(userId, ESTRATEGIA_POR_DEFECTO, 0, Pagina.TAMANIO_POR_DEFECTO);
    }
}
