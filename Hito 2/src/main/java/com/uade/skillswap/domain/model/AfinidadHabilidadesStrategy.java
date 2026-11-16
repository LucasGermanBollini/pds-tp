package com.uade.skillswap.domain.model;

import java.util.Comparator;
import java.util.List;

/** Strategy Pattern - Concrete Strategy. Ordena por cantidad de habilidades en comun con el perfil. */
public class AfinidadHabilidadesStrategy implements RecomendacionStrategy {

    @Override
    public String getNombre() {
        return "AFINIDAD_HABILIDADES";
    }

    @Override
    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas) {
        List<Habilidad> habilidadesPerfil = perfil.getHabilidades();
        return candidatas.stream()
                .sorted(Comparator.comparingInt((Publicacion p) -> contarCoincidencias(p, habilidadesPerfil)).reversed())
                .toList();
    }

    private int contarCoincidencias(Publicacion publicacion, List<Habilidad> habilidadesPerfil) {
        return (int) publicacion.getEtiquetas().stream().filter(habilidadesPerfil::contains).count();
    }
}
