package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Secciones 4.1, 4.2 y 5: tipos de perfil, intereses, disponibilidad, reputacion y baja logica. */
class PerfilTest {

    @Test
    void unProfesionalPublicaOfertasYUnaOrganizacionDesafios() {
        Perfil profesional = new Perfil("prof-1", "Ada", TipoPerfil.PROFESIONAL);
        Perfil organizacion = new Perfil("org-1", "TechCorp", TipoPerfil.ORGANIZACION);

        assertEquals(TipoPublicacion.OFERTA, new Publicacion("p1", profesional, "Mentoria").getTipo());
        assertEquals(TipoPublicacion.DESAFIO,
                new Publicacion("p2", organizacion, "Desafio", TipoPublicacion.DESAFIO).getTipo());
        assertThrows(IllegalArgumentException.class,
                () -> new Publicacion("p3", profesional, "Desafio", TipoPublicacion.DESAFIO));
        assertThrows(IllegalArgumentException.class,
                () -> new Publicacion("p4", organizacion, "Mentoria", TipoPublicacion.OFERTA));
    }

    @Test
    void laDisponibilidadNoAdmiteRangosSuperpuestos() {
        Perfil perfil = new Perfil("prof-1", "Ada");
        DisponibilidadSemanal lunesManiana = new DisponibilidadSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        DisponibilidadSemanal lunesPisado = new DisponibilidadSemanal(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0));
        DisponibilidadSemanal martes = new DisponibilidadSemanal(DayOfWeek.TUESDAY, LocalTime.of(11, 0), LocalTime.of(13, 0));

        perfil.definirDisponibilidad(List.of(lunesManiana, martes));
        assertEquals(2, perfil.getDisponibilidad().size());
        assertThrows(IllegalArgumentException.class, () -> perfil.definirDisponibilidad(List.of(lunesManiana, lunesPisado)));
        assertThrows(IllegalArgumentException.class,
                () -> new DisponibilidadSemanal(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(9, 0)));
    }

    @Test
    void losInteresesInfluyenEnLaRecomendacionPorAfinidad() {
        Habilidad java = new Habilidad("h-java", "Java");
        Habilidad ux = new Habilidad("h-ux", "UX");
        Perfil cliente = new Perfil("cli-1", "Beto");
        cliente.agregarInteres(ux);

        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion deJava = new Publicacion("p-java", autor, "Java");
        deJava.agregarEtiqueta(java);
        Publicacion deUx = new Publicacion("p-ux", autor, "UX");
        deUx.agregarEtiqueta(ux);

        List<Publicacion> recomendadas = new AfinidadHabilidadesStrategy().recomendar(cliente, List.of(deJava, deUx));
        assertEquals("p-ux", recomendadas.get(0).getId());
    }

    @Test
    void laReputacionEsElPromedioDeLasCalificacionesRecibidas() {
        Perfil perfil = new Perfil("prof-1", "Ada");
        perfil.recalcularReputacion(List.of(
                new Calificacion("c1", "k1", "a", "prof-1", 5, "", LocalDateTime.now()),
                new Calificacion("c2", "k2", "b", "prof-1", 4, "", LocalDateTime.now())));
        assertEquals(4.5, perfil.getReputacionPromedio());
        assertEquals(2, perfil.getCantidadCalificaciones());
        perfil.recalcularReputacion(List.of());
        assertEquals(0, perfil.getReputacionPromedio());
    }

    @Test
    void unPerfilDadoDeBajaNoPuedePublicarNiSolicitarPeroSigueExistiendo() {
        Perfil autor = new Perfil("prof-1", "Ada");
        Perfil cliente = new Perfil("cli-1", "Beto");
        Publicacion publicacion = new Publicacion("pub-1", autor, "Mentoria");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("f", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1), 1);
        publicacion.completarDatos("desc", new CategoriaHabilidad("cat", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(10));
        publicacion.agregarFranja(franja);
        publicacion.publicar("prof-1");

        cliente.darBajaLogica();
        assertFalse(cliente.isActivo());
        assertThrows(IllegalStateException.class, () -> new Contratacion("c", cliente, publicacion, franja, "", ""));

        autor.darBajaLogica();
        assertThrows(IllegalStateException.class, () -> new Publicacion("pub-2", autor, "Otra"));
    }

    @Test
    void unaPublicacionSoloSeEditaEnBorradorOPausada() {
        Perfil autor = new Perfil("prof-1", "Ada");
        Publicacion publicacion = new Publicacion("pub-1", autor, "Mentoria");
        publicacion.completarDatos("desc", new CategoriaHabilidad("cat", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(10));
        publicacion.agregarFranja(new FranjaDisponibilidad("f", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1), 1));
        publicacion.publicar("prof-1");
        assertNotNull(publicacion.getFechaPublicacion());

        assertThrows(IllegalStateException.class, () -> publicacion.setTitulo("Otro titulo"));
        publicacion.pausar("prof-1");
        publicacion.setTitulo("Otro titulo");
        assertEquals("Otro titulo", publicacion.getTitulo());
    }

    @Test
    void franjasQueSePisanSeDetectan() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        FranjaDisponibilidad a = new FranjaDisponibilidad("a", base, base.plusHours(2), 1);
        FranjaDisponibilidad b = new FranjaDisponibilidad("b", base.plusHours(1), base.plusHours(3), 1);
        FranjaDisponibilidad c = new FranjaDisponibilidad("c", base.plusHours(2), base.plusHours(3), 1);
        assertTrue(a.seSuperponeCon(b));
        assertFalse(a.seSuperponeCon(c), "una franja que empieza justo cuando termina la otra no se pisa");
    }
}
