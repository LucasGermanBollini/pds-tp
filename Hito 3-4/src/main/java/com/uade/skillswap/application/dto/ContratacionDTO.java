package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

/**
 * Seccion 4.5: en COMPLETADA el DTO funciona tambien como comprobante
 * (monto pagado, fechas, partes). La franja propuesta indica una
 * reprogramacion pendiente de respuesta.
 */
public record ContratacionDTO(String id, String publicacionId, String titulo, String clienteId, String clienteNombre,
                              String proveedorId, String proveedorNombre, FranjaDTO franja, String estado,
                              Long montoPagado, String mensaje, String objetivo, LocalDateTime fechaCreacion,
                              FranjaDTO franjaPropuesta, String reprogramacionPropuestaPor,
                              String motivoCancelacion) {
}
