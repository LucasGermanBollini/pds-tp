package com.uade.skillswap.presentation.controller.comando;

import java.time.LocalDateTime;
import java.util.List;

/** Entrada de PublicacionController.crearBorrador (seccion 4.2). tipo: OFERTA | DESAFIO. */
public record CrearPublicacionComando(String titulo, String descripcion, String categoria, String modalidad,
                                      int duracionMinutos, long precio, List<String> etiquetas,
                                      List<Franja> franjas, String tipo) {

    public record Franja(LocalDateTime inicio, LocalDateTime fin, int cupo) {
    }
}
