package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.MovimientoDTO;
import com.uade.skillswap.application.dto.NotificacionDTO;
import com.uade.skillswap.application.dto.PerfilPublicoDTO;
import com.uade.skillswap.application.dto.PublicacionDTO;

/**
 * Traduce los DTOs de nuestros controllers a los records planos que espera
 * la interfaz de la catedra (ar.edu.uade.skillswap.model.Models). Vive en
 * Presentacion: es la unica pieza que conoce los dos vocabularios.
 */
final class UiMappers {

    private UiMappers() {
    }

    static Models.Modalidad aModalidadUi(String modalidad) {
        return modalidad == null ? Models.Modalidad.REMOTA : Models.Modalidad.valueOf(modalidad);
    }

    static String aModalidad(Models.Modalidad m) {
        return m == null ? null : m.name();
    }

    static Models.Publicacion aPublicacionUi(PublicacionDTO p) {
        return new Models.Publicacion(p.id(), p.titulo(), p.descripcion(), p.categoria(), aModalidadUi(p.modalidad()),
                p.duracionMinutos(), (int) p.precio(), redondear(p.reputacionAutor()), p.autorNombre(), p.etiquetas(),
                Models.EstadoPublicacion.valueOf(p.estado()), false);
    }

    static Models.Contratacion aContratacionUi(ContratacionDTO c, String userIdQueMira) {
        boolean esCliente = c.clienteId().equals(userIdQueMira);
        String contraparte = esCliente ? c.proveedorNombre() : c.clienteNombre() + " (cliente)";
        if (c.franjaPropuesta() != null) {
            contraparte += " · reprogramacion propuesta";
        }
        int precio = c.montoPagado() != null ? c.montoPagado().intValue() : 0;
        var fecha = c.franja() != null ? c.franja().inicio() : c.fechaCreacion();
        return new Models.Contratacion(c.id(), c.titulo(), contraparte, fecha, precio,
                Models.EstadoContratacion.valueOf(c.estado()));
    }

    static Models.Movimiento aMovimientoUi(MovimientoDTO m) {
        return new Models.Movimiento(m.motivo(), m.fecha(), (int) m.importe(), (int) m.saldoPosterior());
    }

    static Models.Notificacion aNotificacionUi(NotificacionDTO n) {
        String tipoEvento = n.tipoEvento() == null ? "" : n.tipoEvento();
        String titulo = switch (tipoEvento) {
            case "SOLICITUD_RECIBIDA" -> "Nueva solicitud";
            case "REPROGRAMACION_PROPUESTA" -> "Propuesta de reprogramacion";
            case "REPROGRAMACION_ACEPTADA" -> "Reprogramacion aceptada";
            case "REPROGRAMACION_RECHAZADA" -> "Reprogramacion rechazada";
            default -> "PUBLICACION".equals(n.entidadTipo()) ? "Actualizacion de tu publicacion"
                    : "Actualizacion de tu contratacion";
        };
        String tipo = n.canal() == null ? "" : n.canal();
        return new Models.Notificacion(n.id(), titulo, n.mensaje(), n.fecha(), n.leida(), tipo);
    }

    static Models.Perfil aPerfilUi(PerfilPublicoDTO p) {
        return new Models.Perfil(p.id(), p.nombre(), p.rol(), p.biografia(), p.habilidades(), redondear(p.reputacion()),
                p.cantidadCalificaciones(), p.historial().totalCompletadas());
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
