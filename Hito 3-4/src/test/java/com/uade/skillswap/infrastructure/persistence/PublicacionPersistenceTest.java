package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.infrastructure.persistence.repository.JpaElementoCatalogoRepository;
import com.uade.skillswap.infrastructure.persistence.repository.JpaPerfilRepository;
import com.uade.skillswap.infrastructure.persistence.repository.JpaPublicacionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba que el mapeo JPA de Publicacion (State persistido como texto +
 * reconstruccion via @PostLoad, Composite de categoria, ManyToMany de
 * etiquetas) sobrevive un guardado y una relectura real contra H2.
 */
class PublicacionPersistenceTest extends JpaTestSupport {

    private final JpaPerfilRepository perfilRepository = new JpaPerfilRepository();
    private final JpaElementoCatalogoRepository catalogoRepository = new JpaElementoCatalogoRepository();
    private final JpaPublicacionRepository publicacionRepository = new JpaPublicacionRepository();

    @Test
    void publicarSePersisteYElEstadoSeReconstruyeAlRecargar() {
        Perfil autor = (Perfil) perfilRepository.guardar(new Perfil("prof-ada", "Ada"));
        CategoriaHabilidad categoria = (CategoriaHabilidad) catalogoRepository.guardar(new CategoriaHabilidad("cat-carrera", "Carrera"));
        Habilidad java = (Habilidad) catalogoRepository.guardar(new Habilidad("hab-java", "Java"));

        Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, "Mentoria de entrevistas");
        FranjaDisponibilidad franja = new FranjaDisponibilidad(UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), 3);
        publicacion.completarDatos("desc", categoria, Modalidad.REMOTA, 60, SkillCoins.de(300));
        publicacion.agregarFranja(franja);
        publicacion.agregarEtiqueta(java);
        publicacion.publicar("prof-ada");

        String id = publicacionRepository.guardar(publicacion).getId();

        Publicacion recargada = publicacionRepository.buscarPorId(id).orElseThrow();
        assertEquals("PUBLICADA", recargada.getEstadoNombre());
        assertEquals(1, recargada.getFranjas().size());
        assertEquals("Carrera", recargada.getCategoria().obtenerNombre());
        assertEquals(List.of("Java"), recargada.getEtiquetas().stream().map(Habilidad::obtenerNombre).toList());

        // el objeto State real se reconstruye (no es solo un string): puede operar.
        assertThrows(IllegalStateException.class, () -> recargada.publicar("prof-ada"),
                "una PUBLICADA no puede volver a publicarse: si esto no lanza, el estado no se reconstruyo bien");
    }

    @Test
    void buscarPublicadasConFranjasEvitaElN1() {
        Perfil autor = (Perfil) perfilRepository.guardar(new Perfil("prof-ada", "Ada"));
        CategoriaHabilidad categoria = (CategoriaHabilidad) catalogoRepository.guardar(new CategoriaHabilidad("cat-general", "General"));

        for (int i = 0; i < 2; i++) {
            Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, "Mentoria " + i);
            FranjaDisponibilidad franja = new FranjaDisponibilidad(UUID.randomUUID().toString(),
                    LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1), 2);
            publicacion.completarDatos("desc", categoria, Modalidad.REMOTA, 30, SkillCoins.de(100));
            publicacion.agregarFranja(franja);
            publicacion.publicar("prof-ada");
            publicacionRepository.guardar(publicacion);
        }

        List<Publicacion> publicadas = publicacionRepository.buscarPublicadasConFranjas();
        assertEquals(2, publicadas.size());
        publicadas.forEach(p -> assertFalse(p.getFranjas().isEmpty()));
    }
}
