package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.CalificacionDTO;
import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;

/** Seccion 9: CalificacionController.calificar(...): "Registrar calificacion verificada". */
public class CalificacionController extends ControllerBase {

    private final ContratacionService contratacionService;

    public CalificacionController(ContratacionService contratacionService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.contratacionService = contratacionService;
    }

    public Resultado<CalificacionDTO> calificar(String actorUserId, CalificarComando c) {
        return modificar(actorUserId, () -> DtoMapper.calificacion(
                contratacionService.calificar(c.contratacionId(), actorUserId, c.puntaje(), c.comentario())));
    }

    public Resultado<Pagina<CalificacionDTO>> listarRecibidas(String userId, int pagina, int tamanio) {
        return consultar(() -> Pagina.de(contratacionService.listarCalificacionesRecibidas(userId), pagina, tamanio)
                .map(DtoMapper::calificacion));
    }
}
