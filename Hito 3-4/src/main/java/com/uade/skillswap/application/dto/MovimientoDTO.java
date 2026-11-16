package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;

/** importe con signo: negativo para debitos, positivo para creditos. */
public record MovimientoDTO(String id, String tipo, long importe, String motivo, LocalDateTime fecha, long saldoPosterior) {
}
