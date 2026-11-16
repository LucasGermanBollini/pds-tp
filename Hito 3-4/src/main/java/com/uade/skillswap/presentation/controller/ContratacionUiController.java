package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.repository.PerfilRepository;

import java.util.List;

public class ContratacionUiController implements UiControllers.ContratacionController {

    private final ContratacionService contratacionService;
    private final PublicacionService publicacionService;
    private final PerfilRepository perfilRepository;
    private final IdentidadProviderPort identidadProviderPort;

    public ContratacionUiController(ContratacionService contratacionService, PublicacionService publicacionService,
                                     PerfilRepository perfilRepository, IdentidadProviderPort identidadProviderPort) {
        this.contratacionService = contratacionService;
        this.publicacionService = publicacionService;
        this.perfilRepository = perfilRepository;
        this.identidadProviderPort = identidadProviderPort;
    }

    @Override
    public List<Models.Contratacion> listarContrataciones(String userId) {
        return contratacionService.listarPorParticipante(userId).stream()
                .map(c -> UiMappers.aContratacionUi(c, userId)).toList();
    }

    /** El kit no pide franja ni mensaje: se toma la primera franja de la publicacion con cupo disponible. */
    @Override
    public Models.Contratacion solicitar(String userId, String publicacionId, String objetivo) {
        identidadProviderPort.validarUsuarioActivo(userId);
        Perfil cliente = perfilRepository.buscarPorId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + userId));
        Publicacion publicacion = publicacionService.buscarPorId(publicacionId);
        FranjaDisponibilidad franja = publicacion.getFranjas().stream()
                .filter(f -> f.getCupoDisponible() > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La publicacion no tiene franjas con cupo disponible"));
        Contratacion contratacion = contratacionService.solicitar(cliente, publicacion, franja, "", objetivo);
        return UiMappers.aContratacionUi(contratacion, userId);
    }

    /** "avanzar" generico del kit -> mira el estado actual y dispara la accion de dominio correspondiente. */
    @Override
    public Models.Contratacion avanzar(String userId, String contratacionId) {
        identidadProviderPort.validarUsuarioActivo(userId);
        Contratacion actual = contratacionService.buscarPorId(contratacionId);
        Contratacion resultado = switch (actual.getEstadoNombre()) {
            case "PENDIENTE" -> contratacionService.aceptar(contratacionId, userId);
            case "ACEPTADA" -> contratacionService.pagar(contratacionId, userId,
                    new PrecioPublicacion(actual.getPublicacion().getPrecioBase()));
            case "CONFIRMADA" -> contratacionService.iniciar(contratacionId, userId);
            case "EN_CURSO" -> contratacionService.completar(contratacionId, userId);
            default -> actual; // COMPLETADA/RECHAZADA/CANCELADA son terminales: no hay "siguiente".
        };
        return UiMappers.aContratacionUi(resultado, userId);
    }

    @Override
    public Models.Contratacion cancelar(String userId, String contratacionId) {
        identidadProviderPort.validarUsuarioActivo(userId);
        Contratacion resultado = contratacionService.cancelar(contratacionId, userId, null);
        return UiMappers.aContratacionUi(resultado, userId);
    }
}
