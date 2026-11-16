package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.SkillCoins;
import com.uade.skillswap.domain.repository.PerfilRepository;

import java.util.List;

public class PublicacionUiController implements UiControllers.PublicacionController {

    private final PublicacionService publicacionService;
    private final PerfilRepository perfilRepository;
    private final IdentidadProviderPort identidadProviderPort;

    public PublicacionUiController(PublicacionService publicacionService, PerfilRepository perfilRepository,
                                    IdentidadProviderPort identidadProviderPort) {
        this.publicacionService = publicacionService;
        this.perfilRepository = perfilRepository;
        this.identidadProviderPort = identidadProviderPort;
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return publicacionService.listarPropias(userId).stream().map(UiMappers::aPublicacionUi).toList();
    }

    /** Seccion 9: los controllers validan permisos con el userId recibido del servicio de identidad mock. */
    @Override
    public Models.Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                                             Models.Modalidad modalidad, int duracion, int precio) {
        identidadProviderPort.validarUsuarioActivo(userId);
        Perfil autor = perfilRepository.buscarPorId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + userId));
        Publicacion publicacion = publicacionService.crearBorradorSimple(autor, titulo, descripcion, categoria,
                UiMappers.aModalidadDominio(modalidad), duracion, SkillCoins.de(precio));
        return UiMappers.aPublicacionUi(publicacion);
    }

    /** "avanzar" generico del kit -> mapea al metodo de dominio que corresponde segun el estado destino. */
    @Override
    public Models.Publicacion cambiarEstado(String userId, String publicacionId, Models.EstadoPublicacion estado) {
        identidadProviderPort.validarUsuarioActivo(userId);
        Publicacion resultado = switch (estado) {
            case PUBLICADA -> publicacionService.publicar(publicacionId, userId);
            case PAUSADA -> publicacionService.pausar(publicacionId, userId);
            case CERRADA -> publicacionService.cerrar(publicacionId, userId);
            case CANCELADA -> publicacionService.cancelar(publicacionId, userId, "Cancelada desde la interfaz");
            case BORRADOR -> throw new IllegalArgumentException("No se puede volver a BORRADOR desde la interfaz");
        };
        return UiMappers.aPublicacionUi(resultado);
    }
}
