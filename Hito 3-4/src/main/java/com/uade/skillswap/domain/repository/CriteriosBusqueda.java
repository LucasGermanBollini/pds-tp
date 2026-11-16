package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.Modalidad;
import com.uade.skillswap.domain.model.TipoPublicacion;

/**
 * Query object de la busqueda de publicaciones (seccion 4.3). Todos los
 * filtros son opcionales (null/blank/0 = "sin filtro"); siempre se buscan
 * solo publicaciones PUBLICADAS. Lo entiende cualquier implementacion del
 * puerto PublicacionRepository (en memoria o JPA), que es quien decide como
 * ejecutarlo (en la base: filtros + ORDER BY + LIMIT/OFFSET).
 */
public record CriteriosBusqueda(
        String texto,
        String categoria,
        Modalidad modalidad,
        Long precioMinimo,
        Long precioMaximo,
        String habilidad,
        Double reputacionMinima,
        TipoPublicacion tipo,
        OrdenBusqueda orden,
        int pagina,
        int tamanio) {

    public CriteriosBusqueda {
        texto = vacioANull(texto);
        categoria = "Todas".equalsIgnoreCase(vacioANull(categoria)) ? null : vacioANull(categoria);
        habilidad = vacioANull(habilidad);
        precioMinimo = precioMinimo != null && precioMinimo <= 0 ? null : precioMinimo;
        precioMaximo = precioMaximo != null && precioMaximo <= 0 ? null : precioMaximo;
        reputacionMinima = reputacionMinima != null && reputacionMinima <= 0 ? null : reputacionMinima;
        if (precioMinimo != null && precioMaximo != null && precioMinimo > precioMaximo) {
            throw new IllegalArgumentException("El precio minimo no puede ser mayor al maximo");
        }
        orden = orden == null ? OrdenBusqueda.RELEVANCIA : orden;
        pagina = Math.max(0, pagina);
        tamanio = Pagina.normalizarTamanio(tamanio);
    }

    /** Busqueda sin filtros, primera pagina. */
    public static CriteriosBusqueda todas() {
        return new CriteriosBusqueda(null, null, null, null, null, null, null, null, OrdenBusqueda.RELEVANCIA, 0,
                Pagina.TAMANIO_POR_DEFECTO);
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
