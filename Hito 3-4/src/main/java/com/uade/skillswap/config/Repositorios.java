package com.uade.skillswap.config;

import com.uade.skillswap.domain.repository.CalificacionRepository;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import com.uade.skillswap.domain.repository.ElementoCatalogoRepository;
import com.uade.skillswap.domain.repository.HistorialEstadoRepository;
import com.uade.skillswap.domain.repository.MovimientoCreditosRepository;
import com.uade.skillswap.domain.repository.NotificacionRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;
import com.uade.skillswap.domain.repository.WalletSkillCoinsRepository;
import com.uade.skillswap.infrastructure.memory.*;
import com.uade.skillswap.infrastructure.persistence.repository.*;

/**
 * Las implementaciones de todos los puertos de persistencia, juntas. Cambiar
 * de "en memoria" a "JPA/MySQL" es cambiar esta unica linea en el arranque:
 * ningun caso de uso se entera (Clean Architecture, seccion 6).
 */
public record Repositorios(PerfilRepository perfiles,
                           PublicacionRepository publicaciones,
                           ContratacionRepository contrataciones,
                           WalletSkillCoinsRepository wallets,
                           MovimientoCreditosRepository movimientos,
                           CalificacionRepository calificaciones,
                           HistorialEstadoRepository historial,
                           NotificacionRepository notificaciones,
                           ElementoCatalogoRepository catalogo) {

    public static Repositorios enMemoria() {
        return new Repositorios(new InMemoryPerfilRepository(), new InMemoryPublicacionRepository(),
                new InMemoryContratacionRepository(), new InMemoryWalletSkillCoinsRepository(),
                new InMemoryMovimientoCreditosRepository(), new InMemoryCalificacionRepository(),
                new InMemoryHistorialEstadoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryElementoCatalogoRepository());
    }

    public static Repositorios jpa() {
        return new Repositorios(new JpaPerfilRepository(), new JpaPublicacionRepository(),
                new JpaContratacionRepository(), new JpaWalletSkillCoinsRepository(),
                new JpaMovimientoCreditosRepository(), new JpaCalificacionRepository(),
                new JpaHistorialEstadoRepository(), new JpaNotificacionRepository(),
                new JpaElementoCatalogoRepository());
    }
}
