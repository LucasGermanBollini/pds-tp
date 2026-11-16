package com.uade.skillswap.infrastructure.memory;

import com.uade.skillswap.domain.repository.Repositorio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementacion en memoria compartida por todos los repositorios del Hito 2.
 * Se reemplaza por una implementacion JPA/Hibernate en el Hito 3 sin que la
 * capa de Aplicacion se entere (Clean Architecture: el puerto no cambia).
 */
public abstract class AbstractInMemoryRepository<T, ID> implements Repositorio<T, ID> {

    private final Map<ID, T> datos = new LinkedHashMap<>();
    private final Function<T, ID> idExtractor;

    protected AbstractInMemoryRepository(Function<T, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public T guardar(T entidad) {
        datos.put(idExtractor.apply(entidad), entidad);
        return entidad;
    }

    @Override
    public Optional<T> buscarPorId(ID id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(datos.values());
    }
}
