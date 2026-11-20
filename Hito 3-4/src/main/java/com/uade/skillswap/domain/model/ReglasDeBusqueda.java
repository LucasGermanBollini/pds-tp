package com.uade.skillswap.domain.model;

import com.uade.skillswap.domain.repository.CriteriosBusqueda;
import com.uade.skillswap.domain.repository.OrdenBusqueda;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;

/**
 * Las reglas de negocio de la busqueda (que significa "coincide" y que
 * significa "relevante"), en un solo lugar. Las usa la implementacion en
 * memoria del repositorio y documenta lo que replica la consulta JPQL del
 * repositorio JPA, para que ambas den el mismo resultado.
 *
 * <p>Relevancia: 3 puntos si el texto aparece en el titulo, 2 si coincide
 * con una etiqueta (habilidad) y 1 si aparece en la descripcion. A igual
 * puntaje (o sin texto) desempata la popularidad (cantidad de solicitudes) y
 * despues la reputacion del autor.
 */
public final class ReglasDeBusqueda {

    private ReglasDeBusqueda() {
    }

    public static boolean coincide(Publicacion p, CriteriosBusqueda c) {
        if (!p.estaPublicada()) {
            return false;
        }
        if (c.texto() != null && puntajeTexto(p, c.texto()) == 0) {
            return false;
        }
        if (c.categoria() != null && (p.getCategoria() == null
                || !p.getCategoria().obtenerNombre().equalsIgnoreCase(c.categoria()))) {
            return false;
        }
        if (c.modalidad() != null && p.getModalidad() != c.modalidad()) {
            return false;
        }
        long precio = p.getPrecioBase() == null ? 0 : p.getPrecioBase().valor();
        if (c.precioMinimo() != null && precio < c.precioMinimo()) {
            return false;
        }
        if (c.precioMaximo() != null && precio > c.precioMaximo()) {
            return false;
        }
        if (c.habilidad() != null && p.getEtiquetas().stream()
                .noneMatch(h -> h.obtenerNombre().equalsIgnoreCase(c.habilidad()))) {
            return false;
        }
        if (c.reputacionMinima() != null && p.getReputacionAutor() < c.reputacionMinima()) {
            return false;
        }
        return c.tipo() == null || p.getTipo() == c.tipo();
    }

    public static int puntajeTexto(Publicacion p, String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        String t = texto.toLowerCase(Locale.ROOT);
        int puntaje = 0;
        if (contiene(p.getTitulo(), t)) {
            puntaje += 3;
        }
        if (p.getEtiquetas().stream().anyMatch(h -> contiene(h.obtenerNombre(), t))) {
            puntaje += 2;
        }
        if (contiene(p.getDescripcion(), t)) {
            puntaje += 1;
        }
        return puntaje;
    }

    public static Comparator<Publicacion> comparador(CriteriosBusqueda c) {
        Comparator<Publicacion> porReputacion = Comparator.comparingDouble(Publicacion::getReputacionAutor).reversed();
        Comparator<Publicacion> porFecha = Comparator.comparing(
                (Publicacion p) -> p.getFechaPublicacion() == null ? LocalDateTime.MIN : p.getFechaPublicacion()).reversed();
        Comparator<Publicacion> porId = Comparator.comparing(Publicacion::getId);
        OrdenBusqueda orden = c.orden();
        return switch (orden) {
            case MEJOR_REPUTACION -> porReputacion.thenComparing(porFecha).thenComparing(porId);
            case MENOR_PRECIO -> Comparator.comparingLong((Publicacion p) -> p.getPrecioBase() == null ? 0 : p.getPrecioBase().valor())
                    .thenComparing(porReputacion).thenComparing(porId);
            case MAS_RECIENTE -> porFecha.thenComparing(porId);
            case RELEVANCIA -> Comparator.comparingInt((Publicacion p) -> puntajeTexto(p, c.texto())).reversed()
                    .thenComparing(Comparator.comparingInt(Publicacion::getContadorSolicitudes).reversed())
                    .thenComparing(porReputacion).thenComparing(porId);
        };
    }

    private static boolean contiene(String campo, String textoEnMinuscula) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(textoEnMinuscula);
    }
}
