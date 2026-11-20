package com.uade.skillswap.domain.model;

/**
 * Rol de quien puede publicar una oferta o un desafio (un Perfil
 * PROFESIONAL u ORGANIZACION). Permite que Publicacion no dependa de una
 * clase concreta (GRASP Protected Variations): la publicacion solo pregunta
 * "quien sos", "estas activo" y "podes publicar esto".
 */
public interface Publicador {

    String obtenerId();

    String obtenerNombreVisible();

    boolean estaActivo();

    boolean puedePublicar(TipoPublicacion tipoPublicacion);
}
