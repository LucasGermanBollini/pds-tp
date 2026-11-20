package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Publicacion;

/** State Pattern - State. Define las acciones posibles sobre una Publicacion. */
public interface EstadoPublicacion {

    String getNombre();

    void publicar(Publicacion ctx, String actorUserId);

    void pausar(Publicacion ctx, String actorUserId);

    void cerrar(Publicacion ctx, String actorUserId);

    void cancelar(Publicacion ctx, String actorUserId, String motivo);
}
