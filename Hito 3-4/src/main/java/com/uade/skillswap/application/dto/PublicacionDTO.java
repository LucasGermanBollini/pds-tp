package com.uade.skillswap.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Lo que devuelven los controllers sobre una publicacion: datos planos, sin entidades JPA (seccion 10). */
public record PublicacionDTO(String id, String tipo, String titulo, String descripcion, String categoria,
                             String modalidad, int duracionMinutos, long precio, List<String> etiquetas,
                             String estado, String autorId, String autorNombre, double reputacionAutor,
                             int cupoTotal, int cupoDisponible, List<FranjaDTO> franjas,
                             LocalDateTime fechaPublicacion) {
}
