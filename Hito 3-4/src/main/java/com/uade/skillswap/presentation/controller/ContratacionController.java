package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;

/**
 * Seccion 9: ContratacionController.solicitar/aceptar/pagar/cancelar/completar,
 * mas rechazar, iniciar y la reprogramacion de comun acuerdo (seccion 4.4).
 */
public class ContratacionController extends ControllerBase {

    private final ContratacionService contratacionService;

    public ContratacionController(ContratacionService contratacionService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.contratacionService = contratacionService;
    }

    public Resultado<ContratacionDTO> solicitar(String actorUserId, SolicitarComando c) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(contratacionService.solicitar(actorUserId,
                c.publicacionId(), c.franjaId(), c.mensaje(), c.objetivo())));
    }

    public Resultado<ContratacionDTO> aceptar(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(contratacionService.aceptar(contratacionId, actorUserId)));
    }

    public Resultado<ContratacionDTO> rechazar(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(contratacionService.rechazar(contratacionId, actorUserId)));
    }

    public Resultado<ContratacionDTO> pagar(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(contratacionService.pagar(contratacionId, actorUserId)));
    }

    public Resultado<ContratacionDTO> iniciar(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(contratacionService.iniciar(contratacionId, actorUserId)));
    }

    public Resultado<ContratacionDTO> completar(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(contratacionService.completar(contratacionId, actorUserId)));
    }

    public Resultado<ContratacionDTO> cancelar(String actorUserId, String contratacionId, String motivo) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(
                contratacionService.cancelar(contratacionId, actorUserId, motivo)));
    }

    public Resultado<ContratacionDTO> proponerReprogramacion(String actorUserId, String contratacionId, String nuevaFranjaId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(
                contratacionService.proponerReprogramacion(contratacionId, actorUserId, nuevaFranjaId)));
    }

    public Resultado<ContratacionDTO> aceptarReprogramacion(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(
                contratacionService.aceptarReprogramacion(contratacionId, actorUserId)));
    }

    public Resultado<ContratacionDTO> rechazarReprogramacion(String actorUserId, String contratacionId) {
        return modificar(actorUserId, () -> DtoMapper.contratacion(
                contratacionService.rechazarReprogramacion(contratacionId, actorUserId)));
    }

    public Resultado<Pagina<ContratacionDTO>> listar(String userId, int pagina, int tamanio) {
        return consultar(() -> Pagina.de(contratacionService.listarPorParticipante(userId), pagina, tamanio)
                .map(DtoMapper::contratacion));
    }

    /** Seccion 4.5: "consultar comprobante" / historial; solo las partes. */
    public Resultado<ContratacionDTO> verComprobante(String actorUserId, String contratacionId) {
        return consultar(() -> DtoMapper.contratacion(contratacionService.verComoParticipante(contratacionId, actorUserId)));
    }
}
