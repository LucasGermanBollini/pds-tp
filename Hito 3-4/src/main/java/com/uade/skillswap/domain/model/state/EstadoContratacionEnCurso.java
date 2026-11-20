package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;

public class EstadoContratacionEnCurso extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "EN_CURSO";
    }

    @Override
    public void completar(Contratacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoContratacionCompletada(), actorUserId, null);
    }
}
