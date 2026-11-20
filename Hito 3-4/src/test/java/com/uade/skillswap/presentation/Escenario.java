package com.uade.skillswap.presentation;

import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.config.AppFactory;
import com.uade.skillswap.config.Repositorios;
import com.uade.skillswap.domain.model.PoliticaCancelacionEstandar;
import com.uade.skillswap.presentation.controller.comando.CrearPerfilComando;
import com.uade.skillswap.presentation.controller.comando.CrearPublicacionComando;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Arma la aplicacion completa (AppFactory) sobre repositorios en memoria, para tests de integracion de controllers. */
public class Escenario {

    public final Repositorios repos;
    public final CanalesEspia canales = new CanalesEspia();
    public final AppFactory.Aplicacion app;
    public final LocalDateTime base = LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.HOURS);

    public Escenario() {
        this(Repositorios.enMemoria(), UnitOfWork.DIRECTO);
    }

    public Escenario(Repositorios repos, UnitOfWork unitOfWork) {
        this.repos = repos;
        this.app = AppFactory.crear(repos, new PoliticaCancelacionEstandar(), unitOfWork, canales);
    }

    public void perfil(String id, String nombre, String tipo, String canal, long saldo) {
        app.perfiles().crearPerfil(new CrearPerfilComando(id, nombre, tipo, "bio", "UTC", "rol", canal)).valorOLanzar();
        if (saldo > 0) {
            app.wallet().acreditarDemo(id, saldo).valorOLanzar();
        }
    }

    public CrearPublicacionComando.Franja franja(int diasDesdeBase, int cupo) {
        LocalDateTime inicio = base.plusDays(diasDesdeBase);
        return new CrearPublicacionComando.Franja(inicio, inicio.plusHours(1), cupo);
    }

    public PublicacionDTO publicar(String autor, String titulo, String categoria, String modalidad, long precio,
                                   List<String> etiquetas, List<CrearPublicacionComando.Franja> franjas) {
        PublicacionDTO borrador = app.publicaciones().crearBorrador(autor, new CrearPublicacionComando(titulo,
                "Descripcion de " + titulo, categoria, modalidad, 60, precio, etiquetas, franjas, null)).valorOLanzar();
        return app.publicaciones().cambiarEstado(autor, borrador.id(), "PUBLICADA", null).valorOLanzar();
    }
}
