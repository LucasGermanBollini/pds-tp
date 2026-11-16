package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Publicacion;

public class EstadoPublicacionPausada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "PAUSADA";
    }

    @Override
    public void publicar(Publicacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoPublicacionPublicada(), actorUserId, null);
    }

    @Override
    public void cerrar(Publicacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoPublicacionCerrada(), actorUserId, null);
    }

    @Override
    public void cancelar(Publicacion ctx, String actorUserId, String motivo) {
        ctx.cambiarEstado(new EstadoPublicacionCancelada(), actorUserId, motivo);
    }
}
