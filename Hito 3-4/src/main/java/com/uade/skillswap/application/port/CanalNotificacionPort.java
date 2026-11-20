package com.uade.skillswap.application.port;

/**
 * Adapter Pattern - puerto. El dominio/aplicacion depende de esta
 * abstraccion, nunca de un SDK de email/push concreto (igual que
 * ProcesadorPago en el ejemplo de Strategy+Adapter de la catedra).
 */
public interface CanalNotificacionPort {

    void enviar(String destinatarioUserId, String mensaje);
}
