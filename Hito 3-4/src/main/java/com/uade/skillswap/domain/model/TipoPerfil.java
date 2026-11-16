package com.uade.skillswap.domain.model;

/**
 * Seccion 4.2: "Un profesional podra publicar una oferta de mentoria o
 * servicio. Una organizacion podra publicar un desafio". Las dos son
 * perfiles (ambas pueden ademas contratar como clientes, seccion 2).
 */
public enum TipoPerfil {
    PROFESIONAL,
    ORGANIZACION;

    /** Que tipo de publicacion puede crear cada tipo de perfil. */
    public boolean puedePublicar(TipoPublicacion tipoPublicacion) {
        return switch (this) {
            case PROFESIONAL -> tipoPublicacion == TipoPublicacion.OFERTA;
            case ORGANIZACION -> tipoPublicacion == TipoPublicacion.DESAFIO;
        };
    }
}
