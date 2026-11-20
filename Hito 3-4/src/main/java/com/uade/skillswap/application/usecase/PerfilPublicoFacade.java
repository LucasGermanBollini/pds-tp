package com.uade.skillswap.application.usecase;

import com.uade.skillswap.application.dto.CalificacionDTO;
import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.dto.PerfilPublicoDTO;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.domain.model.Calificacion;
import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.DisponibilidadSemanal;
import com.uade.skillswap.domain.model.Habilidad;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.repository.CalificacionRepository;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.util.Comparator;
import java.util.List;

/**
 * Facade Pattern (seccion 7: "Vista resumida del perfil"). Armar el perfil
 * publico de la seccion 4.1 requiere consultar cuatro "subsistemas"
 * (perfiles, publicaciones, contrataciones y calificaciones) y combinar sus
 * resultados. La fachada ofrece UNA operacion simple, verPerfilPublico(id),
 * y esconde esa coordinacion: el controller (y la interfaz) no conocen
 * ninguno de los cuatro repositorios.
 */
public class PerfilPublicoFacade {

    private static final int CANTIDAD_CALIFICACIONES_RECIENTES = 5;

    private final PerfilRepository perfilRepository;
    private final PublicacionRepository publicacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final CalificacionRepository calificacionRepository;

    public PerfilPublicoFacade(PerfilRepository perfilRepository, PublicacionRepository publicacionRepository,
                                ContratacionRepository contratacionRepository,
                                CalificacionRepository calificacionRepository) {
        this.perfilRepository = perfilRepository;
        this.publicacionRepository = publicacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.calificacionRepository = calificacionRepository;
    }

    public PerfilPublicoDTO verPerfilPublico(String userId) {
        Perfil perfil = perfilRepository.buscarPorId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + userId));

        List<PublicacionDTO> activas = publicacionRepository.buscarPorAutor(userId).stream()
                .filter(Publicacion::estaPublicada)
                .map(DtoMapper::publicacion)
                .toList();

        List<Contratacion> contrataciones = contratacionRepository.buscarPorParticipante(userId);
        int comoProveedor = (int) contrataciones.stream()
                .filter(c -> c.esProveedor(userId) && "COMPLETADA".equals(c.getEstadoNombre())).count();
        int comoCliente = (int) contrataciones.stream()
                .filter(c -> c.esCliente(userId) && "COMPLETADA".equals(c.getEstadoNombre())).count();
        int canceladas = (int) contrataciones.stream()
                .filter(c -> "CANCELADA".equals(c.getEstadoNombre())).count();

        List<CalificacionDTO> ultimas = calificacionRepository.buscarPorReceptor(userId).stream()
                .sorted(Comparator.comparing(Calificacion::getFecha).reversed())
                .limit(CANTIDAD_CALIFICACIONES_RECIENTES)
                .map(DtoMapper::calificacion)
                .toList();

        return new PerfilPublicoDTO(
                perfil.obtenerId(),
                perfil.obtenerNombreVisible(),
                perfil.getTipo().name(),
                nvl(perfil.getRolDescripcion()),
                nvl(perfil.getBiografia()),
                nvl(perfil.getZonaHoraria()),
                perfil.getCanalPreferido().name(),
                perfil.isActivo(),
                perfil.getHabilidades().stream().map(Habilidad::obtenerNombre).toList(),
                perfil.getIntereses().stream().map(Habilidad::obtenerNombre).toList(),
                perfil.getDisponibilidad().stream().map(DisponibilidadSemanal::toString).toList(),
                perfil.getReputacionPromedio(),
                perfil.getCantidadCalificaciones(),
                activas,
                new PerfilPublicoDTO.HistorialResumido(comoProveedor, comoCliente, canceladas, ultimas));
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
