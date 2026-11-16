package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base para tests de persistencia/integracion: corre las mismas migraciones
 * Flyway que se usarian contra MySQL, pero sobre H2 en modo MySQL (seccion
 * 12: "pruebas de persistencia con una base compatible"). La base es la
 * misma durante toda la clase (DB_CLOSE_DELAY=-1); por eso cada test limpia
 * las tablas en @AfterEach.
 */
public abstract class JpaTestSupport {

    private static final String JDBC_URL = "jdbc:h2:mem:skillswap_test;MODE=MySQL;DB_CLOSE_DELAY=-1";

    @BeforeAll
    static void migrarEsquema() {
        Flyway.configure()
                .dataSource(JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();
        JPAUtil.inicializar("skillswapTestPU");
        // la V2 inserta datos demo; los tests arman su propio fixture, asi que se limpia antes de empezar.
        limpiarTablas();
    }

    @AfterEach
    void limpiarBaseDeDatos() {
        limpiarTablas();
    }

    private static void limpiarTablas() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            String[] tablas = {
                    "movimientos_creditos", "wallets_skillcoins", "calificaciones", "historial_estados",
                    "contrataciones", "franjas_disponibilidad", "publicacion_habilidades", "publicaciones",
                    "perfil_habilidades", "elementos_catalogo", "notificaciones", "perfiles"
            };
            for (String tabla : tablas) {
                em.createNativeQuery("TRUNCATE TABLE " + tabla).executeUpdate();
            }
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
