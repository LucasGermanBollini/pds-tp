package com.uade.skillswap.domain.model;

/** Decorator Pattern - Concrete Decorator. */
public class DescuentoPorcentual extends PromocionDecorator {

    private final double porcentaje;

    public DescuentoPorcentual(Cotizable cotizableEnvuelto, double porcentaje) {
        super(cotizableEnvuelto);
        if (porcentaje <= 0 || porcentaje > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        this.porcentaje = porcentaje;
    }

    @Override
    public SkillCoins calcularPrecio() {
        SkillCoins base = cotizableEnvuelto.calcularPrecio();
        long descuento = (long) Math.floor(base.valor() * (porcentaje / 100.0));
        return base.restarSinNegativo(SkillCoins.de(descuento));
    }
}
