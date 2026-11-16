package com.uade.skillswap.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/** Composite Pattern - Leaf. */
@Entity
@DiscriminatorValue("HABILIDAD")
public class Habilidad extends ElementoCatalogoBase {

    protected Habilidad() {
        // JPA
    }

    public Habilidad(String id, String nombre) {
        super(id, nombre);
    }

    @Override
    public boolean esCategoria() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Habilidad h && h.getId() != null && h.getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? 0 : getId().hashCode();
    }

    @Override
    public String toString() {
        return obtenerNombre();
    }
}
