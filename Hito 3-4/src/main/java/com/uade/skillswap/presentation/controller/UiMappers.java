package com.uade.skillswap.presentation.controller;

import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.usecase.PerfilResumen;
import com.uade.skillswap.domain.model.*;

/**
 * Traduce entre las entidades/VOs del dominio (com.uade.skillswap) y los
 * records planos que espera la UI (ar.edu.uade.skillswap.model.Models).
 * Vive en Presentacion: es la unica capa que conoce ambos vocabularios
 * (seccion 6: Presentacion no conoce reglas de negocio, pero si conoce
 * "como mostrarlas").
 */
final class UiMappers {

    private UiMappers() {
    }

    static Models.Modalidad aModalidadUi(Modalidad m) {
        if (m == null) return Models.Modalidad.REMOTA;
        return Models.Modalidad.valueOf(m.name());
    }

    /** null pasa derecho: en un filtro de busqueda significa "cualquier modalidad". */
    static Modalidad aModalidadDominio(Models.Modalidad m) {
        return m == null ? null : Modalidad.valueOf(m.name());
    }

    static Models.EstadoPublicacion aEstadoPublicacionUi(String nombre) {
        return Models.EstadoPublicacion.valueOf(nombre);
    }

    static Models.EstadoContratacion aEstadoContratacionUi(String nombre) {
        return Models.EstadoContratacion.valueOf(nombre);
    }

    static Models.Publicacion aPublicacionUi(Publicacion p) {
        String categoria = p.getCategoria() == null ? "" : p.getCategoria().obtenerNombre();
        double reputacion = p.getAutor() instanceof Perfil perfil ? perfil.getReputacionPromedio() : 0;
        String proveedor = p.getAutor() == null ? "" : p.getAutor().obtenerNombreVisible();
        long precio = p.getPrecioBase() == null ? 0 : p.getPrecioBase().valor();
        java.util.List<String> etiquetas = p.getEtiquetas().stream().map(Habilidad::obtenerNombre).toList();
        return new Models.Publicacion(p.getId(), p.getTitulo(), nvl(p.getDescripcion()), categoria,
                aModalidadUi(p.getModalidad()), p.getDuracionEstimadaMinutos(), (int) precio, reputacion,
                proveedor, etiquetas, aEstadoPublicacionUi(p.getEstadoNombre()), false);
    }

    static Models.Contratacion aContratacionUi(Contratacion c, String userIdQueMira) {
        boolean esCliente = c.getCliente().obtenerId().equals(userIdQueMira);
        String contraparte = esCliente ? c.getPublicacion().getAutor().obtenerNombreVisible() : c.getCliente().obtenerNombreVisible();
        long precio = c.getMontoPagado() != null ? c.getMontoPagado().valor()
                : (c.getPublicacion().getPrecioBase() == null ? 0 : c.getPublicacion().getPrecioBase().valor());
        var fecha = c.getFranjaElegida() != null ? c.getFranjaElegida().getFechaHoraInicio() : c.getFechaCreacion();
        return new Models.Contratacion(c.getId(), c.getPublicacion().getTitulo(), contraparte, fecha,
                (int) precio, aEstadoContratacionUi(c.getEstadoNombre()));
    }

    static Models.Movimiento aMovimientoUi(MovimientoCreditos m) {
        long importe = m.getTipo() == TipoMovimientoCreditos.DEBITO ? -m.getMonto().valor() : m.getMonto().valor();
        long saldo = m.getSaldoPosterior() == null ? 0 : m.getSaldoPosterior().valor();
        return new Models.Movimiento(nvl(m.getMotivo()), m.getFecha(), (int) importe, (int) saldo);
    }

    static Models.Notificacion aNotificacionUi(Notificacion n) {
        String titulo = switch (nvl(n.getEventoOrigen())) {
            case "PUBLICACION" -> "Actualizacion de tu publicacion";
            case "CONTRATACION" -> "Actualizacion de tu contratacion";
            default -> "Notificacion";
        };
        return new Models.Notificacion(n.getId(), titulo, nvl(n.getMensaje()), n.getFecha(), n.isLeida(), nvl(n.getEventoOrigen()));
    }

    static Models.Perfil aPerfilUi(PerfilResumen resumen) {
        Perfil perfil = resumen.perfil();
        java.util.List<String> habilidades = perfil.getHabilidades().stream().map(Habilidad::obtenerNombre).toList();
        return new Models.Perfil(perfil.obtenerId(), perfil.obtenerNombreVisible(), nvl(perfil.getRolDescripcion()),
                nvl(perfil.getBiografia()), habilidades, perfil.getReputacionPromedio(),
                perfil.getCantidadCalificaciones(), resumen.proyectosCompletados());
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
