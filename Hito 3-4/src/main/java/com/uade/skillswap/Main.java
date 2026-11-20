package com.uade.skillswap;

import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.config.AppFactory;
import com.uade.skillswap.config.Repositorios;
import com.uade.skillswap.domain.model.PoliticaCancelacionEstandar;

/**
 * Demo de consola con repositorios en memoria: el recorrido principal de la
 * seccion 13 sin necesitar ninguna base de datos.
 */
public class Main {

    public static void main(String[] args) {
        AppFactory.Aplicacion app = AppFactory.crear(Repositorios.enMemoria(), new PoliticaCancelacionEstandar(),
                UnitOfWork.DIRECTO);
        RecorridoDemo.ejecutar(app);
    }
}
