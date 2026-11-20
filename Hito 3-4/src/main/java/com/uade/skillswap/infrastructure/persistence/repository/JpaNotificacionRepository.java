package com.uade.skillswap.infrastructure.persistence.repository;

import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.repository.NotificacionRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaNotificacionRepository extends AbstractJpaRepository<Notificacion, String> implements NotificacionRepository {

    public JpaNotificacionRepository() {
        super(Notificacion.class);
    }

    @Override
    public List<Notificacion> buscarPorDestinatario(String destinatarioUserId) {
        EntityManager em = emParaLectura();
        try {
            return em.createQuery("FROM Notificacion n WHERE n.destinatarioUserId = :destinatarioUserId", Notificacion.class)
                    .setParameter("destinatarioUserId", destinatarioUserId)
                    .getResultList();
        } finally {
            cerrarSiEsPropio(em);
        }
    }
}
