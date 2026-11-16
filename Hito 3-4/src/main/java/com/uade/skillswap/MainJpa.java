package com.uade.skillswap;

import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.infrastructure.notification.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notification.NotificadorObserver;
import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.repository.*;
import org.flywaydb.core.Flyway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Misma demo que Main.java (Hito 2) pero contra MySQL real via JPA/Hibernate,
 * con el esquema versionado por Flyway (seccion 8). Requiere haber corrido
 * scripts/bootstrap-mysql.sql una vez contra el MySQL local.
 */
public class MainJpa {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";

    public static void main(String[] args) {
        Flyway.configure()
                .dataSource(JDBC_URL, "skillswap", "skillswap_dev_2024")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        JPAUtil.inicializar("skillswapPU");

        JpaPerfilRepository perfilRepository = new JpaPerfilRepository();
        JpaPublicacionRepository publicacionRepository = new JpaPublicacionRepository();
        JpaContratacionRepository contratacionRepository = new JpaContratacionRepository();
        JpaWalletSkillCoinsRepository walletRepository = new JpaWalletSkillCoinsRepository();
        JpaMovimientoCreditosRepository movimientoRepository = new JpaMovimientoCreditosRepository();
        JpaCalificacionRepository calificacionRepository = new JpaCalificacionRepository();
        JpaHistorialEstadoRepository historialRepository = new JpaHistorialEstadoRepository();
        JpaNotificacionRepository notificacionRepository = new JpaNotificacionRepository();
        JpaElementoCatalogoRepository catalogoRepository = new JpaElementoCatalogoRepository();

        List<ObservadorDeEstado> observadores = List.of(
                new RegistradorHistorialObserver(historialRepository),
                new NotificadorObserver(publicacionRepository, contratacionRepository, notificacionRepository,
                        perfilRepository, new CanalNotificacionFactoryImpl()));

        PublicacionService publicacionService = new PublicacionService(publicacionRepository, catalogoRepository, observadores);
        ContratacionService contratacionService = new ContratacionService(contratacionRepository, publicacionRepository,
                walletRepository, movimientoRepository, calificacionRepository, perfilRepository,
                new PoliticaCancelacionEstandar(), new JpaUnitOfWork(), observadores);

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Perfil ada = (Perfil) perfilRepository.guardar(new Perfil("prof-ada-" + sufijo, "Ada, mentora de carrera"));
        Perfil beto = (Perfil) perfilRepository.guardar(new Perfil("cli-beto-" + sufijo, "Beto"));
        walletRepository.guardar(new WalletSkillCoins("wallet-ada-" + sufijo, ada.obtenerId(), SkillCoins.CERO));
        walletRepository.guardar(new WalletSkillCoins("wallet-beto-" + sufijo, beto.obtenerId(), SkillCoins.de(1000)));
        CategoriaHabilidad categoria = (CategoriaHabilidad) catalogoRepository.guardar(new CategoriaHabilidad("cat-" + sufijo, "Carrera"));

        Publicacion publicacion = publicacionService.crearBorrador(ada, "Preparacion para entrevistas tecnicas");
        System.out.println("1) BORRADOR creado (MySQL real) -> " + publicacion.getEstadoNombre());

        FranjaDisponibilidad franja = new FranjaDisponibilidad(UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), 1);
        publicacionService.completarDatos(publicacion.getId(), "Simulacro de entrevista + feedback", categoria,
                Modalidad.REMOTA, 60, SkillCoins.de(300), List.of(), List.of(franja));
        Publicacion publicada = publicacionService.publicar(publicacion.getId(), ada.obtenerId());
        System.out.println("2) PUBLICADA -> " + publicada.getEstadoNombre());

        FranjaDisponibilidad franjaPersistida = publicada.getFranjas().get(0);
        Contratacion contratacion = contratacionService.solicitar(beto, publicada, franjaPersistida,
                "Hola Ada, quiero practicar para una entrevista", "Conseguir trabajo de dev junior");
        System.out.println("3) Contratacion PENDIENTE -> " + contratacion.getEstadoNombre());

        contratacionService.aceptar(contratacion.getId(), ada.obtenerId());
        System.out.println("4) ACEPTADA (cupo bloqueado con optimistic locking) -> cupo disponible: "
                + franjaRepositorioDisponible(contratacionRepository, contratacion.getId()));

        contratacionService.pagar(contratacion.getId(), beto.obtenerId(), new PrecioPublicacion(publicada.getPrecioBase()));
        System.out.println("5) CONFIRMADA (pago con SkillCoins, una sola transaccion) -> saldo Beto: "
                + walletRepository.buscarPorTitular(beto.obtenerId()).orElseThrow().getSaldo()
                + " | saldo Ada: " + walletRepository.buscarPorTitular(ada.obtenerId()).orElseThrow().getSaldo());

        contratacionService.iniciar(contratacion.getId(), ada.obtenerId());
        contratacionService.completar(contratacion.getId(), ada.obtenerId());
        contratacionService.calificar(contratacion.getId(), beto.obtenerId(), ada.obtenerId(), 5, "Excelente mentoria");
        System.out.println("6) COMPLETADA y calificada -> reputacion de Ada: "
                + perfilRepository.buscarPorId(ada.obtenerId()).orElseThrow().getReputacionPromedio());

        System.out.println();
        System.out.println("Historial persistido en MySQL:");
        historialRepository.buscarPorEntidad("CONTRATACION", contratacion.getId())
                .forEach(h -> System.out.println("  " + h.getEstadoAnterior() + " -> " + h.getEstadoNuevo() + " (actor " + h.getActorUserId() + ")"));

        JPAUtil.close();
    }

    private static int franjaRepositorioDisponible(JpaContratacionRepository repo, String contratacionId) {
        return repo.buscarPorId(contratacionId).orElseThrow().getFranjaElegida().getCupoDisponible();
    }
}
