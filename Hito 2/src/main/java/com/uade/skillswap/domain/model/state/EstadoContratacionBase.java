package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;

public abstract class EstadoContratacionBase implements EstadoContratacion {

    @Override
    public void aceptar(Contratacion ctx, String actorUserId) {
        throw error("aceptar");
    }

    @Override
    public void rechazar(Contratacion ctx, String actorUserId) {
        throw error("rechazar");
    }

    @Override
    public void pagar(Contratacion ctx, String actorUserId) {
        throw error("pagar");
    }

    @Override
    public void iniciar(Contratacion ctx, String actorUserId) {
        throw error("iniciar");
    }

    @Override
    public void completar(Contratacion ctx, String actorUserId) {
        throw error("completar");
    }

    @Override
    public void cancelar(Contratacion ctx, String actorUserId, String motivo) {
        throw error("cancelar");
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx, String actorUserId, FranjaDisponibilidad nuevaFranja) {
        throw error("proponer una reprogramacion");
    }

    private IllegalStateException error(String accion) {
        return new IllegalStateException("No se puede " + accion + " desde el estado " + getNombre());
    }
}
