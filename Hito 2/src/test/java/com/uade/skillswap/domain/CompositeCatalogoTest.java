package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.CategoriaHabilidad;
import com.uade.skillswap.domain.model.ElementoCatalogo;
import com.uade.skillswap.domain.model.Habilidad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeCatalogoTest {

    @Test
    void unaCategoriaPuedeContenerSubcategoriasYHabilidadesDeFormaUniforme() {
        CategoriaHabilidad desarrollo = new CategoriaHabilidad("cat-1", "Desarrollo");
        CategoriaHabilidad backend = new CategoriaHabilidad("cat-2", "Backend");
        Habilidad java = new Habilidad("h-1", "Java");
        Habilidad sql = new Habilidad("h-2", "SQL");

        backend.agregarElemento(java);
        backend.agregarElemento(sql);
        desarrollo.agregarElemento(backend);

        List<ElementoCatalogo> descendientes = desarrollo.listarDescendientes();

        assertEquals(3, descendientes.size(), "backend + java + sql");
        assertTrue(descendientes.contains(java));
        assertTrue(descendientes.contains(sql));
        assertTrue(descendientes.contains(backend));
    }

    @Test
    void unaHabilidadEsHojaYUnaCategoriaEsComposite() {
        assertFalse(new Habilidad("h-1", "Java").esCategoria());
        assertTrue(new CategoriaHabilidad("cat-1", "Backend").esCategoria());
    }
}
