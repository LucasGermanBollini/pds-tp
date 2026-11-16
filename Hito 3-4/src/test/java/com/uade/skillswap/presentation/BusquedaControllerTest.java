package com.uade.skillswap.presentation;

import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.Resultado;
import com.uade.skillswap.presentation.controller.comando.BuscarPublicacionesComando;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seccion 4.3 y caso de aceptacion 1 de la seccion 15: "Publicar una
 * mentoria valida y encontrarla mediante busqueda filtrada". Cubre todos los
 * filtros, los cuatro ordenes y la paginacion.
 */
class BusquedaControllerTest {

    private Escenario e;

    @BeforeEach
    void setUp() {
        e = new Escenario();
        e.perfil("ada", "Ada", "PROFESIONAL", "INTERNA", 0);
        e.perfil("bruno", "Bruno", "PROFESIONAL", "INTERNA", 0);
        e.perfil("cli", "Cliente", "PROFESIONAL", "INTERNA", 5000);
        e.perfil("org", "TechCorp", "ORGANIZACION", "INTERNA", 0);

        e.publicar("ada", "Entrevistas tecnicas en Java", "Carrera", "REMOTA", 300, List.of("Java", "Entrevistas"),
                List.of(e.franja(0, 5)));
        e.publicar("ada", "Revision de portfolio", "Diseno", "PRESENCIAL", 100, List.of("Portfolio"),
                List.of(e.franja(1, 5)));
        e.publicar("bruno", "Arquitectura de software", "Carrera", "HIBRIDA", 500, List.of("Java"),
                List.of(e.franja(2, 5)));
        e.publicar("org", "Desafio: app de turnos", "Desarrollo", "REMOTA", 800, List.of("Prototipado"),
                List.of(e.franja(3, 5)));

        // Bruno recibe una calificacion de 5 para poder filtrar por reputacion.
        String arquitectura = buscar(new BuscarPublicacionesComando("Arquitectura", null, null, null, null, null, null,
                null, null, 0, 10)).get(0).id();
        String c = e.app.contrataciones().solicitar("cli", new SolicitarComando(arquitectura, null, "", "")).valorOLanzar().id();
        e.app.contrataciones().aceptar("bruno", c).valorOLanzar();
        e.app.contrataciones().pagar("cli", c).valorOLanzar();
        e.app.contrataciones().iniciar("bruno", c).valorOLanzar();
        e.app.contrataciones().completar("bruno", c).valorOLanzar();
        e.app.calificaciones().calificar("cli", new CalificarComando(c, 5, "Excelente")).valorOLanzar();
    }

    private List<PublicacionDTO> buscar(BuscarPublicacionesComando comando) {
        return e.app.busqueda().buscar(comando).valorOLanzar().contenido();
    }

    private static BuscarPublicacionesComando filtros(String texto, String categoria, String modalidad, Long min,
                                                     Long max, String habilidad, Double reputacion, String tipo,
                                                     String orden) {
        return new BuscarPublicacionesComando(texto, categoria, modalidad, min, max, habilidad, reputacion, tipo, orden, 0, 10);
    }

    @Test
    void filtraPorTextoEnTituloEtiquetasYDescripcion() {
        assertEquals(List.of("Entrevistas tecnicas en Java"),
                buscar(filtros("entrevista", null, null, null, null, null, null, null, null)).stream().map(PublicacionDTO::titulo).toList());
        assertEquals(2, buscar(filtros("java", null, null, null, null, null, null, null, null)).size(),
                "matchea por titulo y tambien por etiqueta");
    }

    @Test
    void filtraPorCategoriaModalidadYRangoDePrecio() {
        assertEquals(2, buscar(filtros(null, "carrera", null, null, null, null, null, null, null)).size());
        assertEquals(1, buscar(filtros(null, null, "PRESENCIAL", null, null, null, null, null, null)).size());
        List<PublicacionDTO> entre = buscar(filtros(null, null, null, 200L, 600L, null, null, null, null));
        assertEquals(2, entre.size());
        assertTrue(entre.stream().allMatch(p -> p.precio() >= 200 && p.precio() <= 600));
    }

    @Test
    void filtraPorHabilidadReputacionMinimaYTipo() {
        assertEquals(2, buscar(filtros(null, null, null, null, null, "java", null, null, null)).size());
        List<PublicacionDTO> buenaReputacion = buscar(filtros(null, null, null, null, null, null, 4.5, null, null));
        assertEquals(List.of("Arquitectura de software"), buenaReputacion.stream().map(PublicacionDTO::titulo).toList());
        assertEquals(List.of("Desafio: app de turnos"),
                buscar(filtros(null, null, null, null, null, null, null, "DESAFIO", null)).stream().map(PublicacionDTO::titulo).toList());
    }

    @Test
    void ordenaPorMenorPrecioMejorReputacionYMasReciente() {
        List<Long> precios = buscar(filtros(null, null, null, null, null, null, null, null, "MENOR_PRECIO")).stream()
                .map(PublicacionDTO::precio).toList();
        assertEquals(List.of(100L, 300L, 500L, 800L), precios);

        assertEquals("Arquitectura de software",
                buscar(filtros(null, null, null, null, null, null, null, null, "Mejor reputación")).get(0).titulo());

        List<PublicacionDTO> recientes = buscar(filtros(null, null, null, null, null, null, null, null, "MAS_RECIENTE"));
        for (int i = 1; i < recientes.size(); i++) {
            assertFalse(recientes.get(i).fechaPublicacion().isAfter(recientes.get(i - 1).fechaPublicacion()));
        }
    }

    @Test
    void ordenaPorRelevanciaTituloAntesQueEtiqueta() {
        List<PublicacionDTO> java = buscar(filtros("java", null, null, null, null, null, null, null, "RELEVANCIA"));
        assertEquals("Entrevistas tecnicas en Java", java.get(0).titulo(), "titulo (3) + etiqueta (2) gana a solo etiqueta (2)");
    }

    @Test
    void paginaLosResultados() {
        Pagina<PublicacionDTO> primera = e.app.busqueda().buscar(new BuscarPublicacionesComando(null, null, null, null,
                null, null, null, null, "MENOR_PRECIO", 0, 3)).valorOLanzar();
        Pagina<PublicacionDTO> segunda = e.app.busqueda().buscar(new BuscarPublicacionesComando(null, null, null, null,
                null, null, null, null, "MENOR_PRECIO", 1, 3)).valorOLanzar();
        assertEquals(4, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertEquals(3, primera.contenido().size());
        assertTrue(primera.tieneSiguiente());
        assertEquals(1, segunda.contenido().size());
        assertEquals(800L, segunda.contenido().get(0).precio());
        assertFalse(segunda.tieneSiguiente());
    }

    @Test
    void lasPublicacionesNoPublicadasNoAparecen() {
        String id = buscar(filtros("portfolio", null, null, null, null, null, null, null, null)).get(0).id();
        e.app.publicaciones().cambiarEstado("ada", id, "PAUSADA", null).valorOLanzar();
        assertTrue(buscar(filtros("portfolio", null, null, null, null, null, null, null, null)).isEmpty());
    }

    @Test
    void unRangoDePrecioInvertidoEsUnDatoInvalido() {
        Resultado<?> r = e.app.busqueda().buscar(filtros(null, null, null, 500L, 100L, null, null, null, null));
        assertFalse(r.exito());
        assertEquals("DATOS_INVALIDOS", r.codigoError());
    }

    @Test
    void recomiendaConDosEstrategiasIntercambiablesSinIncluirLasPropias() {
        e.app.perfiles().agregarInteres("cli", "Portfolio").valorOLanzar();
        List<PublicacionDTO> porAfinidad = e.app.recomendaciones().recomendar("cli", "AFINIDAD_HABILIDADES", 0, 10)
                .valorOLanzar().contenido();
        assertEquals("Revision de portfolio", porAfinidad.get(0).titulo());

        List<PublicacionDTO> porPopularidad = e.app.recomendaciones().recomendar("cli", "POPULARIDAD_RECIENTE", 0, 10)
                .valorOLanzar().contenido();
        assertEquals("Arquitectura de software", porPopularidad.get(0).titulo(), "es la unica con una solicitud");

        assertTrue(e.app.recomendaciones().recomendar("ada", null, 0, 10).valorOLanzar().contenido().stream()
                .noneMatch(p -> p.autorId().equals("ada")), "no se recomiendan las publicaciones propias");
        assertEquals("DATOS_INVALIDOS", e.app.recomendaciones().recomendar("cli", "INEXISTENTE", 0, 10).codigoError());
    }
}
