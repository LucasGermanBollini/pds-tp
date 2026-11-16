package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.PerfilPublicoDTO;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.PerfilPublicoFacade;
import com.uade.skillswap.application.usecase.PerfilService;
import com.uade.skillswap.domain.model.DisponibilidadSemanal;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.domain.model.TipoPerfil;
import com.uade.skillswap.presentation.controller.comando.CrearPerfilComando;

import java.util.List;

/** Seccion 9: PerfilController.crearPerfil(...) y verPerfil(id), mas la administracion del perfil (4.1). */
public class PerfilController extends ControllerBase {

    private final PerfilService perfilService;
    private final PerfilPublicoFacade perfilPublicoFacade;

    public PerfilController(PerfilService perfilService, PerfilPublicoFacade perfilPublicoFacade,
                            IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.perfilService = perfilService;
        this.perfilPublicoFacade = perfilPublicoFacade;
    }

    public Resultado<PerfilPublicoDTO> crearPerfil(CrearPerfilComando comando) {
        return modificar(comando.userId(), () -> {
            perfilService.crearPerfil(comando.userId(), new PerfilService.DatosPerfil(comando.nombreVisible(),
                    Enums.valor(TipoPerfil.class, comando.tipo(), TipoPerfil.PROFESIONAL), comando.biografia(),
                    comando.zonaHoraria(), comando.rol(),
                    Enums.valor(TipoCanalNotificacion.class, comando.canalPreferido(), TipoCanalNotificacion.INTERNA)));
            return perfilPublicoFacade.verPerfilPublico(comando.userId());
        });
    }

    public Resultado<PerfilPublicoDTO> verPerfil(String userId) {
        return consultar(() -> perfilPublicoFacade.verPerfilPublico(userId));
    }

    public Resultado<PerfilPublicoDTO> actualizarDatos(String actorUserId, String nombreVisible, String biografia,
                                                       String zonaHoraria, String rol) {
        return modificar(actorUserId, () -> {
            perfilService.actualizarDatos(actorUserId, nombreVisible, biografia, zonaHoraria, rol);
            return perfilPublicoFacade.verPerfilPublico(actorUserId);
        });
    }

    public Resultado<PerfilPublicoDTO> agregarHabilidad(String actorUserId, String habilidad) {
        return modificar(actorUserId, () -> {
            perfilService.agregarHabilidad(actorUserId, habilidad);
            return perfilPublicoFacade.verPerfilPublico(actorUserId);
        });
    }

    public Resultado<PerfilPublicoDTO> agregarInteres(String actorUserId, String interes) {
        return modificar(actorUserId, () -> {
            perfilService.agregarInteres(actorUserId, interes);
            return perfilPublicoFacade.verPerfilPublico(actorUserId);
        });
    }

    public Resultado<PerfilPublicoDTO> definirDisponibilidad(String actorUserId, List<DisponibilidadSemanal> rangos) {
        return modificar(actorUserId, () -> {
            perfilService.definirDisponibilidad(actorUserId, rangos);
            return perfilPublicoFacade.verPerfilPublico(actorUserId);
        });
    }

    public Resultado<PerfilPublicoDTO> elegirCanalPreferido(String actorUserId, String canal) {
        return modificar(actorUserId, () -> {
            perfilService.elegirCanalPreferido(actorUserId,
                    Enums.valor(TipoCanalNotificacion.class, canal, TipoCanalNotificacion.INTERNA));
            return perfilPublicoFacade.verPerfilPublico(actorUserId);
        });
    }

    public Resultado<Void> darDeBaja(String actorUserId) {
        return modificarSinValor(actorUserId, () -> perfilService.darDeBaja(actorUserId, actorUserId));
    }
}
