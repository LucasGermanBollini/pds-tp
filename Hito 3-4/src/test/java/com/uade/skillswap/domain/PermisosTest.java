package com.uade.skillswap.domain;

import com.uade.skillswap.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seccion 5: "Los controllers que modifican informacion deben validar
 * permisos a partir del userId". Cada accion la puede hacer solo quien
 * corresponde; el resto recibe AccesoDenegadoException y nada cambia.
 */
class PermisosTest {

    private Perfil proveedor;
    private Perfil cliente;
    private Publicacion publicacion;
    private FranjaDisponibilidad franja;

    @BeforeEach
    void setUp() {
        proveedor = new Perfil("prof-1", "Ada");
        cliente = new Perfil("cli-1", "Beto");
        publicacion = new Publicacion("pub-1", proveedor, "Mentoria");
        franja = new FranjaDisponibilidad("f-1", LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(1), 2);
        publicacion.completarDatos("desc", new CategoriaHabilidad("cat", "Carrera"), Modalidad.REMOTA, 60, SkillCoins.de(100));
        publicacion.agregarFranja(franja);
    }

    @Test
    void soloElAutorPuedePublicarPausarCerrarYCancelarSuPublicacion() {
        assertThrows(AccesoDenegadoException.class, () -> publicacion.publicar("intruso"));
        assertEquals("BORRADOR", publicacion.getEstadoNombre());

        publicacion.publicar("prof-1");
        assertThrows(AccesoDenegadoException.class, () -> publicacion.pausar("cli-1"));
        assertThrows(AccesoDenegadoException.class, () -> publicacion.cerrar("cli-1"));
        assertThrows(AccesoDenegadoException.class, () -> publicacion.cancelar("cli-1", "porque si"));
        assertEquals("PUBLICADA", publicacion.getEstadoNombre());
    }

    @Test
    void elClienteNoPuedeAceptarNiRechazarNiIniciarNiCompletar() {
        publicacion.publicar("prof-1");
        Contratacion c = new Contratacion("c-1", cliente, publicacion, franja, "hola", "objetivo");

        assertThrows(AccesoDenegadoException.class, () -> c.aceptar("cli-1"));
        assertThrows(AccesoDenegadoException.class, () -> c.rechazar("cli-1"));
        assertEquals("PENDIENTE", c.getEstadoNombre());
        assertEquals(2, franja.getCupoDisponible(), "un intento no autorizado no debe tocar el cupo");

        c.aceptar("prof-1");
        c.pagar("cli-1");
        assertThrows(AccesoDenegadoException.class, () -> c.iniciar("cli-1"));
        c.iniciar("prof-1");
        assertThrows(AccesoDenegadoException.class, () -> c.completar("cli-1"));
    }

    @Test
    void elProveedorNoPuedePagarPorElCliente() {
        publicacion.publicar("prof-1");
        Contratacion c = new Contratacion("c-1", cliente, publicacion, franja, "hola", "objetivo");
        c.aceptar("prof-1");
        assertThrows(AccesoDenegadoException.class, () -> c.pagar("prof-1"));
        assertEquals("ACEPTADA", c.getEstadoNombre());
    }

    @Test
    void unTerceroNoPuedeCancelarNiCalificar() {
        publicacion.publicar("prof-1");
        Contratacion c = new Contratacion("c-1", cliente, publicacion, franja, "hola", "objetivo");
        c.aceptar("prof-1");
        assertThrows(AccesoDenegadoException.class, () -> c.cancelar("intruso", "motivo"));

        c.pagar("cli-1");
        c.iniciar("prof-1");
        c.completar("prof-1");
        assertThrows(AccesoDenegadoException.class, () -> c.calificar("cal-1", "intruso", 5, "x"));
    }

    @Test
    void laCalificacionVaSiempreALaOtraParteYSoloEnCompletada() {
        publicacion.publicar("prof-1");
        Contratacion c = new Contratacion("c-1", cliente, publicacion, franja, "hola", "objetivo");
        c.aceptar("prof-1");
        assertThrows(IllegalStateException.class, () -> c.calificar("cal-1", "cli-1", 5, "antes de tiempo"));

        c.pagar("cli-1");
        c.iniciar("prof-1");
        c.completar("prof-1");
        Calificacion delCliente = c.calificar("cal-1", "cli-1", 5, "genial");
        Calificacion delProveedor = c.calificar("cal-2", "prof-1", 4, "puntual");
        assertEquals("prof-1", delCliente.getReceptorUserId());
        assertEquals("cli-1", delProveedor.getReceptorUserId());
    }

    @Test
    void elDoblePagoSeRechazaEnElDominio() {
        publicacion.publicar("prof-1");
        Contratacion c = new Contratacion("c-1", cliente, publicacion, franja, "hola", "objetivo");
        c.aceptar("prof-1");
        c.pagar("cli-1");
        c.registrarMontoPagado(SkillCoins.de(100));
        assertThrows(IllegalStateException.class, () -> c.pagar("cli-1"));
        assertEquals("CONFIRMADA", c.getEstadoNombre());
    }
}
