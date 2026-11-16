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
        if (!evento.getTipo().registraHistorial()) {
            return; // una propuesta de reprogramacion no es una transicion de estado
        }
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

    /**
     * Por clase, no por instancia: solo debe haber UN registrador de
     * historial adjunto a cada Publicacion/Contratacion. Sin esto,
     * Publicacion/ContratacionService.obtener() re-wirea un observer nuevo
     * en cada llamada (los observers son @Transient) y, cuando el repositorio
     * devuelve siempre la misma instancia en memoria (a diferencia de JPA,
     * donde cada carga es un objeto nuevo), se acumulan copias que duplican
     * cada entrada de historial.
     */
    @Override
    public boolean equals(Object o) {
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
