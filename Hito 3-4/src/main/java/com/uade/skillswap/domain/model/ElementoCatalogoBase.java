package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

/**
 * Composite Pattern - Component persistible (equivalente a Vendible en el
 * ejemplo de la catedra: Vendible implements ItemVendible). ElementoCatalogo
 * sigue siendo una interfaz simple de dominio; esta clase abstracta es la
 * que realmente se mapea a la tabla unica (SINGLE_TABLE) con discriminador.
 */
@Entity
@Table(name = "elementos_catalogo")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_elemento")
public abstract class ElementoCatalogoBase implements ElementoCatalogo {

    @Id
    @Column(length = 36)
    protected String id;

    @Column(nullable = false)
    protected String nombre;

    /** Cada elemento (hoja o categoria) puede colgar de una categoria padre. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_padre_id")
    protected CategoriaHabilidad categoriaPadre;

    protected ElementoCatalogoBase() {
        // JPA
    }

    protected ElementoCatalogoBase(String id, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
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

    void asignarCategoriaPadre(CategoriaHabilidad categoriaPadre) {
        this.categoriaPadre = categoriaPadre;
    }
}
