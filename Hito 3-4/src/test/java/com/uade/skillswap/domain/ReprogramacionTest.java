package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.CambioEstadoEvento;
import com.uade.skillswap.domain.model.observer.TipoEventoDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Seccion 4.4: "Una reserva puede reprogramarse antes de comenzar si ambas partes aceptan". */
class ReprogramacionTest {

    private FranjaDisponibilidad original;
    private FranjaDisponibilidad otra;
    private Contratacion contratacion;
    private final List<CambioEstadoEvento> eventos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Perfil proveedor = new Perfil("prof-1", "Ada");
        Publicacion publicacion = new Publicacion("pub-1", proveedor, "Mentoria");
        original = new FranjaDisponibilidad("f-1", LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1), 1);
        otra = new FranjaDisponibilidad("f-2", LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(1), 1);
        publicacion.completarDatos("desc", new CategoriaHabilidad("cat", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(100));
        publicacion.agregarFranja(original);
        publicacion.agregarFranja(otra);
        publicacion.publicar("prof-1");
        contratacion = new Contratacion("c-1", new Perfil("cli-1", "Beto"), publicacion, original, "hola", "obj");
        contratacion.agregarObservador(eventos::add);
        contratacion.aceptar("prof-1");
    }

    @Test
    void unaParteProponeYLaOtraAceptaYElCupoSeMueve() {
        contratacion.proponerReprogramacion("cli-1", otra);
        assertEquals(original, contratacion.getFranjaElegida(), "proponer no cambia nada todavia");
        assertEquals(otra, contratacion.getFranjaPropuesta());

        contratacion.aceptarReprogramacion("prof-1");

        assertEquals(otra, contratacion.getFranjaElegida());
        assertNull(contratacion.getFranjaPropuesta());
        assertEquals(1, original.getCupoDisponible(), "se libera el cupo de la franja vieja");
        assertEquals(0, otra.getCupoDisponible(), "se reserva el cupo de la nueva");
        assertTrue(eventos.stream().anyMatch(e -> e.getTipo() == TipoEventoDominio.REPROGRAMACION_PROPUESTA));
        assertTrue(eventos.stream().anyMatch(e -> e.getTipo() == TipoEventoDominio.REPROGRAMACION_ACEPTADA));
    }

    @Test
    void quienProponeNoPuedeAceptarSuPropiaPropuesta() {
        contratacion.proponerReprogramacion("cli-1", otra);
        assertThrows(AccesoDenegadoException.class, () -> contratacion.aceptarReprogramacion("cli-1"));
        assertEquals(original, contratacion.getFranjaElegida());
    }

    @Test
    void laOtraPartepuedeRechazarYNadaCambia() {
        contratacion.proponerReprogramacion("prof-1", otra);
        contratacion.rechazarReprogramacion("cli-1");
        assertEquals(original, contratacion.getFranjaElegida());
        assertNull(contratacion.getFranjaPropuesta());
        assertEquals(1, otra.getCupoDisponible());
        assertTrue(eventos.stream().anyMatch(e -> e.getTipo() == TipoEventoDominio.REPROGRAMACION_RECHAZADA));
    }

    @Test
    void noSePuedeReprogramarUnaVezComenzada() {
        contratacion.pagar("cli-1");
        contratacion.iniciar("prof-1");
        assertThrows(IllegalStateException.class, () -> contratacion.proponerReprogramacion("cli-1", otra));
    }

    @Test
    void noSePuedeProponerUnaFranjaSinCupoNiDosPropuestasALaVez() {
        otra.reservarCupo();
        assertThrows(IllegalStateException.class, () -> contratacion.proponerReprogramacion("cli-1", otra));
        otra.liberarCupo();
        contratacion.proponerReprogramacion("cli-1", otra);
        assertThrows(IllegalStateException.class, () -> contratacion.proponerReprogramacion("prof-1", otra));
    }

    @Test
    void unTerceroNoPuedeProponerNiResponder() {
        assertThrows(AccesoDenegadoException.class, () -> contratacion.proponerReprogramacion("intruso", otra));
        contratacion.proponerReprogramacion("cli-1", otra);
        assertThrows(AccesoDenegadoException.class, () -> contratacion.aceptarReprogramacion("intruso"));
    }

    @Test
    void cancelarDescartaLaPropuestaPendiente() {
        contratacion.proponerReprogramacion("cli-1", otra);
        contratacion.cancelar("cli-1", "no puedo");
        assertNull(contratacion.getFranjaPropuesta());
        assertEquals("CANCELADA", contratacion.getEstadoNombre());
    }
}
