package com.uade.skillswap.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Composite Pattern - Composite: agrupa subcategorias y/o habilidades hoja. */
public class CategoriaHabilidad implements ElementoCatalogo {

    private final String id;
    private final String nombre;
    private final List<ElementoCatalogo> elementos = new ArrayList<>();

    public CategoriaHabilidad(String id, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio");
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
        return true;
    }

    public void agregarElemento(ElementoCatalogo elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("El elemento es obligatorio");
        }
        if (elemento == this) {
            throw new IllegalArgumentException("Una categoria no puede contenerse a si misma");
        }
        elementos.add(elemento);
    }

    public void quitarElemento(ElementoCatalogo elemento) {
        elementos.remove(elemento);
    }

    public List<ElementoCatalogo> getElementos() {
        return List.copyOf(elementos);
    }

    /** Recorre el arbol completo (categorias y habilidades) de forma uniforme. */
    public List<ElementoCatalogo> listarDescendientes() {
        List<ElementoCatalogo> resultado = new ArrayList<>();
        for (ElementoCatalogo e : elementos) {
            resultado.add(e);
            if (e instanceof CategoriaHabilidad categoria) {
                resultado.addAll(categoria.listarDescendientes());
            }
        }
        return resultado;
    }
}
