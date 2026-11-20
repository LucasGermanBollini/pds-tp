package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicacionStateTest {

    private Publicacion nuevaPublicacionIncompleta() {
        Perfil autor = new Perfil("prof-1", "Ada");
        return new Publicacion("pub-1", autor, "Mentoria de entrevistas");
    }

    private Publicacion publicacionCompleta() {
        Publicacion publicacion = nuevaPublicacionIncompleta();
        CategoriaHabilidad categoria = new CategoriaHabilidad("cat-1", "Carrera");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("franja-1",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), 3);
        publicacion.completarDatos("Preparacion de entrevistas tecnicas", categoria, Modalidad.REMOTA, 60, SkillCoins.de(100));
        publicacion.agregarFranja(franja);
        return publicacion;
    }

    @Test
    void rechazaPublicarUnaPublicacionIncompleta() {
        Publicacion publicacion = nuevaPublicacionIncompleta();
        assertThrows(IllegalArgumentException.class, () -> publicacion.publicar("prof-1"));
    }

    @Test
    void publicaUnaPublicacionCompleta() {
        Publicacion publicacion = publicacionCompleta();
        publicacion.publicar("prof-1");
        assertEquals("PUBLICADA", publicacion.getEstadoNombre());
    }

    @Test
    void pausaYReanudaUnaPublicacionPublicada() {
        Publicacion publicacion = publicacionCompleta();
        publicacion.publicar("prof-1");
        publicacion.pausar("prof-1");
        assertEquals("PAUSADA", publicacion.getEstadoNombre());
        publicacion.publicar("prof-1");
        assertEquals("PUBLICADA", publicacion.getEstadoNombre());
    }

    @Test
    void unaPublicacionCerradaNoPuedeReabrirse() {
        Publicacion publicacion = publicacionCompleta();
        publicacion.publicar("prof-1");
        publicacion.cerrar("prof-1");
        assertEquals("CERRADA", publicacion.getEstadoNombre());
        assertThrows(IllegalStateException.class, () -> publicacion.publicar("prof-1"));
    }

    @Test
    void cancelarSinMotivoEsInvalido() {
        Publicacion publicacion = publicacionCompleta();
        assertThrows(IllegalArgumentException.class, () -> publicacion.cancelar("prof-1", "   "));
    }

    @Test
    void cancelarConMotivoFunciona() {
        Publicacion publicacion = publicacionCompleta();
        publicacion.cancelar("prof-1", "Ya no tengo disponibilidad");
        assertEquals("CANCELADA", publicacion.getEstadoNombre());
    }

    @Test
    void registraHistorialMedianteObserver() {
        Publicacion publicacion = publicacionCompleta();
        List<String> capturados = new java.util.ArrayList<>();
        publicacion.agregarObservador(evento -> capturados.add(evento.getEstadoAnterior() + "->" + evento.getEstadoNuevo()));
        publicacion.publicar("prof-1");
        assertEquals(List.of("BORRADOR->PUBLICADA"), capturados);
    }
}
