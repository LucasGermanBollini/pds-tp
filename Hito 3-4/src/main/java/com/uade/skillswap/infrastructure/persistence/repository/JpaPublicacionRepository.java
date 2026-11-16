package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.repository.PublicacionRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaPublicacionRepository extends AbstractJpaRepository<Publicacion, String> implements PublicacionRepository {

    public JpaPublicacionRepository() {
        super(Publicacion.class);
    }

    /**
     * JOIN FETCH para traer las franjas junto con la publicacion en una sola
     * consulta: evita el N+1 de pedir publicacion.getFranjas() (LAZY) por
     * cada fila al recomendar/buscar (seccion 8).
     */
    public List<Publicacion> buscarPublicadasConFranjas() {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "SELECT DISTINCT p FROM Publicacion p LEFT JOIN FETCH p.franjas WHERE p.estadoNombre = 'PUBLICADA'",
                    Publicacion.class).getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    @Override
    public List<Publicacion> buscarPorAutor(String autorUserId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "SELECT p FROM Publicacion p WHERE p.autor.id = :autorUserId", Publicacion.class)
                    .setParameter("autorUserId", autorUserId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
