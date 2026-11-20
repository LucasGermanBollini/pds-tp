package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;

public class EstadoContratacionPendiente extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "PENDIENTE";
    }

    @Override
    public void aceptar(Contratacion ctx, String actorUserId) {
        boolean bloqueado = ctx.getFranjaElegida().reservarCupo();
        if (!bloqueado) {
            throw new IllegalStateException("No hay cupo disponible en la franja elegida");
        }
        ctx.cambiarEstado(new EstadoContratacionAceptada(), actorUserId, null);
    }

    @Override
    public void rechazar(Contratacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoContratacionRechazada(), actorUserId, null);
    }
}
