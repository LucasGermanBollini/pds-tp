package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Publicacion;

public class EstadoPublicacionBorrador extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "BORRADOR";
    }

    @Override
    public void publicar(Publicacion ctx, String actorUserId) {
        ctx.validarRequisitosDePublicacion();
        ctx.cambiarEstado(new EstadoPublicacionPublicada(), actorUserId, null);
    }

    @Override
    public void cancelar(Publicacion ctx, String actorUserId, String motivo) {
        ctx.cambiarEstado(new EstadoPublicacionCancelada(), actorUserId, motivo);
    }
}
