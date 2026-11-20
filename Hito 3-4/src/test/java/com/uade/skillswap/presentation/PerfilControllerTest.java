package com.uade.skillswap.presentation;

import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.PerfilPublicoDTO;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.domain.model.DisponibilidadSemanal;
import com.uade.skillswap.presentation.controller.Resultado;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;
import com.uade.skillswap.presentation.controller.comando.CrearPerfilComando;
import com.uade.skillswap.presentation.controller.comando.CrearPublicacionComando;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Seccion 4.1 (perfiles y reputacion) y la Facade del perfil publico. */
class PerfilControllerTest {

    private Escenario e;

    @BeforeEach
    void setUp() {
        e = new Escenario();
        e.perfil("ada", "Ada", "PROFESIONAL", "INTERNA", 0);
        e.perfil("beto", "Beto", "PROFESIONAL", "INTERNA", 1000);
    }

    @Test
    void crearPerfilAbreSuWalletYNoPermiteDuplicados() {
        assertEquals(0L, e.app.wallet().consultarSaldo("ada").valorOLanzar());
        Resultado<PerfilPublicoDTO> duplicado = e.app.perfiles().crearPerfil(
                new CrearPerfilComando("ada", "Otra Ada", "PROFESIONAL", "", "", "", null));
        assertEquals("OPERACION_NO_PERMITIDA", duplicado.codigoError());
        assertEquals("ACCESO_DENEGADO", e.app.perfiles().crearPerfil(
                new CrearPerfilComando("", "Sin id", null, "", "", "", null)).codigoError(),
                "el userId lo tiene que reconocer el servicio de identidad");
        assertEquals("DATOS_INVALIDOS", e.app.perfiles().crearPerfil(
                new CrearPerfilComando("nuevo", "Nuevo", "ROBOT", "", "", "", null)).codigoError());
    }

    @Test
    void administrarHabilidadesInteresesDisponibilidadYCanal() {
        e.app.perfiles().agregarHabilidad("ada", "Java").valorOLanzar();
        e.app.perfiles().agregarInteres("ada", "UX").valorOLanzar();
        e.app.perfiles().definirDisponibilidad("ada", List.of(
                new DisponibilidadSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)))).valorOLanzar();
        e.app.perfiles().elegirCanalPreferido("ada", "EMAIL").valorOLanzar();
        PerfilPublicoDTO perfil = e.app.perfiles().actualizarDatos("ada", "Ada L.", "Mentora", "UTC", "Mentora")
                .valorOLanzar();

        assertEquals("Ada L.", perfil.nombre());
        assertEquals(List.of("Java"), perfil.habilidades());
        assertEquals(List.of("UX"), perfil.intereses());
        assertEquals(1, perfil.disponibilidad().size());
        assertEquals("EMAIL", perfil.canalPreferido());
        assertEquals("DATOS_INVALIDOS", e.app.perfiles().elegirCanalPreferido("ada", "PALOMA").codigoError());
    }

    @Test
    void elPerfilPublicoMuestraReputacionPublicacionesActivasEHistorial() {
        PublicacionDTO pub = e.publicar("ada", "Mentoria", "Carrera", "REMOTA", 100, List.of(), List.of(e.franja(0, 2)));
        PublicacionDTO borrador = e.app.publicaciones().crearBorrador("ada", new CrearPublicacionComando("Borrador",
                "", "Carrera", "REMOTA", 30, 10, List.of(), List.of(), null)).valorOLanzar();

        ContratacionDTO c = e.app.contrataciones().solicitar("beto", new SolicitarComando(pub.id(), null, "", ""))
                .valorOLanzar();
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().pagar("beto", c.id()).valorOLanzar();
        e.app.contrataciones().iniciar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().completar("ada", c.id()).valorOLanzar();
        e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 5, "Genial")).valorOLanzar();

        PerfilPublicoDTO ada = e.app.perfiles().verPerfil("ada").valorOLanzar();
        assertEquals(5.0, ada.reputacion());
        assertEquals(1, ada.cantidadCalificaciones());
        assertEquals(List.of(pub.id()), ada.publicacionesActivas().stream().map(PublicacionDTO::id).toList(),
                "el borrador " + borrador.id() + " no es una publicacion activa");
        assertEquals(1, ada.historial().sesionesCompletadasComoProveedor());
        assertEquals("Genial", ada.historial().ultimasCalificaciones().get(0).comentario());
        assertEquals(1, e.app.perfiles().verPerfil("beto").valorOLanzar().historial().sesionesCompletadasComoCliente());
        assertEquals("NO_ENCONTRADO", e.app.perfiles().verPerfil("nadie").codigoError());
    }

    @Test
    void laBajaLogicaPausaSusPublicacionesYConservaElHistorial() {
        PublicacionDTO pub = e.publicar("ada", "Mentoria", "Carrera", "REMOTA", 100, List.of(), List.of(e.franja(0, 2)));
        ContratacionDTO c = e.app.contrataciones().solicitar("beto", new SolicitarComando(pub.id(), null, "", ""))
                .valorOLanzar();

        e.app.perfiles().darDeBaja("ada").valorOLanzar();

        PerfilPublicoDTO ada = e.app.perfiles().verPerfil("ada").valorOLanzar();
        assertFalse(ada.activo());
        assertEquals("PAUSADA", e.app.publicaciones().verDetalle(pub.id()).valorOLanzar().estado());
        assertEquals(1, e.app.contrataciones().listar("beto", 0, 10).valorOLanzar().totalElementos(),
                "las contrataciones historicas no se borran");
        assertNotNull(c.id());
        assertEquals("OPERACION_NO_PERMITIDA", e.app.publicaciones().cambiarEstado("ada", pub.id(), "PUBLICADA", null).codigoError());
        assertEquals("OPERACION_NO_PERMITIDA", e.app.perfiles().agregarHabilidad("ada", "Java").codigoError());
    }

    @Test
    void unaOrganizacionPublicaDesafios() {
        e.perfil("org", "TechCorp", "ORGANIZACION", "INTERNA", 0);
        PublicacionDTO desafio = e.publicar("org", "Desafio", "Desarrollo", "REMOTA", 500, List.of(), List.of(e.franja(1, 10)));
        assertEquals("DESAFIO", desafio.tipo());
        assertEquals("DATOS_INVALIDOS", e.app.publicaciones().crearBorrador("org", new CrearPublicacionComando("Oferta",
                "", "Carrera", "REMOTA", 30, 10, List.of(), List.of(), "OFERTA")).codigoError());
    }

    @Test
    void publicarUnaOfertaIncompletaSeRechaza() {
        PublicacionDTO borrador = e.app.publicaciones().crearBorrador("ada", new CrearPublicacionComando("Sin franjas",
                "", "Carrera", "REMOTA", 30, 10, List.of(), List.of(), null)).valorOLanzar();
        Resultado<PublicacionDTO> r = e.app.publicaciones().cambiarEstado("ada", borrador.id(), "PUBLICADA", null);
        assertEquals("DATOS_INVALIDOS", r.codigoError());
        assertTrue(r.mensaje().contains("franja"));
        assertEquals("DATOS_INVALIDOS", e.app.publicaciones().crearBorrador("ada", new CrearPublicacionComando("Negativa",
                "", "Carrera", "REMOTA", 30, -5, List.of(), List.of(), null)).codigoError());
        assertEquals("DATOS_INVALIDOS", e.app.publicaciones().cambiarEstado("ada", borrador.id(), "VOLANDO", null).codigoError());
        assertEquals("DATOS_INVALIDOS", e.app.publicaciones().cambiarEstado("ada", borrador.id(), "CANCELADA", null).codigoError(),
                "cancelar exige motivo");
        e.app.publicaciones().agregarFranja("ada", borrador.id(), e.franja(4, 1)).valorOLanzar();
        assertEquals("PUBLICADA", e.app.publicaciones().cambiarEstado("ada", borrador.id(), "PUBLICADA", null).valorOLanzar().estado());
        assertEquals(1, e.app.publicaciones().listarPropias("ada", 0, 10).valorOLanzar().totalElementos());
    }
}
