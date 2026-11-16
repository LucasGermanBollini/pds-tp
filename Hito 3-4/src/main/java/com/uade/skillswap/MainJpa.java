package com.uade.skillswap;

import com.uade.skillswap.config.AppFactory;
import com.uade.skillswap.config.BaseDeDatos;
import com.uade.skillswap.config.Repositorios;
import com.uade.skillswap.domain.model.PoliticaCancelacionEstandar;
import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaUnitOfWork;

/**
 * El mismo recorrido de Main, pero persistiendo con JPA/Hibernate: contra
 * MySQL real (por defecto; requiere scripts/bootstrap-mysql.sql) o contra H2
 * en memoria con el argumento --h2. El esquema lo crea Flyway en ambos casos.
 */
public class MainJpa {

    public static void main(String[] args) {
        if (args.length > 0 && "--h2".equals(args[0])) {
            BaseDeDatos.iniciarH2();
        } else {
            BaseDeDatos.iniciarMySql();
        }
        try {
            AppFactory.Aplicacion app = AppFactory.crear(Repositorios.jpa(), new PoliticaCancelacionEstandar(),
                    new JpaUnitOfWork());
            RecorridoDemo.ejecutar(app);
        } finally {
            JPAUtil.close();
        }
    }
}
