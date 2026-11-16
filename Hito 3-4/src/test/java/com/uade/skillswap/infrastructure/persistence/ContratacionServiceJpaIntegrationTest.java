package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.infrastructure.notification.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notification.NotificadorObserver;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repite el flujo principal del Hito 2 (publicar -> solicitar -> aceptar ->
 * pagar -> completar -> calificar) pero contra JPA/H2 real en vez de
 * repositorios en memoria. PublicacionService y ContratacionService no
 * cambiaron una linea: esa es la prueba de que Clean Architecture cumplio
 * su promesa (seccion 6).
 */
class ContratacionServiceJpaIntegrationTest extends JpaTestSupport {

    @Test
    void flujoCompletoContraJpaReal() {
        JpaPerfilRepository perfilRepository = new JpaPerfilRepository();
        JpaPublicacionRepository publicacionRepository = new JpaPublicacionRepository();
        JpaContratacionRepository contratacionRepository = new JpaContratacionRepository();
        JpaWalletSkillCoinsRepository walletRepository = new JpaWalletSkillCoinsRepository();
        JpaMovimientoCreditosRepository movimientoRepository = new JpaMovimientoCreditosRepository();
        JpaCalificacionRepository calificacionRepository = new JpaCalificacionRepository();
        JpaHistorialEstadoRepository historialRepository = new JpaHistorialEstadoRepository();
        JpaNotificacionRepository notificacionRepository = new JpaNotificacionRepository();
        JpaElementoCatalogoRepository catalogoRepository = new JpaElementoCatalogoRepository();
        JpaUnitOfWork unitOfWork = new JpaUnitOfWork();
        List<ObservadorDeEstado> observadores = List.of(
                new RegistradorHistorialObserver(historialRepository),
                new NotificadorObserver(publicacionRepository, contratacionRepository, notificacionRepository,
                        perfilRepository, new CanalNotificacionFactoryImpl()));

        PublicacionService publicacionService = new PublicacionService(publicacionRepository, catalogoRepository, observadores);
        ContratacionService contratacionService = new ContratacionService(contratacionRepository, publicacionRepository,
                walletRepository, movimientoRepository, calificacionRepository, perfilRepository,
                new PoliticaCancelacionEstandar(), unitOfWork, observadores);

        Perfil ada = (Perfil) perfilRepository.guardar(new Perfil("prof-ada", "Ada"));
        Perfil beto = (Perfil) perfilRepository.guardar(new Perfil("cli-beto", "Beto"));
        walletRepository.guardar(new WalletSkillCoins("wallet-ada", "prof-ada", SkillCoins.CERO));
        walletRepository.guardar(new WalletSkillCoins("wallet-beto", "cli-beto", SkillCoins.de(1000)));
        CategoriaHabilidad categoria = (CategoriaHabilidad) catalogoRepository.guardar(new CategoriaHabilidad("cat-1", "Carrera"));

        Publicacion publicacion = publicacionService.crearBorrador(ada, "Mentoria de entrevistas");
        FranjaDisponibilidad franja = new FranjaDisponibilidad(java.util.UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), 1);
        publicacionService.completarDatos(publicacion.getId(), "desc", categoria, Modalidad.REMOTA, 60,
                SkillCoins.de(300), List.of(), List.of(franja));
        publicacion = publicacionService.publicar(publicacion.getId(), "prof-ada");
        FranjaDisponibilidad franjaPersistida = publicacion.getFranjas().get(0);

        Contratacion contratacion = contratacionService.solicitar(beto, publicacion, franjaPersistida, "hola", "practicar");
        contratacionService.aceptar(contratacion.getId(), "prof-ada");
        contratacionService.pagar(contratacion.getId(), "cli-beto", new PrecioPublicacion(publicacion.getPrecioBase()));
        contratacionService.iniciar(contratacion.getId(), "prof-ada");
        contratacionService.completar(contratacion.getId(), "prof-ada");
        contratacionService.calificar(contratacion.getId(), "cli-beto", "prof-ada", 5, "Excelente");

        Contratacion recargada = contratacionRepository.buscarPorId(contratacion.getId()).orElseThrow();
        assertEquals("COMPLETADA", recargada.getEstadoNombre());
        assertEquals(SkillCoins.de(700), walletRepository.buscarPorTitular("cli-beto").orElseThrow().getSaldo());
        assertEquals(SkillCoins.de(300), walletRepository.buscarPorTitular("prof-ada").orElseThrow().getSaldo());
        assertEquals(5.0, perfilRepository.buscarPorId("prof-ada").orElseThrow().getReputacionPromedio());
        assertEquals(4, historialRepository.buscarPorEntidad("CONTRATACION", contratacion.getId()).size());
    }

    @Test
    void unPagoConSaldoInsuficienteNoModificaNadaEnLaBaseReal() {
        JpaPerfilRepository perfilRepository = new JpaPerfilRepository();
        JpaPublicacionRepository publicacionRepository = new JpaPublicacionRepository();
        JpaContratacionRepository contratacionRepository = new JpaContratacionRepository();
        JpaWalletSkillCoinsRepository walletRepository = new JpaWalletSkillCoinsRepository();
        JpaMovimientoCreditosRepository movimientoRepository = new JpaMovimientoCreditosRepository();
        JpaCalificacionRepository calificacionRepository = new JpaCalificacionRepository();
        JpaHistorialEstadoRepository historialRepository = new JpaHistorialEstadoRepository();
        JpaNotificacionRepository notificacionRepository = new JpaNotificacionRepository();
        JpaElementoCatalogoRepository catalogoRepository = new JpaElementoCatalogoRepository();
        JpaUnitOfWork unitOfWork = new JpaUnitOfWork();
        List<ObservadorDeEstado> observadores = List.of(
                new RegistradorHistorialObserver(historialRepository),
                new NotificadorObserver(publicacionRepository, contratacionRepository, notificacionRepository,
                        perfilRepository, new CanalNotificacionFactoryImpl()));

        PublicacionService publicacionService = new PublicacionService(publicacionRepository, catalogoRepository, observadores);
        ContratacionService contratacionService = new ContratacionService(contratacionRepository, publicacionRepository,
                walletRepository, movimientoRepository, calificacionRepository, perfilRepository,
                new PoliticaCancelacionEstandar(), unitOfWork, observadores);

        Perfil ada = (Perfil) perfilRepository.guardar(new Perfil("prof-ada", "Ada"));
        Perfil beto = (Perfil) perfilRepository.guardar(new Perfil("cli-beto", "Beto"));
        walletRepository.guardar(new WalletSkillCoins("wallet-ada", "prof-ada", SkillCoins.CERO));
        walletRepository.guardar(new WalletSkillCoins("wallet-beto", "cli-beto", SkillCoins.de(50)));
        CategoriaHabilidad categoria = (CategoriaHabilidad) catalogoRepository.guardar(new CategoriaHabilidad("cat-1", "Carrera"));

        Publicacion publicacion = publicacionService.crearBorrador(ada, "Mentoria de entrevistas");
        FranjaDisponibilidad franja = new FranjaDisponibilidad(java.util.UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), 1);
        publicacionService.completarDatos(publicacion.getId(), "desc", categoria, Modalidad.REMOTA, 60,
                SkillCoins.de(300), List.of(), List.of(franja));
        Publicacion publicada = publicacionService.publicar(publicacion.getId(), "prof-ada");
        FranjaDisponibilidad franjaPersistida = publicada.getFranjas().get(0);

        Contratacion contratacion = contratacionService.solicitar(beto, publicada, franjaPersistida, "hola", "practicar");
        contratacionService.aceptar(contratacion.getId(), "prof-ada");

        assertThrows(IllegalStateException.class, () -> contratacionService.pagar(
                contratacion.getId(), "cli-beto", new PrecioPublicacion(publicada.getPrecioBase())));

        assertEquals("ACEPTADA", contratacionRepository.buscarPorId(contratacion.getId()).orElseThrow().getEstadoNombre());
        assertEquals(SkillCoins.de(50), walletRepository.buscarPorTitular("cli-beto").orElseThrow().getSaldo());
        assertEquals(SkillCoins.de(0), walletRepository.buscarPorTitular("prof-ada").orElseThrow().getSaldo());
    }
}
