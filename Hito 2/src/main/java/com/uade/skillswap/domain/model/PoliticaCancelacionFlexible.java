package com.uade.skillswap.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;

/** Strategy Pattern - Concrete Strategy. 100% / 50% / 0% con ventanas de 24 y 6 horas. */
public class PoliticaCancelacionFlexible implements PoliticaCancelacionStrategy {

    @Override
    public String getNombre() {
        return "FLEXIBLE";
    }

    @Override
    public SkillCoins calcularReintegro(SkillCoins montoPagado, Contratacion contratacion, LocalDateTime momentoCancelacion) {
        Duration anticipacion = Duration.between(momentoCancelacion, contratacion.getFranjaElegida().getFechaHoraInicio());
        if (anticipacion.toHours() >= 24) {
            return montoPagado;
        }
        if (anticipacion.toHours() >= 6) {
            return SkillCoins.de(montoPagado.valor() / 2);
        }
        return SkillCoins.CERO;
    }
}
