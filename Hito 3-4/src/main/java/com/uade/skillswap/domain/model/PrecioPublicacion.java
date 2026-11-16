package com.uade.skillswap.domain.model;

/** Decorator Pattern - Concrete Component: el precio base de una publicacion, sin promociones. */
public class PrecioPublicacion implements Cotizable {

    private final SkillCoins montoBase;

    public PrecioPublicacion(SkillCoins montoBase) {
        if (montoBase == null) {
            throw new IllegalArgumentException("El monto base es obligatorio");
        }
        this.montoBase = montoBase;
    }

    @Override
    public SkillCoins calcularPrecio() {
        return montoBase;
    }
}
