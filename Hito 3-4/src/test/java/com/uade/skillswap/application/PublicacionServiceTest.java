package com.uade.skillswap.application;

import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.infrastructure.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicacionServiceTest {

    private PublicacionService service;
    private InMemoryPublicacionRepository publicacionRepository;

    @BeforeEach
    void setUp() {
        publicacionRepository = new InMemoryPublicacionRepository();
        service = new PublicacionService(publicacionRepository, new InMemoryElementoCatalogoRepository(), List.of());
    }

    @Test
    void crearCompletarYPublicarUnaOferta() {
        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion borrador = service.crearBorrador(autor, "Mentoria de entrevistas");
        assertEquals("BORRADOR", borrador.getEstadoNombre());

        FranjaDisponibilidad franja = new FranjaDisponibilidad("f-1", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), 3);
        service.completarDatos(borrador.getId(), "desc", new CategoriaHabilidad("cat-1", "Carrera"),
                Modalidad.REMOTA, 60, SkillCoins.de(500), List.of(), List.of(franja));

        Publicacion publicada = service.publicar(borrador.getId(), "prof-1");
        assertEquals("PUBLICADA", publicada.getEstadoNombre());
        assertEquals(1, service.buscarPublicadas().size());
    }

    @Test
    void rechazaPublicarUnaOfertaIncompleta() {
        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion borrador = service.crearBorrador(autor, "Sin completar");
        assertThrows(IllegalArgumentException.class, () -> service.publicar(borrador.getId(), "prof-1"));
    }

    @Test
    void cambiarLaEstrategiaDeRecomendacionNoRequiereTocarElCasoDeUso() {
        Perfil autor = new Perfil("prof-1", "Ada");
        Habilidad java = new Habilidad("h-1", "Java");
        Perfil cliente = new Perfil("cli-1", "Beto");
        cliente.agregarHabilidad(java);

        Publicacion conJava = publicarOferta(autor, "Con Java", List.of(java), 1);
        Publicacion sinJava = publicarOferta(autor, "Sin Java", List.of(), 2);
        sinJava.registrarSolicitud();
        sinJava.registrarSolicitud();
        publicacionRepository.guardar(sinJava);

        List<Publicacion> porAfinidad = service.recomendar(cliente, "AFINIDAD_HABILIDADES");
        List<Publicacion> porPopularidad = service.recomendar(cliente, "POPULARIDAD_RECIENTE");

        assertEquals(conJava.getId(), porAfinidad.get(0).getId());
        assertEquals(sinJava.getId(), porPopularidad.get(0).getId());
    }

    private Publicacion publicarOferta(Perfil autor, String titulo, List<Habilidad> etiquetas, int sufijoFranja) {
        Publicacion publicacion = service.crearBorrador(autor, titulo);
        FranjaDisponibilidad franja = new FranjaDisponibilidad("f-" + sufijoFranja, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), 3);
        service.completarDatos(publicacion.getId(), "desc", new CategoriaHabilidad("cat-" + sufijoFranja, "Cat"),
                Modalidad.REMOTA, 60, SkillCoins.de(500), etiquetas, List.of(franja));
        return service.publicar(publicacion.getId(), autor.obtenerId());
    }
}
