package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.domain.model.FranjaDisponibilidad;
import com.uade.skillswap.domain.model.Modalidad;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.SkillCoins;
import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import com.uade.skillswap.infrastructure.persistence.repository.JpaElementoCatalogoRepository;
import com.uade.skillswap.infrastructure.persistence.repository.JpaPerfilRepository;
import com.uade.skillswap.infrastructure.persistence.repository.JpaPublicacionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seccion 8/15: optimistic locking real sobre FranjaDisponibilidad para que
 * dos aceptaciones concurrentes no consuman el mismo ultimo cupo, incluso
 * entre dos transacciones distintas (no solo dentro del mismo proceso Java).
 */
class OptimisticLockingTest extends JpaTestSupport {

    @Test
    void dosTransaccionesQueLeenLaMismaFranjaNoPuedenGuardarAmbasSuCambioDeCupo() {
        JpaPerfilRepository perfilRepository = new JpaPerfilRepository();
        JpaElementoCatalogoRepository catalogoRepository = new JpaElementoCatalogoRepository();
        JpaPublicacionRepository publicacionRepository = new JpaPublicacionRepository();

        Perfil autor = (Perfil) perfilRepository.guardar(new Perfil("prof-ada", "Ada"));
        var categoria = catalogoRepository.guardar(new com.uade.skillswap.domain.model.CategoriaHabilidad("cat-1", "Carrera"));
        Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, "Mentoria");
        FranjaDisponibilidad franja = new FranjaDisponibilidad(UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), 1);
        publicacion.completarDatos("desc", categoria, Modalidad.REMOTA, 60, SkillCoins.de(100));
        publicacion.agregarFranja(franja);
        publicacion.publicar("prof-ada");
        publicacionRepository.guardar(publicacion);
        String franjaId = franja.getId();

        // Dos "transacciones" distintas leen la MISMA fila (misma version).
        EntityManager em1 = JPAUtil.getEntityManager();
        EntityManager em2 = JPAUtil.getEntityManager();
        try {
            em1.getTransaction().begin();
            em2.getTransaction().begin();

            FranjaDisponibilidad vistaDesdeTx1 = em1.find(FranjaDisponibilidad.class, franjaId);
            FranjaDisponibilidad vistaDesdeTx2 = em2.find(FranjaDisponibilidad.class, franjaId);

            assertTrue(vistaDesdeTx1.reservarCupo());
            assertTrue(vistaDesdeTx2.reservarCupo()); // en memoria "funciona", el conflicto es al persistir

            em1.getTransaction().commit(); // esta gana: incrementa la version

            assertThrows(Exception.class, () -> em2.getTransaction().commit(),
                    "la segunda transaccion debe fallar por optimistic locking: la version ya cambio");
        } finally {
            if (em1.isOpen()) em1.close();
            if (em2.isOpen()) em2.close();
        }
    }
}
