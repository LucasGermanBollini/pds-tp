package com.uade.skillswap.application.port;

import com.uade.skillswap.domain.model.TipoCanalNotificacion;

/**
 * Simple Factory Pattern - puerto (seccion 7: "Creacion del canal de
 * notificacion... segun la preferencia del usuario"). La fabrica concreta
 * vive en Infraestructura, porque es quien conoce los adaptadores reales
 * de email/push; aca solo se declara "dado un tipo, dame un canal".
 */
public interface CanalNotificacionFactory {

    CanalNotificacionPort crear(TipoCanalNotificacion tipo);
}
