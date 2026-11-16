package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContratacionStateTest {

    private Publicacion publicacionPublicada(int cupo) {
        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion publicacion = new Publicacion("pub-1", autor, "Mentoria de entrevistas");
        CategoriaHabilidad categoria = new CategoriaHabilidad("cat-1", "Carrera");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("franja-1",
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), cupo);
        publicacion.completarDatos("desc", categoria, Modalidad.REMOTA, 60, SkillCoins.de(100));
        publicacion.agregarFranja(franja);
        publicacion.publicar("prof-1");
        return publicacion;
    }

    @Test
    void elAutorNoPuedeSolicitarSuPropiaPublicacion() {
        Publicacion publicacion = publicacionPublicada(2);
        Perfil autorComoCliente = (Perfil) publicacion.getAutor();
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);
        assertThrows(IllegalArgumentException.class,
                () -> new Contratacion("c-1", autorComoCliente, publicacion, franja, "hola", "objetivo"));
    }

    @Test
    void noAdmiteSolicitudesSobreUnaPublicacionNoPublicada() {
        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion borrador = new Publicacion("pub-2", autor, "Sin publicar");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("f", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), 1);
        borrador.agregarFranja(franja);
        Perfil cliente = new Perfil("cli-1", "Beto");
        assertThrows(IllegalArgumentException.class,
                () -> new Contratacion("c-2", cliente, borrador, franja, "hola", "objetivo"));
    }

    @Test
    void flujoCompletoPendienteHastaCompletada() {
        Publicacion publicacion = publicacionPublicada(1);
        Perfil cliente = new Perfil("cli-1", "Beto");
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);
        Contratacion contratacion = new Contratacion("c-1", cliente, publicacion, franja, "hola", "practicar");

        assertEquals("PENDIENTE", contratacion.getEstadoNombre());
        contratacion.aceptar("prof-1");
        assertEquals("ACEPTADA", contratacion.getEstadoNombre());
        assertEquals(0, franja.getCupoDisponible());
        contratacion.pagar("cli-1");
        assertEquals("CONFIRMADA", contratacion.getEstadoNombre());
        contratacion.iniciar("prof-1");
        assertEquals("EN_CURSO", contratacion.getEstadoNombre());
        contratacion.completar("prof-1");
        assertEquals("COMPLETADA", contratacion.getEstadoNombre());
    }

    @Test
    void dosAceptacionesNoPuedenConsumirElUltimoCupo() {
        Publicacion publicacion = publicacionPublicada(1);
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);
        Perfil cliente1 = new Perfil("cli-1", "Beto");
        Perfil cliente2 = new Perfil("cli-2", "Clara");
        Contratacion c1 = new Contratacion("c-1", cliente1, publicacion, franja, "hola", "practicar");
        Contratacion c2 = new Contratacion("c-2", cliente2, publicacion, franja, "hola", "practicar");

        c1.aceptar("prof-1");
        assertEquals("ACEPTADA", c1.getEstadoNombre());
        assertThrows(IllegalStateException.class, () -> c2.aceptar("prof-1"));
        assertEquals("PENDIENTE", c2.getEstadoNombre(), "la segunda contratacion no debe cambiar de estado");
    }

    @Test
    void rechazarEsTerminal() {
        Publicacion publicacion = publicacionPublicada(1);
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);
        Perfil cliente = new Perfil("cli-1", "Beto");
        Contratacion contratacion = new Contratacion("c-1", cliente, publicacion, franja, "hola", "practicar");
        contratacion.rechazar("prof-1");
        assertEquals("RECHAZADA", contratacion.getEstadoNombre());
        assertThrows(IllegalStateException.class, () -> contratacion.aceptar("prof-1"));
    }
}
