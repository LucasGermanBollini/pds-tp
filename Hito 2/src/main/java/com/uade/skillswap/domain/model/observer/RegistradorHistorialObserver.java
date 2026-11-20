package com.uade.skillswap.domain.model.observer;

import com.uade.skillswap.domain.model.HistorialEstado;
import com.uade.skillswap.domain.repository.HistorialEstadoRepository;

import java.util.UUID;

/**
 * Observer Pattern - Observer concreto.
 * Escucha cambios de estado de Publicacion/Contratacion y los deja en el
 * historial inmutable (seccion 5), sin que esas clases sepan que existe.
 */
public class RegistradorHistorialObserver implements ObservadorDeEstado {

    private final HistorialEstadoRepository historialEstadoRepository;

    public RegistradorHistorialObserver(HistorialEstadoRepository historialEstadoRepository) {
        this.historialEstadoRepository = historialEstadoRepository;
    }

    @Override
    public void actualizar(CambioEstadoEvento evento) {
        HistorialEstado registro = new HistorialEstado(
                UUID.randomUUID().toString(),
                evento.getEntidadTipo(),
                evento.getEntidadId(),
                evento.getEstadoAnterior(),
                evento.getEstadoNuevo(),
                evento.getActorUserId(),
                evento.getMotivo(),
                evento.getFecha());
        historialEstadoRepository.guardar(registro);
    }
}
