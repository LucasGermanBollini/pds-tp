package com.uade.skillswap.infrastructure.notification;

import java.util.UUID;

/**
 * Simula una libreria externa de envio de correo (p.ej. un SDK de SendGrid
 * o Mailgun) que no podemos modificar: su contrato usa "direccion" +
 * "asunto" + "cuerpo", incompatible con CanalNotificacionPort.enviar(userId,
 * mensaje). Mismo rol que PasarelaPagoLegacy en el ejemplo de Strategy+
 * Adapter de la catedra.
 */
public class EmailExternoSdk {

    public String despacharCorreo(String direccionDestino, String asunto, String cuerpo) {
        if (direccionDestino == null || direccionDestino.isBlank()) {
            throw new IllegalArgumentException("La direccion de destino es obligatoria");
        }
        System.out.printf("[EMAIL] para %s | asunto: %s | cuerpo: %s%n", direccionDestino, asunto, cuerpo);
        return "EMAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
