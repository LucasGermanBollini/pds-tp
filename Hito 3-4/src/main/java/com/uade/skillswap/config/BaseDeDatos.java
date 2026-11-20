package com.uade.skillswap.config;

import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import org.flywaydb.core.Flyway;

/**
 * Arranque de la persistencia: corre las migraciones Flyway (el esquema lo
 * versiona Flyway, nunca Hibernate: seccion 8) e inicializa la unica
 * EntityManagerFactory (JPAUtil).
 */
public final class BaseDeDatos {

    public static final String MYSQL_URL =
            "jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    public static final String H2_URL = "jdbc:h2:mem:skillswap_test;MODE=MySQL;DB_CLOSE_DELAY=-1";

    private BaseDeDatos() {
    }

    /** MySQL local (requiere scripts/bootstrap-mysql.sql corrido una vez). */
    public static void iniciarMySql() {
        migrar(MYSQL_URL, "skillswap", "skillswap_dev_2024");
        JPAUtil.inicializar("skillswapPU");
    }

    /** H2 en memoria en modo MySQL: mismas migraciones, sin instalar nada. Ideal para una demo rapida. */
    public static void iniciarH2() {
        migrar(H2_URL, "sa", "");
        JPAUtil.inicializar("skillswapTestPU");
    }

    private static void migrar(String url, String usuario, String clave) {
        Flyway.configure()
                .dataSource(url, usuario, clave)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
