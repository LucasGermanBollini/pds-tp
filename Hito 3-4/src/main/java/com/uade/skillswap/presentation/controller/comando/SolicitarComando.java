package com.uade.skillswap.presentation.controller.comando;

/** Entrada de ContratacionController.solicitar (seccion 4.4). franjaId null = primera franja con cupo. */
public record SolicitarComando(String publicacionId, String franjaId, String mensaje, String objetivo) {
}
