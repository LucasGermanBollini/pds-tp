package com.uade.skillswap.presentation;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.presentation.controller.ErrorDeAplicacionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seccion 10: la interfaz de la catedra conectada a nuestros controllers.
 * Se ejercita el contrato UiControllers exactamente como lo usan las
 * pantallas (mismos metodos y parametros), jugando los dos roles.
 */
class UiAdaptadoresTest {

    private Escenario e;
    private UiControllers ui;
    private PublicacionDTO mentoria;

    @BeforeEach
    void setUp() {
        e = new Escenario();
        ui = e.app.ui();
        e.perfil("ada", "Ada Lovelace", "PROFESIONAL", "INTERNA", 0);
        e.perfil("beto", "Beto", "PROFESIONAL", "INTERNA", 1000);
        e.app.perfiles().agregarHabilidad("ada", "Java").valorOLanzar();
        mentoria = e.publicar("ada", "Mentoria Java", "Tecnologia", "REMOTA", 200, List.of("Java"), List.of(e.franja(0, 2)));
    }

    @Test
    void explorarCategoriasYRecomendaciones() {
        assertEquals("Todas", ui.busqueda().categorias().get(0));
        assertTrue(ui.busqueda().categorias().contains("Tecnologia"));
        List<Models.Publicacion> encontradas = ui.busqueda().buscar(
                new Models.FiltrosBusqueda("java", "Todas", null, 0, "Menor precio"));
        assertEquals(1, encontradas.size());
        assertEquals("Ada Lovelace", encontradas.get(0).proveedor());
        assertEquals(Models.EstadoPublicacion.PUBLICADA, encontradas.get(0).estado());
        assertEquals(1, ui.busqueda().recomendar("beto").size());
        assertTrue(ui.busqueda().buscar(new Models.FiltrosBusqueda("", "Todas", null, 100, "Relevancia")).isEmpty(),
                "precio maximo 100 deja afuera la mentoria de 200");
    }

    @Test
    void elBotonAvanzarJuegaLosDosRolesYRespetaPermisos() {
        Models.Contratacion c = ui.contrataciones().solicitar("beto", mentoria.id(), "Aprender Java");
        assertEquals(Models.EstadoContratacion.PENDIENTE, c.estado());
        assertEquals("Ada Lovelace", c.contraparte());

        ErrorDeAplicacionException error = assertThrows(ErrorDeAplicacionException.class,
                () -> ui.contrataciones().avanzar("beto", c.id()), "el cliente no puede aceptar");
        assertEquals("ACCESO_DENEGADO", error.getCodigo());

        assertEquals(Models.EstadoContratacion.ACEPTADA, ui.contrataciones().avanzar("ada", c.id()).estado());
        assertEquals(Models.EstadoContratacion.CONFIRMADA, ui.contrataciones().avanzar("beto", c.id()).estado());
        assertEquals(800, ui.wallet().consultarSaldo("beto"));
        assertEquals(Models.EstadoContratacion.EN_CURSO, ui.contrataciones().avanzar("ada", c.id()).estado());
        assertEquals(Models.EstadoContratacion.COMPLETADA, ui.contrataciones().avanzar("ada", c.id()).estado());
        assertEquals(Models.EstadoContratacion.COMPLETADA, ui.contrataciones().avanzar("ada", c.id()).estado(),
                "COMPLETADA es terminal: avanzar no hace nada");

        assertEquals(1, ui.perfiles().verPerfil("ada").proyectosCompletados());
        assertEquals(List.of("Java"), ui.perfiles().verPerfil("ada").habilidades());
        assertFalse(ui.wallet().listarMovimientos("beto").isEmpty());
    }

    @Test
    void cancelarDelProveedorSobreUnaPendienteEsRechazar() {
        Models.Contratacion c = ui.contrataciones().solicitar("beto", mentoria.id(), "Aprender");
        assertEquals(Models.EstadoContratacion.RECHAZADA, ui.contrataciones().cancelar("ada", c.id()).estado());
        Models.Contratacion otra = ui.contrataciones().solicitar("beto", mentoria.id(), "Otra vez");
        ui.contrataciones().avanzar("ada", otra.id());
        assertEquals(Models.EstadoContratacion.CANCELADA, ui.contrataciones().cancelar("beto", otra.id()).estado());
    }

    @Test
    void misPublicacionesCrearYCambiarEstado() {
        Models.Publicacion borrador = ui.publicaciones().crearBorrador("ada", "Nueva", "desc", "Tecnologia",
                Models.Modalidad.REMOTA, 60, 150);
        assertEquals(Models.EstadoPublicacion.BORRADOR, borrador.estado());
        assertEquals(Models.EstadoPublicacion.PUBLICADA,
                ui.publicaciones().cambiarEstado("ada", borrador.id(), Models.EstadoPublicacion.PUBLICADA).estado(),
                "la interfaz agrega una franja por defecto, asi que se puede publicar");
        assertEquals(Models.EstadoPublicacion.PAUSADA,
                ui.publicaciones().cambiarEstado("ada", borrador.id(), Models.EstadoPublicacion.PAUSADA).estado());
        assertEquals(2, ui.publicaciones().listarPropias("ada").size());
        assertThrows(ErrorDeAplicacionException.class,
                () -> ui.publicaciones().cambiarEstado("beto", borrador.id(), Models.EstadoPublicacion.PUBLICADA));
    }

    @Test
    void notificacionesYWallet() {
        ui.contrataciones().solicitar("beto", mentoria.id(), "Aprender");
        List<Models.Notificacion> deAda = ui.notificaciones().listarNotificaciones("ada");
        assertTrue(deAda.stream().anyMatch(n -> n.titulo().equals("Nueva solicitud")));
        ui.notificaciones().marcarTodasLeidas("ada");
        assertTrue(ui.notificaciones().listarNotificaciones("ada").stream().allMatch(Models.Notificacion::leida));
        assertEquals(500, ui.wallet().acreditarDemo("ada", 500));
        assertEquals(1, ui.contrataciones().listarContrataciones("ada").size());
    }
}
