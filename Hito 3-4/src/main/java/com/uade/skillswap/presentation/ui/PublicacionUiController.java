package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.PublicacionController;
import com.uade.skillswap.presentation.controller.comando.CrearPublicacionComando;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Adapter (contrato del Mock UI Kit -> PublicacionController). */
public class PublicacionUiController implements UiControllers.PublicacionController {

    private final PublicacionController publicacionController;

    public PublicacionUiController(PublicacionController publicacionController) {
        this.publicacionController = publicacionController;
    }

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        return publicacionController.listarPropias(userId, 0, Pagina.TAMANIO_MAXIMO).valorOLanzar().contenido().stream()
                .map(UiMappers::aPublicacionUi).toList();
    }

    /**
     * El formulario del kit no pide franjas, pero publicar exige al menos una
     * (seccion 4.2). Decision de la interfaz (no del dominio): se crea una
     * franja por defecto dentro de una semana, con cupo 5.
     */
    @Override
    public Models.Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                                             Models.Modalidad modalidad, int duracion, int precio) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.HOURS);
        CrearPublicacionComando comando = new CrearPublicacionComando(titulo, descripcion, categoria,
                UiMappers.aModalidad(modalidad), duracion, precio, List.of(),
                List.of(new CrearPublicacionComando.Franja(inicio, inicio.plusMinutes(Math.max(30, duracion)), 5)), null);
        return UiMappers.aPublicacionUi(publicacionController.crearBorrador(userId, comando).valorOLanzar());
    }

    @Override
    public Models.Publicacion cambiarEstado(String userId, String publicacionId, Models.EstadoPublicacion estado) {
        String motivo = estado == Models.EstadoPublicacion.CANCELADA ? "Cancelada desde la interfaz" : null;
        return UiMappers.aPublicacionUi(publicacionController.cambiarEstado(userId, publicacionId, estado.name(), motivo)
                .valorOLanzar());
    }
}
