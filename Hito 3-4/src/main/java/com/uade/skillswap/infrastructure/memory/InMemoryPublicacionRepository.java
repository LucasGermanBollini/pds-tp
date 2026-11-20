package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.ReglasDeBusqueda;
import com.uade.skillswap.domain.repository.CriteriosBusqueda;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.util.List;

public class InMemoryPublicacionRepository extends AbstractInMemoryRepository<Publicacion, String> implements PublicacionRepository {

    public InMemoryPublicacionRepository() {
        super(Publicacion::getId);
    }

    @Override
    public List<Publicacion> buscarPorAutor(String autorUserId) {
        return listarTodos().stream().filter(p -> p.getAutor().obtenerId().equals(autorUserId)).toList();
    }

    @Override
    public Pagina<Publicacion> buscar(CriteriosBusqueda criterios) {
        List<Publicacion> filtradas = listarTodos().stream()
                .filter(p -> ReglasDeBusqueda.coincide(p, criterios))
                .sorted(ReglasDeBusqueda.comparador(criterios))
                .toList();
        return Pagina.de(filtradas, criterios.pagina(), criterios.tamanio());
    }

    @Override
    public List<Publicacion> buscarPublicadas() {
        return listarTodos().stream().filter(Publicacion::estaPublicada).toList();
    }
}
