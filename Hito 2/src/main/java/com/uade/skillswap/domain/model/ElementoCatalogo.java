package com.uade.skillswap.domain.model;

/**
 * Composite Pattern - Component.
 * Contrato comun entre una habilidad simple (Leaf) y una categoria que
 * agrupa otras categorias/habilidades (Composite).
 */
public interface ElementoCatalogo {

    String obtenerNombre();

    boolean esCategoria();
}
