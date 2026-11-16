package com.uade.skillswap.domain;

import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.state.EstadoContratacionFactory;
import com.uade.skillswap.domain.model.state.EstadoPublicacionFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las dos maquinas de estado completas (secciones 4.2 y 4.5): para CADA
 * estado se prueba CADA accion, y solo las de la tabla de la consigna pueden
 * funcionar; el resto las rechaza el dominio sin cambiar el estado.
 */
class MatrizDeEstadosTest {

    // ---------------- Publicacion (seccion 4.2) ----------------

    private static final Map<String, Set<String>> PUBLICACION_PERMITIDAS = Map.of(
            "BORRADOR", Set.of("publicar", "cancelar"),
            "PUBLICADA", Set.of("pausar", "cerrar", "cancelar"),
            "PAUSADA", Set.of("publicar", "cerrar", "cancelar"),
            "CERRADA", Set.of(),
            "CANCELADA", Set.of());

    private static final Map<String, Consumer<Publicacion>> ACCIONES_PUBLICACION = Map.of(
            "publicar", p -> p.publicar("prof-1"),
            "pausar", p -> p.pausar("prof-1"),
            "cerrar", p -> p.cerrar("prof-1"),
            "cancelar", p -> p.cancelar("prof-1", "motivo"));

    private Publicacion publicacionEn(String estado) {
        Publicacion p = new Publicacion("pub", new Perfil("prof-1", "Ada"), "Mentoria");
        p.completarDatos("d", new CategoriaHabilidad("c", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(10));
        p.agregarFranja(new FranjaDisponibilidad("f", LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1), 1));
        switch (estado) {
            case "PUBLICADA" -> p.publicar("prof-1");
            case "PAUSADA" -> { p.publicar("prof-1"); p.pausar("prof-1"); }
            case "CERRADA" -> { p.publicar("prof-1"); p.cerrar("prof-1"); }
            case "CANCELADA" -> p.cancelar("prof-1", "motivo");
            default -> { }
        }
        assertEquals(estado, p.getEstadoNombre());
        return p;
    }

    @Test
    void publicacionCadaEstadoSoloAdmiteSusTransiciones() {
        PUBLICACION_PERMITIDAS.forEach((estado, permitidas) -> ACCIONES_PUBLICACION.forEach((accion, ejecutar) -> {
            Publicacion p = publicacionEn(estado);
            if (permitidas.contains(accion)) {
                assertDoesNotThrow(() -> ejecutar.accept(p), estado + " debe admitir " + accion);
                assertNotEquals(estado, p.getEstadoNombre());
            } else {
                assertThrows(IllegalStateException.class, () -> ejecutar.accept(p), estado + " no debe admitir " + accion);
                assertEquals(estado, p.getEstadoNombre());
            }
        }));
    }

    // ---------------- Contratacion (seccion 4.5) ----------------

    private static final Map<String, Set<String>> CONTRATACION_PERMITIDAS = Map.of(
            "PENDIENTE", Set.of("aceptar", "rechazar"),
            "ACEPTADA", Set.of("pagar", "cancelar", "reprogramar"),
            "CONFIRMADA", Set.of("iniciar", "cancelar", "reprogramar"),
            "EN_CURSO", Set.of("completar"),
            "COMPLETADA", Set.of("calificar"),
            "RECHAZADA", Set.of(),
            "CANCELADA", Set.of());

    private FranjaDisponibilidad otraFranja;

    private final Map<String, Consumer<Contratacion>> accionesContratacion = Map.of(
            "aceptar", c -> c.aceptar("prof-1"),
            "rechazar", c -> c.rechazar("prof-1"),
            "pagar", c -> c.pagar("cli-1"),
            "iniciar", c -> c.iniciar("prof-1"),
            "completar", c -> c.completar("prof-1"),
            "cancelar", c -> c.cancelar("cli-1", "motivo"),
            "reprogramar", c -> c.proponerReprogramacion("cli-1", otraFranja),
            "calificar", c -> c.calificar("cal", "cli-1", 5, "ok"));

    private Contratacion contratacionEn(String estado) {
        Publicacion p = new Publicacion("pub", new Perfil("prof-1", "Ada"), "Mentoria");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("f1", LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1), 2);
        otraFranja = new FranjaDisponibilidad("f2", LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(4).plusHours(1), 2);
        p.completarDatos("d", new CategoriaHabilidad("c", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(10));
        p.agregarFranja(franja);
        p.agregarFranja(otraFranja);
        p.publicar("prof-1");
        Contratacion c = new Contratacion("c", new Perfil("cli-1", "Beto"), p, franja, "", "");
        List<String> camino = switch (estado) {
            case "ACEPTADA" -> List.of("aceptar");
            case "CONFIRMADA" -> List.of("aceptar", "pagar");
            case "EN_CURSO" -> List.of("aceptar", "pagar", "iniciar");
            case "COMPLETADA" -> List.of("aceptar", "pagar", "iniciar", "completar");
            case "RECHAZADA" -> List.of("rechazar");
            case "CANCELADA" -> List.of("aceptar", "cancelar");
            default -> List.of();
        };
        camino.forEach(a -> accionesContratacion.get(a).accept(c));
        assertEquals(estado, c.getEstadoNombre());
        return c;
    }

    @Test
    void contratacionCadaEstadoSoloAdmiteLasAccionesDeLaTabla() {
        CONTRATACION_PERMITIDAS.forEach((estado, permitidas) -> accionesContratacion.keySet().forEach(accion -> {
            Contratacion c = contratacionEn(estado);
            Consumer<Contratacion> ejecutar = accionesContratacion.get(accion);
            if (permitidas.contains(accion)) {
                assertDoesNotThrow(() -> ejecutar.accept(c), estado + " debe admitir " + accion);
            } else {
                assertThrows(IllegalStateException.class, () -> ejecutar.accept(c), estado + " no debe admitir " + accion);
                assertEquals(estado, c.getEstadoNombre());
            }
        }));
    }

    @Test
    void aceptarUnaReprogramacionTambienDependeDelEstado() {
        Contratacion c = contratacionEn("CONFIRMADA");
        c.proponerReprogramacion("cli-1", otraFranja);
        c.aceptarReprogramacion("prof-1");
        assertEquals(otraFranja, c.getFranjaElegida());

        Contratacion enCurso = contratacionEn("ACEPTADA");
        enCurso.proponerReprogramacion("cli-1", otraFranja);
        enCurso.pagar("cli-1");
        enCurso.iniciar("prof-1");
        assertThrows(IllegalStateException.class, () -> enCurso.aceptarReprogramacion("prof-1"),
                "una propuesta que quedo pendiente no se puede aceptar una vez comenzada la sesion");
    }

    @Test
    void lasFactoriesReconstruyenCadaEstadoDesdeSuNombre() {
        PUBLICACION_PERMITIDAS.keySet().forEach(n -> assertEquals(n, EstadoPublicacionFactory.desdeNombre(n).getNombre()));
        CONTRATACION_PERMITIDAS.keySet().forEach(n -> assertEquals(n, EstadoContratacionFactory.desdeNombre(n).getNombre()));
        assertEquals("BORRADOR", EstadoPublicacionFactory.desdeNombre(null).getNombre());
        assertEquals("PENDIENTE", EstadoContratacionFactory.desdeNombre("").getNombre());
        assertThrows(IllegalArgumentException.class, () -> EstadoPublicacionFactory.desdeNombre("VOLANDO"));
        assertThrows(IllegalArgumentException.class, () -> EstadoContratacionFactory.desdeNombre("VOLANDO"));
    }

    @Test
    void unitOfWorkDirectoEjecutaLaAccion() {
        int[] veces = {0};
        UnitOfWork.DIRECTO.ejecutar(() -> {
            veces[0]++;
        });
        assertEquals(1, veces[0]);
        assertEquals("ok", UnitOfWork.DIRECTO.ejecutar(() -> "ok"));
    }
}
