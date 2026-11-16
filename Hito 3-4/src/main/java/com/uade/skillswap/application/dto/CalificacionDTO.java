package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record CalificacionDTO(String id, String contratacionId, String autorUserId, String receptorUserId,
                              int puntaje, String comentario, LocalDateTime fecha) {
}
