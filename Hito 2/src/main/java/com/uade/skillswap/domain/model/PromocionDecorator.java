package com.uade.skillswap.domain.model;

/**
 * Decorator Pattern - Decorator base.
 * Envuelve otro Cotizable y permite encadenar promociones sin tocar
 * PrecioPublicacion ni explotar en subclases (Open/Closed).
 */
public abstract class PromocionDecorator implements Cotizable {

    protected final Cotizable cotizableEnvuelto;

    protected PromocionDecorator(Cotizable cotizableEnvuelto) {
        if (cotizableEnvuelto == null) {
            throw new IllegalArgumentException("El cotizable envuelto es obligatorio");
        }
        this.cotizableEnvuelto = cotizableEnvuelto;
    }
}
