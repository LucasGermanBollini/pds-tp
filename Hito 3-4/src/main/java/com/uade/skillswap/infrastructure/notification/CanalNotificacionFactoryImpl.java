package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionFactory;
import com.uade.skillswap.application.port.CanalNotificacionPort;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;

/** Simple Factory Pattern - implementacion concreta: resuelve el canal segun la preferencia del usuario. */
public class CanalNotificacionFactoryImpl implements CanalNotificacionFactory {

    private final CanalNotificacionPort email = new EmailNotificacionAdapter(new EmailExternoSdk());
    private final CanalNotificacionPort push = new PushNotificacionAdapter(new PushExternoSdk());
    private final CanalNotificacionPort interna = new NotificacionInternaAdapter();

    @Override
    public CanalNotificacionPort crear(TipoCanalNotificacion tipo) {
        if (tipo == null) {
            return interna;
        }
        return switch (tipo) {
            case EMAIL -> email;
            case PUSH -> push;
            case INTERNA -> interna;
        };
    }
}
