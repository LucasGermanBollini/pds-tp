package com.uade.skillswap.domain.model;

import java.util.Objects;

/** Composite Pattern - Leaf. */
public class Habilidad implements ElementoCatalogo {

    private final String id;
    private final String nombre;

    public Habilidad(String id, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la habilidad es obligatorio");
        }
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    @Override
    public String obtenerNombre() {
        return nombre;
    }

    @Override
    public boolean esCategoria() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Habilidad h && Objects.equals(h.id, this.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
