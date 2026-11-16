package com.uade.skillswap.infrastructure.notification;

import java.util.Map;
import java.util.UUID;

/**
 * Simula un SDK externo de push (p.ej. Firebase Cloud Messaging): recibe un
 * "token de dispositivo" y un payload clave-valor, no un userId + texto
 * plano. Mismo rol que PasarelaPagoLegacy / EmailExternoSdk.
 */
public class PushExternoSdk {

    public String enviarPush(String tokenDispositivo, Map<String, String> payload) {
        if (tokenDispositivo == null || tokenDispositivo.isBlank()) {
            throw new IllegalArgumentException("El token de dispositivo es obligatorio");
        }
        System.out.printf("[PUSH] token %s | payload: %s%n", tokenDispositivo, payload);
        return "PUSH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
