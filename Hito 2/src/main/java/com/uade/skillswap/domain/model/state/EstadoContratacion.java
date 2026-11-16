package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;

/** State Pattern - State. Define las acciones posibles sobre una Contratacion (seccion 4.5). */
public interface EstadoContratacion {

    String getNombre();

    void aceptar(Contratacion ctx, String actorUserId);

    void rechazar(Contratacion ctx, String actorUserId);

    void pagar(Contratacion ctx, String actorUserId);

    void iniciar(Contratacion ctx, String actorUserId);

    void completar(Contratacion ctx, String actorUserId);

    void cancelar(Contratacion ctx, String actorUserId, String motivo);

    void proponerReprogramacion(Contratacion ctx, String actorUserId, FranjaDisponibilidad nuevaFranja);
}
