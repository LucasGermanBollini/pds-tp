package com.uade.skillswap.presentation.controller.comando;

/** Entrada de PerfilController.crearPerfil. tipo: PROFESIONAL | ORGANIZACION. canal: EMAIL | PUSH | INTERNA. */
public record CrearPerfilComando(String userId, String nombreVisible, String tipo, String biografia,
                                 String zonaHoraria, String rol, String canalPreferido) {
}
