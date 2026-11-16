package com.uade.skillswap;

import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.infrastructure.memory.*;
import com.uade.skillswap.infrastructure.notification.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notification.NotificadorObserver;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo de consola del Hito 2: recorre "publicar -> solicitar -> aceptar ->
 * pagar -> completar -> calificar" sobre el dominio puro, con repositorios en
 * memoria. La interfaz Swing/JavaFX real y los Controllers llegan en el Hito 4.
 */
public class Main {

    public static void main(String[] args) {
        InMemoryPerfilRepository perfilRepository = new InMemoryPerfilRepository();
        InMemoryPublicacionRepository publicacionRepository = new InMemoryPublicacionRepository();
        InMemoryContratacionRepository contratacionRepository = new InMemoryContratacionRepository();
        InMemoryWalletSkillCoinsRepository walletRepository = new InMemoryWalletSkillCoinsRepository();
        InMemoryMovimientoCreditosRepository movimientoRepository = new InMemoryMovimientoCreditosRepository();
        InMemoryCalificacionRepository calificacionRepository = new InMemoryCalificacionRepository();
        InMemoryHistorialEstadoRepository historialRepository = new InMemoryHistorialEstadoRepository();
        InMemoryNotificacionRepository notificacionRepository = new InMemoryNotificacionRepository();
        InMemoryElementoCatalogoRepository catalogoRepository = new InMemoryElementoCatalogoRepository();

        List<ObservadorDeEstado> observadores = List.of(
                new RegistradorHistorialObserver(historialRepository),
                new NotificadorObserver(publicacionRepository, contratacionRepository, notificacionRepository,
                        perfilRepository, new CanalNotificacionFactoryImpl()));

        PublicacionService publicacionService = new PublicacionService(publicacionRepository, catalogoRepository, observadores);
        ContratacionService contratacionService = new ContratacionService(contratacionRepository, publicacionRepository,
                walletRepository, movimientoRepository, calificacionRepository, perfilRepository,
                new PoliticaCancelacionEstandar(), new NoOpUnitOfWork(), observadores);

        Perfil ada = new Perfil("prof-ada", "Ada, mentora de carrera");
        Perfil beto = new Perfil("cli-beto", "Beto, busca mentoria");
        perfilRepository.guardar(ada);
        perfilRepository.guardar(beto);
        walletRepository.guardar(new WalletSkillCoins("wallet-ada", "prof-ada", SkillCoins.CERO));
        walletRepository.guardar(new WalletSkillCoins("wallet-beto", "cli-beto", SkillCoins.de(1000)));

        Publicacion publicacion = publicacionService.crearBorrador(ada, "Preparacion para entrevistas tecnicas");
        System.out.println("1) BORRADOR creado -> " + publicacion.getEstadoNombre());

        FranjaDisponibilidad franja = new FranjaDisponibilidad("franja-1", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1), 1);
        publicacionService.completarDatos(publicacion.getId(), "Simulacro de entrevista + feedback",
                new CategoriaHabilidad("cat-carrera", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(300),
                List.of(new Habilidad("h-java", "Java")), List.of(franja));
        publicacionService.publicar(publicacion.getId(), "prof-ada");
        System.out.println("2) PUBLICADA -> " + publicacion.getEstadoNombre());

        Contratacion contratacion = contratacionService.solicitar(beto, publicacion, franja,
                "Hola Ada, me gustaria practicar para una entrevista de backend", "Conseguir trabajo de dev junior");
        System.out.println("3) Contratacion PENDIENTE -> " + contratacion.getEstadoNombre());

        contratacionService.aceptar(contratacion.getId(), "prof-ada");
        System.out.println("4) ACEPTADA (cupo bloqueado) -> " + contratacion.getEstadoNombre()
                + " | cupo disponible: " + franja.getCupoDisponible());

        contratacionService.pagar(contratacion.getId(), "cli-beto", new PrecioPublicacion(publicacion.getPrecioBase()));
        System.out.println("5) CONFIRMADA (pago con SkillCoins) -> " + contratacion.getEstadoNombre()
                + " | saldo Beto: " + walletRepository.buscarPorTitular("cli-beto").orElseThrow().getSaldo()
                + " | saldo Ada: " + walletRepository.buscarPorTitular("prof-ada").orElseThrow().getSaldo());

        contratacionService.iniciar(contratacion.getId(), "prof-ada");
        contratacionService.completar(contratacion.getId(), "prof-ada");
        System.out.println("6) COMPLETADA -> " + contratacion.getEstadoNombre());

        contratacionService.calificar(contratacion.getId(), "cli-beto", "prof-ada", 5, "Excelente mentoria");
        System.out.println("7) Calificada -> reputacion de Ada: " + ada.getReputacionPromedio());

        System.out.println();
        System.out.println("Historial de la contratacion:");
        historialRepository.buscarPorEntidad("CONTRATACION", contratacion.getId())
                .forEach(h -> System.out.println("  " + h.getEstadoAnterior() + " -> " + h.getEstadoNuevo() + " (actor " + h.getActorUserId() + ")"));

        System.out.println();
        System.out.println("Notificaciones generadas:");
        List<String> destinatarios = List.of("prof-ada", "cli-beto");
        destinatarios.forEach(userId -> notificacionRepository.buscarPorDestinatario(userId)
                .forEach(n -> System.out.println("  [" + userId + "] " + n.getMensaje())));
    }
}
