package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.repository.PerfilRepository;

import java.util.ArrayList;
import java.util.List;

public class BusquedaUiController implements UiControllers.BusquedaController {

    private final PublicacionService publicacionService;
    private final PerfilRepository perfilRepository;

    public BusquedaUiController(PublicacionService publicacionService, PerfilRepository perfilRepository) {
        this.publicacionService = publicacionService;
        this.perfilRepository = perfilRepository;
    }

    @Override
    public List<Models.Publicacion> buscar(Models.FiltrosBusqueda f) {
        return publicacionService.buscar(f.texto(), f.categoria(), UiMappers.aModalidadDominio(f.modalidad()),
                        f.precioMaximo(), f.orden())
                .stream().map(UiMappers::aPublicacionUi).toList();
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        Perfil perfil = perfilRepository.buscarPorId(userId).orElse(null);
        if (perfil == null) {
            return publicacionService.buscarPublicadas().stream().limit(3).map(UiMappers::aPublicacionUi).toList();
        }
        return publicacionService.recomendar(perfil, "AFINIDAD_HABILIDADES").stream()
                .limit(3).map(UiMappers::aPublicacionUi).toList();
    }

    @Override
    public List<String> categorias() {
        List<String> categorias = new ArrayList<>();
        categorias.add("Todas");
        categorias.addAll(publicacionService.listarCategorias());
        return categorias;
    }
}
