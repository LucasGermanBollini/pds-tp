package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Publicacion;

public class EstadoPublicacionPublicada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "PUBLICADA";
    }

    @Override
    public void pausar(Publicacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoPublicacionPausada(), actorUserId, null);
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
