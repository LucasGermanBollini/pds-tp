package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.Calificacion;
import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;
import com.uade.skillswap.domain.model.Habilidad;
import com.uade.skillswap.domain.model.MovimientoCreditos;
import com.uade.skillswap.domain.model.Notificacion;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.TipoMovimientoCreditos;

import java.util.Comparator;

/**
 * Traduce entidades del dominio a DTOs planos. Es el borde: lo que sale de
 * aca hacia los controllers y la interfaz nunca es una entidad JPA (seccion
 * 10: "La UI conocera interfaces de controllers, nunca repositorios ni
 * entidades JPA").
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static FranjaDTO franja(FranjaDisponibilidad f) {
        if (f == null) {
            return null;
        }
        return new FranjaDTO(f.getId(), f.getFechaHoraInicio(), f.getFechaHoraFin(), f.getCupoTotal(), f.getCupoDisponible());
    }

    public static PublicacionDTO publicacion(Publicacion p) {
        return new PublicacionDTO(
                p.getId(),
                p.getTipo().name(),
                p.getTitulo(),
                nvl(p.getDescripcion()),
                p.getCategoria() == null ? "" : p.getCategoria().obtenerNombre(),
                p.getModalidad() == null ? null : p.getModalidad().name(),
                p.getDuracionEstimadaMinutos(),
                p.getPrecioBase() == null ? 0 : p.getPrecioBase().valor(),
                p.getEtiquetas().stream().map(Habilidad::obtenerNombre).toList(),
                p.getEstadoNombre(),
                p.getAutor().obtenerId(),
                p.getAutor().obtenerNombreVisible(),
                p.getReputacionAutor(),
                p.getCupoTotal(),
                p.getCupoDisponible(),
                p.getFranjas().stream()
                        .sorted(Comparator.comparing(FranjaDisponibilidad::getFechaHoraInicio))
                        .map(DtoMapper::franja).toList(),
                p.getFechaPublicacion());
    }

    public static ContratacionDTO contratacion(Contratacion c) {
        return new ContratacionDTO(
                c.getId(),
                c.getPublicacion().getId(),
                c.getPublicacion().getTitulo(),
                c.getCliente().obtenerId(),
                c.getCliente().obtenerNombreVisible(),
                c.getProveedorId(),
                c.getPublicacion().getAutor().obtenerNombreVisible(),
                franja(c.getFranjaElegida()),
                c.getEstadoNombre(),
                c.getMontoPagado() == null ? null : c.getMontoPagado().valor(),
                c.getMensaje(),
                c.getObjetivo(),
                c.getFechaCreacion(),
                franja(c.getFranjaPropuesta()),
                c.getReprogramacionPropuestaPor(),
                c.getMotivoCancelacion());
    }

    public static CalificacionDTO calificacion(Calificacion c) {
        return new CalificacionDTO(c.getId(), c.getContratacionId(), c.getAutorUserId(), c.getReceptorUserId(),
                c.getPuntaje(), c.getComentario(), c.getFecha());
    }

    public static NotificacionDTO notificacion(Notificacion n) {
        return new NotificacionDTO(n.getId(), nvl(n.getMensaje()), n.getCanal() == null ? null : n.getCanal().name(),
                n.getTipoEvento(), n.getEventoOrigen(), n.getEntidadId(), n.getFecha(), n.isLeida());
    }

    public static MovimientoDTO movimiento(MovimientoCreditos m) {
        long importe = m.getTipo() == TipoMovimientoCreditos.DEBITO ? -m.getMonto().valor() : m.getMonto().valor();
        long saldo = m.getSaldoPosterior() == null ? 0 : m.getSaldoPosterior().valor();
        return new MovimientoDTO(m.getId(), m.getTipo().name(), importe, nvl(m.getMotivo()), m.getFecha(), saldo);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
