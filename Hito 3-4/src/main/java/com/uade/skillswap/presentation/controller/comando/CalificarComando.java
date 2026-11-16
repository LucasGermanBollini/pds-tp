package com.uade.skillswap.presentation.controller.comando;

/** Entrada de CalificacionController.calificar. El receptor no se pide: es la otra parte de la contratacion. */
public record CalificarComando(String contratacionId, int puntaje, String comentario) {
}
