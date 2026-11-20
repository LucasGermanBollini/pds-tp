package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;

public class EstadoContratacionAceptada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "ACEPTADA";
    }

    /**
     * El movimiento de SkillCoins (debito/credito, validacion de saldo) lo
     * hace el caso de uso ANTES de llamar a esta transicion; el estado solo
     * valida que el orden de pasos sea el correcto (seccion 4.5).
     */
    @Override
    public void pagar(Contratacion ctx, String actorUserId) {
        ctx.cambiarEstado(new EstadoContratacionConfirmada(), actorUserId, null);
    }

    @Override
    public void cancelar(Contratacion ctx, String actorUserId, String motivo) {
        ctx.getFranjaElegida().liberarCupo();
        ctx.cambiarEstado(new EstadoContratacionCancelada(), actorUserId, motivo);
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx, String actorUserId, FranjaDisponibilidad nuevaFranja) {
        ctx.registrarPropuestaDeReprogramacion(actorUserId, nuevaFranja);
    }

    /** Seccion 4.4: solo "antes de comenzar" (ACEPTADA o CONFIRMADA); EN_CURSO ya no lo admite. */
    @Override
    public void aceptarReprogramacion(Contratacion ctx, String actorUserId) {
        ctx.aplicarReprogramacion(actorUserId);
    }
}
