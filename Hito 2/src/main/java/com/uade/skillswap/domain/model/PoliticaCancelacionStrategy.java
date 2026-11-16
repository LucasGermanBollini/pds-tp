package com.uade.skillswap.domain.model;

import java.time.LocalDateTime;

/**
 * Strategy Pattern - Strategy.
 * Seccion 5: "La cancelacion devuelve 100%, 50% o 0% segun la anticipacion
 * configurable". La anticipacion se mide contra el inicio de la franja.
 */
public interface PoliticaCancelacionStrategy {

    String getNombre();

    SkillCoins calcularReintegro(SkillCoins montoPagado, Contratacion contratacion, LocalDateTime momentoCancelacion);
}
