package com.uade.skillswap.domain.model;

/** Decorator Pattern - Concrete Decorator. */
public class DescuentoMontoFijo extends PromocionDecorator {

    private final SkillCoins monto;

    public DescuentoMontoFijo(Cotizable cotizableEnvuelto, SkillCoins monto) {
        super(cotizableEnvuelto);
        if (monto == null) {
            throw new IllegalArgumentException("El monto del descuento es obligatorio");
        }
        this.monto = monto;
    }

    @Override
    public SkillCoins calcularPrecio() {
        return cotizableEnvuelto.calcularPrecio().restarSinNegativo(monto);
    }
}
