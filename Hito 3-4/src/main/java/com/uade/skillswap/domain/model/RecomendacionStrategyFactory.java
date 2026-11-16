package com.uade.skillswap.domain.model;

/**
 * Simple Factory Pattern. Resuelve una RecomendacionStrategy a partir de un
 * nombre, para que el caso de uso pueda cambiar de estrategia sin un
 * if/switch propio (seccion 15: "Cambiar la estrategia de recomendacion sin
 * modificar el caso de uso consumidor").
 */
public final class RecomendacionStrategyFactory {

    private RecomendacionStrategyFactory() {
    }

    public static RecomendacionStrategy crear(String nombre) {
        if (nombre == null) {
            return new AfinidadHabilidadesStrategy();
        }
        return switch (nombre) {
            case "AFINIDAD_HABILIDADES" -> new AfinidadHabilidadesStrategy();
            case "POPULARIDAD_RECIENTE" -> new PopularidadRecienteStrategy();
            default -> throw new IllegalArgumentException("Estrategia de recomendacion desconocida: " + nombre);
        };
    }
}
