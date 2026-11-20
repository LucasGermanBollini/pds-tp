package com.uade.skillswap.domain.model;

/**
 * Rol de quien puede publicar una oferta o un desafio (Perfil profesional,
 * y en el futuro Organizacion). Permite que Publicacion no dependa de una
 * clase concreta (GRASP Protected Variations).
 */
public interface Publicador {

    String obtenerId();

    String obtenerNombreVisible();
}
