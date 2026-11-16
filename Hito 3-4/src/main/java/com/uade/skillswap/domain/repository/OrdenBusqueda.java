package com.uade.skillswap.domain.repository;

/** Seccion 4.3: "Ordenar por relevancia, mejor reputacion, menor precio o fecha mas reciente". */
public enum OrdenBusqueda {
    RELEVANCIA,
    MEJOR_REPUTACION,
    MENOR_PRECIO,
    MAS_RECIENTE;

    /** Acepta tambien los textos que manda el combo del Mock UI Kit ("Mejor reputacion", "Menor precio"...). */
    public static OrdenBusqueda desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return RELEVANCIA;
        }
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase()
                .replace(' ', '_');
        return switch (normalizado) {
            case "MEJOR_REPUTACION" -> MEJOR_REPUTACION;
            case "MENOR_PRECIO" -> MENOR_PRECIO;
            case "MAS_RECIENTE", "FECHA_MAS_RECIENTE", "MAS_RECIENTES" -> MAS_RECIENTE;
            default -> RELEVANCIA;
        };
    }
}
