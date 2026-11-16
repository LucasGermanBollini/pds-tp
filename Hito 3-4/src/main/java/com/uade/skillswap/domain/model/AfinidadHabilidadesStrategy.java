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
        // habilidades que ya tiene + intereses (lo que quiere aprender), seccion 4.1
        List<Habilidad> habilidadesPerfil = new java.util.ArrayList<>(perfil.getHabilidades());
        habilidadesPerfil.addAll(perfil.getIntereses());
        return candidatas.stream()
                .sorted(Comparator.comparingInt((Publicacion p) -> contarCoincidencias(p, habilidadesPerfil)).reversed())
                .toList();
    }

    private int contarCoincidencias(Publicacion publicacion, List<Habilidad> habilidadesPerfil) {
        return (int) publicacion.getEtiquetas().stream().filter(habilidadesPerfil::contains).count();
    }
}
