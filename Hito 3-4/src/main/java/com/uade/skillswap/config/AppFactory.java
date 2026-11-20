package com.uade.skillswap.config;

import ar.edu.uade.skillswap.controllers.UiControllers;
import com.uade.skillswap.application.port.CanalNotificacionFactory;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.application.usecase.NotificacionService;
import com.uade.skillswap.application.usecase.PerfilPublicoFacade;
import com.uade.skillswap.application.usecase.PerfilService;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.application.usecase.WalletService;
import com.uade.skillswap.domain.model.PoliticaCancelacionStrategy;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.infrastructure.identidad.IdentidadExternaSdk;
import com.uade.skillswap.infrastructure.identidad.IdentidadServiceAdapter;
import com.uade.skillswap.infrastructure.notification.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notification.NotificadorObserver;
import com.uade.skillswap.presentation.controller.*;
import com.uade.skillswap.presentation.ui.*;

import java.util.List;

/**
 * Composition Root: la UNICA clase que conoce todas las implementaciones
 * concretas (repositorios, adaptadores de identidad y notificacion,
 * observers) y arma el grafo de dependencias a mano, por constructor (no hay
 * Spring Boot, seccion 1). Mismo rol que el Main del ejemplo Adapter de la
 * catedra ("El Composition Root conecta las piezas").
 */
public final class AppFactory {

    /** Todo lo que se arma: los controllers de la seccion 9 y su adaptacion al Mock UI Kit. */
    public record Aplicacion(PerfilController perfiles,
                             PublicacionController publicaciones,
                             BusquedaController busqueda,
                             RecomendacionController recomendaciones,
                             ContratacionController contrataciones,
                             CalificacionController calificaciones,
                             NotificacionController notificaciones,
                             WalletController wallet,
                             UiControllers ui) {
    }

    private AppFactory() {
    }

    public static Aplicacion crear(Repositorios repos, PoliticaCancelacionStrategy politicaCancelacionPorDefecto,
                                   UnitOfWork unitOfWork) {
        return crear(repos, politicaCancelacionPorDefecto, unitOfWork, new CanalNotificacionFactoryImpl());
    }

    public static Aplicacion crear(Repositorios repos, PoliticaCancelacionStrategy politicaCancelacionPorDefecto,
                                   UnitOfWork unitOfWork, CanalNotificacionFactory canalNotificacionFactory) {
        IdentidadProviderPort identidad = new IdentidadServiceAdapter(new IdentidadExternaSdk());

        List<ObservadorDeEstado> observadores = List.of(
                new RegistradorHistorialObserver(repos.historial()),
                new NotificadorObserver(repos.notificaciones(), repos.perfiles(), canalNotificacionFactory));

        PublicacionService publicacionService = new PublicacionService(repos.publicaciones(), repos.catalogo(),
                repos.perfiles(), unitOfWork, observadores);
        ContratacionService contratacionService = new ContratacionService(repos.contrataciones(),
                repos.publicaciones(), repos.wallets(), repos.movimientos(), repos.calificaciones(), repos.perfiles(),
                politicaCancelacionPorDefecto, unitOfWork, observadores);
        PerfilService perfilService = new PerfilService(repos.perfiles(), repos.wallets(), repos.catalogo(),
                repos.publicaciones(), identidad, unitOfWork, observadores);
        PerfilPublicoFacade perfilPublicoFacade = new PerfilPublicoFacade(repos.perfiles(), repos.publicaciones(),
                repos.contrataciones(), repos.calificaciones());
        WalletService walletService = new WalletService(repos.wallets(), repos.movimientos());
        NotificacionService notificacionService = new NotificacionService(repos.notificaciones());

        PerfilController perfilController = new PerfilController(perfilService, perfilPublicoFacade, identidad);
        PublicacionController publicacionController = new PublicacionController(publicacionService, identidad);
        BusquedaController busquedaController = new BusquedaController(publicacionService, identidad);
        RecomendacionController recomendacionController = new RecomendacionController(publicacionService, identidad);
        ContratacionController contratacionController = new ContratacionController(contratacionService, identidad);
        CalificacionController calificacionController = new CalificacionController(contratacionService, identidad);
        NotificacionController notificacionController = new NotificacionController(notificacionService, identidad);
        WalletController walletController = new WalletController(walletService, identidad);

        UiControllers ui = new SkillSwapRealControllers(
                new BusquedaUiController(busquedaController, recomendacionController),
                new PublicacionUiController(publicacionController),
                new ContratacionUiController(contratacionController),
                new WalletUiController(walletController),
                new NotificacionUiController(notificacionController),
                new PerfilUiController(perfilController));

        return new Aplicacion(perfilController, publicacionController, busquedaController, recomendacionController,
                contratacionController, calificacionController, notificacionController, walletController, ui);
    }
}
