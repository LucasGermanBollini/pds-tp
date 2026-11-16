package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.presentation.controller.BusquedaController;
import com.uade.skillswap.presentation.controller.RecomendacionController;
import com.uade.skillswap.presentation.controller.comando.BuscarPublicacionesComando;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter (contrato del Mock UI Kit -> nuestros controllers). La pantalla
 * "Explorar" no tiene paginador: se le entrega la primera pagina.
 */
public class BusquedaUiController implements UiControllers.BusquedaController {

    static final int RESULTADOS_POR_PANTALLA = 30;

    private final BusquedaController busquedaController;
    private final RecomendacionController recomendacionController;

    public BusquedaUiController(BusquedaController busquedaController, RecomendacionController recomendacionController) {
        this.busquedaController = busquedaController;
        this.recomendacionController = recomendacionController;
    }

    @Override
    public List<Models.Publicacion> buscar(Models.FiltrosBusqueda f) {
        BuscarPublicacionesComando comando = new BuscarPublicacionesComando(f.texto(), f.categoria(),
                UiMappers.aModalidad(f.modalidad()), null, f.precioMaximo() > 0 ? (long) f.precioMaximo() : null,
                null, null, null, f.orden(), 0, RESULTADOS_POR_PANTALLA);
        return busquedaController.buscar(comando).valorOLanzar().contenido().stream()
                .map(UiMappers::aPublicacionUi).toList();
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        return recomendacionController.recomendar(userId, RecomendacionController.ESTRATEGIA_POR_DEFECTO, 0, 3)
                .valorOLanzar().contenido().stream().map(UiMappers::aPublicacionUi).toList();
    }

    @Override
    public List<String> categorias() {
        List<String> categorias = new ArrayList<>();
        categorias.add("Todas");
        categorias.addAll(busquedaController.categorias().valorOLanzar());
        return categorias;
    }
}
