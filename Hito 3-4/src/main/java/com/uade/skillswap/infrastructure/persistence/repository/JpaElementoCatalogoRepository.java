package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.CategoriaHabilidad;
import com.uade.skillswap.domain.model.Habilidad;
import com.uade.skillswap.domain.model.ElementoCatalogoBase;
import com.uade.skillswap.domain.repository.ElementoCatalogoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;

public class JpaElementoCatalogoRepository extends AbstractJpaRepository<ElementoCatalogoBase, String> implements ElementoCatalogoRepository {

    public JpaElementoCatalogoRepository() {
        super(ElementoCatalogoBase.class);
    }

    @Override
    public Optional<CategoriaHabilidad> buscarCategoriaPorNombre(String nombre) {
        EntityManager em = emParaLectura();
        try {
            return Optional.of(em.createQuery(
                    "FROM CategoriaHabilidad c WHERE lower(c.nombre) = lower(:nombre)", CategoriaHabilidad.class)
                    .setParameter("nombre", nombre)
                    .getSingleResult());
        } catch (NoResultException ex) {
            return Optional.empty();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    @Override
    public List<CategoriaHabilidad> listarCategorias() {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery("FROM CategoriaHabilidad", CategoriaHabilidad.class).getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    @Override
    public Optional<Habilidad> buscarHabilidadPorNombre(String nombre) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery("FROM Habilidad h WHERE lower(h.nombre) = lower(:nombre)", Habilidad.class)
                    .setParameter("nombre", nombre)
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
