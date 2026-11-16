package com.uade.skillswap.domain.model;

/** Decorator Pattern - Component. Cualquier cosa que sepa calcular su propio precio. */
public interface Cotizable {

    SkillCoins calcularPrecio();
}
