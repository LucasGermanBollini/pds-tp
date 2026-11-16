package com.uade.skillswap.domain.repository;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado (seccion 4.3: "Paginar todos los listados devueltos por
 * los controllers de consulta"). Inmutable. `numero` empieza en 0.
 */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos) {

    public static final int TAMANIO_POR_DEFECTO = 10;
    public static final int TAMANIO_MAXIMO = 100;

    public Pagina {
        contenido = contenido == null ? List.of() : List.copyOf(contenido);
        if (numero < 0) {
            throw new IllegalArgumentException("El numero de pagina no puede ser negativo");
        }
        if (tamanio <= 0) {
            throw new IllegalArgumentException("El tamanio de pagina debe ser positivo");
        }
    }

    public int totalPaginas() {
        return (int) Math.ceil((double) totalElementos / tamanio);
    }

    public boolean tieneSiguiente() {
        return numero + 1 < totalPaginas();
    }

    public boolean tieneAnterior() {
        return numero > 0;
    }

    public <R> Pagina<R> map(Function<T, R> mapper) {
        return new Pagina<>(contenido.stream().map(mapper).toList(), numero, tamanio, totalElementos);
    }

    /** Pagina una lista ya cargada en memoria (listados chicos: notificaciones, movimientos, propias). */
    public static <T> Pagina<T> de(List<T> todos, int numero, int tamanio) {
        int tamanioValido = normalizarTamanio(tamanio);
        int numeroValido = Math.max(0, numero);
        int desde = Math.min(todos.size(), numeroValido * tamanioValido);
        int hasta = Math.min(todos.size(), desde + tamanioValido);
        return new Pagina<>(todos.subList(desde, hasta), numeroValido, tamanioValido, todos.size());
    }

    public static int normalizarTamanio(int tamanio) {
        if (tamanio <= 0) {
            return TAMANIO_POR_DEFECTO;
        }
        return Math.min(tamanio, TAMANIO_MAXIMO);
    }
}
