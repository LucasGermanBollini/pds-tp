package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

public record FranjaDTO(String id, LocalDateTime inicio, LocalDateTime fin, int cupoTotal, int cupoDisponible) {
}
