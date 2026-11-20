package com.uade.skillswap.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * Puerto generico de persistencia. El dominio y la aplicacion trabajan contra
 * esta interfaz, nunca contra un EntityManager/SQL directo. Las implementaciones
 * en memoria de este Hito 2 se reemplazan por JPA/Hibernate en el Hito 3 sin
 * tocar los casos de uso.
 */
public interface Repositorio<T, ID> {

    T guardar(T entidad);

    Optional<T> buscarPorId(ID id);

    List<T> listarTodos();
}
