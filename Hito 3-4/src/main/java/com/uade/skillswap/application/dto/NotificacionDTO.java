package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record NotificacionDTO(String id, String mensaje, String canal, String tipoEvento, String entidadTipo,
                              String entidadId, LocalDateTime fecha, boolean leida) {
}
