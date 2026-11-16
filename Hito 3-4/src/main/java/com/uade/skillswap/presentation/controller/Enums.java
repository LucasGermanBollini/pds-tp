package com.uade.skillswap.presentation.controller;

import java.util.Locale;

/** Convierte el texto que llega de la interfaz al enum del dominio, con un mensaje claro si no existe. */
final class Enums {

    private Enums() {
    }

    static <E extends Enum<E>> E valor(Class<E> tipo, String texto, E porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor invalido para " + tipo.getSimpleName() + ": " + texto);
        }
    }
}
