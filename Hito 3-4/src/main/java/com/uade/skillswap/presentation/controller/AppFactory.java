package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.controllers.UiControllers;
import com.uade.skillswap.application.port.CanalNotificacionFactory;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.application.usecase.*;
import com.uade.skillswap.domain.model.PoliticaCancelacionStrategy;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.domain.repository.*;
import com.uade.skillswap.infrastructure.identidad.IdentidadExternaSdk;
import com.uade.skillswap.infrastructure.identidad.IdentidadServiceAdapter;
import com.uade.skillswap.infrastructure.notification.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notification.NotificadorObserver;

import java.util.List;

/**
 * Cablea casos de uso + Controllers reales a partir de un set de
 * repositorios (en memoria o JPA) y un UnitOfWork. Mismo rol que
 * "AppFactory" en los ejemplos de la catedra: la unica clase que conoce
 * las implementaciones concretas (incluidas las de Infraestructura, como
 * los adaptadores de notificacion y de identidad) y arma el grafo de
 * dependencias a mano (no hay Spring Boot, seccion 1).
 */
public final class AppFactory {

    private AppFactory() {
    }

    public static UiControllers crearControllers(PerfilRepository perfilRepository,
                                                   PublicacionRepository publicacionRepository,
                                                   ContratacionRepository contratacionRepository,
                                                   WalletSkillCoinsRepository walletSkillCoinsRepository,
                                                   MovimientoCreditosRepository movimientoCreditosRepository,
                                                   CalificacionRepository calificacionRepository,
                                                   HistorialEstadoRepository historialEstadoRepository,
                                                   NotificacionRepository notificacionRepository,
                                                   ElementoCatalogoRepository elementoCatalogoRepository,
                                                   PoliticaCancelacionStrategy politicaCancelacionPorDefecto,
                                                   UnitOfWork unitOfWork) {

        CanalNotificacionFactory canalNotificacionFactory = new CanalNotificacionFactoryImpl();
        IdentidadProviderPort identidadProviderPort = new IdentidadServiceAdapter(new IdentidadExternaSdk());

        List<ObservadorDeEstado> observadores = List.of(
                new RegistradorHistorialObserver(historialEstadoRepository),
                new NotificadorObserver(publicacionRepository, contratacionRepository, notificacionRepository,
                        perfilRepository, canalNotificacionFactory));

        PublicacionService publicacionService = new PublicacionService(publicacionRepository,
                elementoCatalogoRepository, observadores);
        ContratacionService contratacionService = new ContratacionService(contratacionRepository, publicacionRepository,
                walletSkillCoinsRepository, movimientoCreditosRepository, calificacionRepository, perfilRepository,
                politicaCancelacionPorDefecto, unitOfWork, observadores);
        WalletService walletService = new WalletService(walletSkillCoinsRepository, movimientoCreditosRepository);
        NotificacionService notificacionService = new NotificacionService(notificacionRepository);
        PerfilService perfilService = new PerfilService(perfilRepository, contratacionRepository);

        return new SkillSwapRealControllers(
                new BusquedaUiController(publicacionService, perfilRepository),
                new PublicacionUiController(publicacionService, perfilRepository, identidadProviderPort),
                new ContratacionUiController(contratacionService, publicacionService, perfilRepository, identidadProviderPort),
                new WalletUiController(walletService, identidadProviderPort),
                new NotificacionUiController(notificacionService),
                new PerfilUiController(perfilService));
    }
}
