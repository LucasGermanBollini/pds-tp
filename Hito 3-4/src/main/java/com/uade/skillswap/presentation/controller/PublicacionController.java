package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.Modalidad;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.TipoPublicacion;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.comando.CrearPublicacionComando;

import java.util.List;
import java.util.Locale;

/** Seccion 9: PublicacionController.crearBorrador(...) y cambiarEstado(...). */
public class PublicacionController extends ControllerBase {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.publicacionService = publicacionService;
    }

    public Resultado<PublicacionDTO> crearBorrador(String actorUserId, CrearPublicacionComando c) {
        return modificar(actorUserId, () -> {
            List<PublicacionService.NuevaFranja> franjas = c.franjas() == null ? List.of() : c.franjas().stream()
                    .map(f -> new PublicacionService.NuevaFranja(f.inicio(), f.fin(), f.cupo())).toList();
            Publicacion borrador = publicacionService.crearBorrador(actorUserId, new PublicacionService.DatosPublicacion(
                    c.titulo(), c.descripcion(), c.categoria(), Enums.valor(Modalidad.class, c.modalidad(), null),
                    c.duracionMinutos(), c.precio(), c.etiquetas(), franjas,
                    Enums.valor(TipoPublicacion.class, c.tipo(), null)));
            return DtoMapper.publicacion(borrador);
        });
    }

    public Resultado<PublicacionDTO> agregarFranja(String actorUserId, String publicacionId,
                                                   CrearPublicacionComando.Franja franja) {
        return modificar(actorUserId, () -> DtoMapper.publicacion(publicacionService.agregarFranja(publicacionId,
                actorUserId, new PublicacionService.NuevaFranja(franja.inicio(), franja.fin(), franja.cupo()))));
    }

    /**
     * Ejecuta una transicion de estado (seccion 4.2). El destino llega como
     * texto; cual es valida desde el estado actual lo decide el State.
     */
    public Resultado<PublicacionDTO> cambiarEstado(String actorUserId, String publicacionId, String estadoDestino,
                                                   String motivo) {
        return modificar(actorUserId, () -> {
            String destino = estadoDestino == null ? "" : estadoDestino.trim().toUpperCase(Locale.ROOT);
            Publicacion resultado = switch (destino) {
                case "PUBLICADA" -> publicacionService.publicar(publicacionId, actorUserId);
                case "PAUSADA" -> publicacionService.pausar(publicacionId, actorUserId);
                case "CERRADA" -> publicacionService.cerrar(publicacionId, actorUserId);
                case "CANCELADA" -> publicacionService.cancelar(publicacionId, actorUserId, motivo);
                default -> throw new IllegalArgumentException("Estado destino invalido: " + estadoDestino);
            };
            return DtoMapper.publicacion(resultado);
        });
    }

    public Resultado<Pagina<PublicacionDTO>> listarPropias(String userId, int pagina, int tamanio) {
        return consultar(() -> Pagina.de(publicacionService.listarPropias(userId), pagina, tamanio)
                .map(DtoMapper::publicacion));
    }

    public Resultado<PublicacionDTO> verDetalle(String publicacionId) {
        return consultar(() -> DtoMapper.publicacion(publicacionService.buscarPorId(publicacionId)));
    }
}
