package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Publicacion;

/** Clase base: cada estado concreto sobrescribe solo las acciones permitidas. */
public abstract class EstadoPublicacionBase implements EstadoPublicacion {

    @Override
    public void publicar(Publicacion ctx, String actorUserId) {
        throw new IllegalStateException("No se puede publicar desde el estado " + getNombre());
    }

    @Override
    public void pausar(Publicacion ctx, String actorUserId) {
        throw new IllegalStateException("No se puede pausar desde el estado " + getNombre());
    }

    @Override
    public void cerrar(Publicacion ctx, String actorUserId) {
        throw new IllegalStateException("No se puede cerrar desde el estado " + getNombre());
    }

    @Override
    public void cancelar(Publicacion ctx, String actorUserId, String motivo) {
        throw new IllegalStateException("No se puede cancelar desde el estado " + getNombre());
    }
}
