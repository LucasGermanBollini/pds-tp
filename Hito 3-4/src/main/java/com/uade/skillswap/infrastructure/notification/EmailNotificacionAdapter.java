package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionPort;

/** Adapter Pattern - adapta EmailExternoSdk al contrato que espera la aplicacion. */
public class EmailNotificacionAdapter implements CanalNotificacionPort {

    private final EmailExternoSdk sdk;

    public EmailNotificacionAdapter(EmailExternoSdk sdk) {
        this.sdk = sdk;
    }

    @Override
    public void enviar(String destinatarioUserId, String mensaje) {
        String direccion = destinatarioUserId + "@skillswap.test";
        sdk.despacharCorreo(direccion, "Novedades en SkillSwap", mensaje);
    }
}
