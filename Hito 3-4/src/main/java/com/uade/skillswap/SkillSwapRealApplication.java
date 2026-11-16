package com.uade.skillswap;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.ui.SkillSwapView;
import ar.edu.uade.skillswap.ui.Theme;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.repository.*;
import com.uade.skillswap.presentation.controller.AppFactory;
import org.flywaydb.core.Flyway;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mismo SkillSwapView del Mock UI Kit (no se rediseñan pantallas, seccion
 * 10), pero conectado a los Controllers reales (presentation.controller) en
 * vez de MockSkillSwapControllers. Requiere scripts/bootstrap-mysql.sql
 * corrido una vez.
 */
public final class SkillSwapRealApplication {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USUARIO_DEMO = "user-mateo";

    private SkillSwapRealApplication() {
    }

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

        sembrarDatosDemoSiHaceFalta(perfilRepository, publicacionRepository, walletRepository, catalogoRepository);

        UiControllers controllers = AppFactory.crearControllers(perfilRepository, publicacionRepository,
                contratacionRepository, walletRepository, movimientoRepository, calificacionRepository,
                historialRepository, notificacionRepository, catalogoRepository,
                new PoliticaCancelacionEstandar(), new JpaUnitOfWork());

        Theme.install();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SkillSwap");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1120, 700));
            frame.setSize(1365, 820);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new SkillSwapView(controllers));
            frame.setVisible(true);
        });
    }

    /** Usuario "user-mateo" (misma persona que el Mock) para poder loguearse y ver algo la primera vez. */
    private static void sembrarDatosDemoSiHaceFalta(JpaPerfilRepository perfilRepository,
                                                      JpaPublicacionRepository publicacionRepository,
                                                      JpaWalletSkillCoinsRepository walletRepository,
                                                      JpaElementoCatalogoRepository catalogoRepository) {
        if (perfilRepository.buscarPorId(USUARIO_DEMO).isPresent()) {
            return;
        }
        Perfil mateo = new Perfil(USUARIO_DEMO, "Mateo Silva");
        mateo.actualizarDatos("Mateo Silva", "Construyo productos digitales y acompaño a personas que quieren crecer en tecnologia.",
                "America/Argentina/Buenos_Aires", "Software Engineer & Mentor");
        perfilRepository.guardar(mateo);
        walletRepository.guardar(new WalletSkillCoins(java.util.UUID.randomUUID().toString(), USUARIO_DEMO, SkillCoins.de(2480)));

        Perfil sofia = (Perfil) perfilRepository.guardar(new Perfil("prof-sofia", "Sofia Mendez"));
        walletRepository.guardar(new WalletSkillCoins(java.util.UUID.randomUUID().toString(), "prof-sofia", SkillCoins.CERO));
        CategoriaHabilidad tecnologia = (CategoriaHabilidad) catalogoRepository.guardar(
                new CategoriaHabilidad(java.util.UUID.randomUUID().toString(), "Tecnologia"));

        Publicacion publicacion = new Publicacion(java.util.UUID.randomUUID().toString(), sofia,
                "Preparacion para entrevistas tecnicas");
        FranjaDisponibilidad franja = new FranjaDisponibilidad(java.util.UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1), 5);
        publicacion.completarDatos("Simulacro completo, feedback y plan personalizado para tu proxima entrevista.",
                tecnologia, Modalidad.REMOTA, 60, SkillCoins.de(180));
        publicacion.agregarFranja(franja);
        publicacion.publicar("prof-sofia");
        publicacionRepository.guardar(publicacion);
    }
}
