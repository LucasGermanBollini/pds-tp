package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionPort;

/** Canal INTERNA: no hay SDK externo que adaptar, solo deja constancia en consola (la bandeja real la escribe NotificadorObserver). */
public class NotificacionInternaAdapter implements CanalNotificacionPort {

    @Override
    public void enviar(String destinatarioUserId, String mensaje) {
        System.out.printf("[INTERNA] para %s | %s%n", destinatarioUserId, mensaje);
    }
}
