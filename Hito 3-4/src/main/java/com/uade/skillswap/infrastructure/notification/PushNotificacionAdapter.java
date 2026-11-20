package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionPort;

import java.util.Map;

/** Adapter Pattern - adapta PushExternoSdk al contrato que espera la aplicacion. */
public class PushNotificacionAdapter implements CanalNotificacionPort {

    private final PushExternoSdk sdk;

    public PushNotificacionAdapter(PushExternoSdk sdk) {
        this.sdk = sdk;
    }

    @Override
    public void enviar(String destinatarioUserId, String mensaje) {
        String tokenSimulado = "device-token-" + destinatarioUserId;
        sdk.enviarPush(tokenSimulado, Map.of("titulo", "SkillSwap", "cuerpo", mensaje));
    }
}
