package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;

public class EstadoContratacionConfirmada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "CONFIRMADA";
    }

    @Override
    public void iniciar(Contratacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoContratacionEnCurso(), actorUserId, null);
    }

    @Override
    public void cancelar(Contratacion ctx, String actorUserId, String motivo) {
        ctx.getFranjaElegida().liberarCupo();
        ctx.cambiarEstado(new EstadoContratacionCancelada(), actorUserId, motivo);
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx, String actorUserId, FranjaDisponibilidad nuevaFranja) {
        ctx.reprogramar(nuevaFranja);
    }
}
