package com.uade.skillswap.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Composite Pattern - Composite: agrupa subcategorias y/o habilidades hoja. */
@Entity
@DiscriminatorValue("CATEGORIA")
public class CategoriaHabilidad extends ElementoCatalogoBase {

    /**
     * mappedBy "categoriaPadre" (definido en ElementoCatalogoBase): esta
     * categoria es el lado "uno" de la relacion arbol 1-a-muchos. LAZY para
     * no traer todo el arbol cada vez que se toca una categoria.
     */
    @OneToMany(mappedBy = "categoriaPadre", fetch = FetchType.LAZY)
    private List<ElementoCatalogoBase> hijos = new ArrayList<>();

    protected CategoriaHabilidad() {
        // JPA
    }

    public CategoriaHabilidad(String id, String nombre) {
        super(id, nombre);
    }

    @Override
    public boolean esCategoria() {
        return true;
    }

    public void agregarElemento(ElementoCatalogoBase elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("El elemento es obligatorio");
        }
        if (elemento == this) {
            throw new IllegalArgumentException("Una categoria no puede contenerse a si misma");
        }
        elemento.asignarCategoriaPadre(this);
        hijos.add(elemento);
    }

    public void quitarElemento(ElementoCatalogoBase elemento) {
        hijos.remove(elemento);
        elemento.asignarCategoriaPadre(null);
    }

    public List<ElementoCatalogo> getElementos() {
        return List.copyOf(hijos);
    }

    /** Recorre el arbol completo (categorias y habilidades) de forma uniforme. */
    public List<ElementoCatalogo> listarDescendientes() {
        List<ElementoCatalogo> resultado = new ArrayList<>();
        for (ElementoCatalogoBase e : hijos) {
            resultado.add(e);
            if (e instanceof CategoriaHabilidad categoria) {
                resultado.addAll(categoria.listarDescendientes());
            }
        }
        return resultado;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CategoriaHabilidad c && c.getId() != null && c.getId().equals(this.getId());
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
