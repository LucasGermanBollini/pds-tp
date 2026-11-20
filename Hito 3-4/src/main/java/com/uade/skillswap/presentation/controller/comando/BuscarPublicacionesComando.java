package com.uade.skillswap.presentation.controller.comando;

/**
 * Filtros de BusquedaController.buscar (seccion 4.3). Todos opcionales.
 * orden: RELEVANCIA | MEJOR_REPUTACION | MENOR_PRECIO | MAS_RECIENTE.
 */
public record BuscarPublicacionesComando(String texto, String categoria, String modalidad, Long precioMinimo,
                                         Long precioMaximo, String habilidad, Double reputacionMinima,
                                         String tipo, String orden, int pagina, int tamanio) {

    public static BuscarPublicacionesComando porTexto(String texto) {
        return new BuscarPublicacionesComando(texto, null, null, null, null, null, null, null, null, 0, 10);
    }
}
