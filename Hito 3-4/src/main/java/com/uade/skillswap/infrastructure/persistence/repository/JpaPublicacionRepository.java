package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.repository.CriteriosBusqueda;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.domain.repository.PublicacionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JpaPublicacionRepository extends AbstractJpaRepository<Publicacion, String> implements PublicacionRepository {

    public JpaPublicacionRepository() {
        super(Publicacion.class);
    }

    /**
     * JOIN FETCH para traer autor, categoria y franjas junto con la
     * publicacion en una sola consulta: evita el N+1 de cargar cada relacion
     * por separado al recomendar/buscar (seccion 8). Las etiquetas (otra
     * coleccion) no pueden ir en el mismo JOIN FETCH (Hibernate no permite
     * dos "bags" a la vez): las trae en lotes hibernate.default_batch_fetch_size
     * (persistence.xml), una consulta por lote en vez de una por fila.
     */
    @Override
    public List<Publicacion> buscarPublicadas() {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "SELECT DISTINCT p FROM Publicacion p JOIN FETCH p.autor LEFT JOIN FETCH p.categoria "
                            + "LEFT JOIN FETCH p.franjas WHERE p.estadoNombre = 'PUBLICADA'",
                    Publicacion.class).getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    /** Nombre historico (Hito 3); mismo comportamiento que buscarPublicadas(). */
    public List<Publicacion> buscarPublicadasConFranjas() {
        return buscarPublicadas();
    }

    @Override
    public List<Publicacion> buscarPorAutor(String autorUserId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery(
                    "SELECT p FROM Publicacion p WHERE p.autor.id = :autorUserId ORDER BY p.fechaCreacion DESC", Publicacion.class)
                    .setParameter("autorUserId", autorUserId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    /**
     * Seccion 4.3 + seccion 8 ("consultas eficientes"). Tres pasos, todo en la base:
     * <ol>
     *   <li>SELECT de ids con todos los filtros, el ORDER BY pedido y LIMIT/OFFSET
     *       (setFirstResult/setMaxResults). Paginar sobre ids evita el problema de
     *       paginar en memoria cuando hay JOIN FETCH de colecciones.</li>
     *   <li>COUNT con los mismos filtros, para el total de la pagina.</li>
     *   <li>Carga de esas publicaciones con JOIN FETCH (sin N+1) y se respeta el
     *       orden del paso 1.</li>
     * </ol>
     * Las reglas (que es "coincidir" y que es "relevante") son las mismas que
     * documenta ReglasDeBusqueda para la version en memoria.
     */
    @Override
    public Pagina<Publicacion> buscar(CriteriosBusqueda c) {
        Map<String, Object> parametros = new HashMap<>();
        String where = construirWhere(c, parametros);

        EntityManager em = emParaLectura();
        try {
            TypedQuery<String> consultaIds = em.createQuery(
                    "SELECT p.id FROM Publicacion p JOIN p.autor a LEFT JOIN p.categoria cat " + where
                            + " ORDER BY " + construirOrden(c, parametros), String.class);
            parametros.forEach(consultaIds::setParameter);
            List<String> ids = consultaIds
                    .setFirstResult(c.pagina() * c.tamanio())
                    .setMaxResults(c.tamanio())
                    .getResultList();

            TypedQuery<Long> consultaTotal = em.createQuery(
                    "SELECT COUNT(p) FROM Publicacion p JOIN p.autor a LEFT JOIN p.categoria cat " + where, Long.class);
            parametros.entrySet().stream()
                    .filter(e -> where.contains(":" + e.getKey()))
                    .forEach(e -> consultaTotal.setParameter(e.getKey(), e.getValue()));
            long total = consultaTotal.getSingleResult();

            if (ids.isEmpty()) {
                return new Pagina<>(List.of(), c.pagina(), c.tamanio(), total);
            }
            Map<String, Publicacion> porId = em.createQuery(
                            "SELECT DISTINCT p FROM Publicacion p JOIN FETCH p.autor LEFT JOIN FETCH p.categoria "
                                    + "LEFT JOIN FETCH p.franjas WHERE p.id IN :ids", Publicacion.class)
                    .setParameter("ids", ids)
                    .getResultList().stream()
                    .collect(Collectors.toMap(Publicacion::getId, Function.identity()));
            List<Publicacion> ordenadas = ids.stream().map(porId::get).toList();
            return new Pagina<>(ordenadas, c.pagina(), c.tamanio(), total);
        } finally {
            cerrarSiEsPropio(em);
        }
    }

    private String construirWhere(CriteriosBusqueda c, Map<String, Object> parametros) {
        StringBuilder where = new StringBuilder("WHERE p.estadoNombre = 'PUBLICADA'");
        if (c.texto() != null) {
            parametros.put("texto", "%" + c.texto().toLowerCase(Locale.ROOT) + "%");
            where.append(" AND (LOWER(p.titulo) LIKE :texto OR LOWER(p.descripcion) LIKE :texto")
                    .append(" OR EXISTS (SELECT 1 FROM Publicacion p2 JOIN p2.etiquetas e WHERE p2 = p AND LOWER(e.nombre) LIKE :texto))");
        }
        if (c.categoria() != null) {
            parametros.put("categoria", c.categoria().toLowerCase(Locale.ROOT));
            where.append(" AND LOWER(cat.nombre) = :categoria");
        }
        if (c.modalidad() != null) {
            parametros.put("modalidad", c.modalidad());
            where.append(" AND p.modalidad = :modalidad");
        }
        if (c.precioMinimo() != null) {
            parametros.put("precioMinimo", c.precioMinimo());
            where.append(" AND p.precioBase.valor >= :precioMinimo");
        }
        if (c.precioMaximo() != null) {
            parametros.put("precioMaximo", c.precioMaximo());
            where.append(" AND p.precioBase.valor <= :precioMaximo");
        }
        if (c.habilidad() != null) {
            parametros.put("habilidad", c.habilidad().toLowerCase(Locale.ROOT));
            where.append(" AND EXISTS (SELECT 1 FROM Publicacion p3 JOIN p3.etiquetas h WHERE p3 = p AND LOWER(h.nombre) = :habilidad)");
        }
        if (c.reputacionMinima() != null) {
            parametros.put("reputacionMinima", c.reputacionMinima());
            where.append(" AND a.reputacionPromedio >= :reputacionMinima");
        }
        if (c.tipo() != null) {
            parametros.put("tipo", c.tipo());
            where.append(" AND p.tipo = :tipo");
        }
        return where.toString();
    }

    private String construirOrden(CriteriosBusqueda c, Map<String, Object> parametros) {
        return switch (c.orden()) {
            case MEJOR_REPUTACION -> "a.reputacionPromedio DESC, p.fechaPublicacion DESC, p.id";
            case MENOR_PRECIO -> "p.precioBase.valor ASC, a.reputacionPromedio DESC, p.id";
            case MAS_RECIENTE -> "p.fechaPublicacion DESC, p.id";
            case RELEVANCIA -> {
                if (c.texto() == null) {
                    yield "p.contadorSolicitudes DESC, a.reputacionPromedio DESC, p.id";
                }
                yield "(CASE WHEN LOWER(p.titulo) LIKE :texto THEN 3 ELSE 0 END"
                        + " + CASE WHEN EXISTS (SELECT 1 FROM Publicacion p4 JOIN p4.etiquetas e4 WHERE p4 = p AND LOWER(e4.nombre) LIKE :texto) THEN 2 ELSE 0 END"
                        + " + CASE WHEN LOWER(p.descripcion) LIKE :texto THEN 1 ELSE 0 END) DESC,"
                        + " p.contadorSolicitudes DESC, a.reputacionPromedio DESC, p.id";
            }
        };
    }
}
