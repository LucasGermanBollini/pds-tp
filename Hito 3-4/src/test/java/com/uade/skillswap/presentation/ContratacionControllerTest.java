package com.uade.skillswap.presentation;

import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.NotificacionDTO;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.presentation.controller.Resultado;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracion controllers + casos de uso (seccion 12) sobre repositorios en
 * memoria, con foco en los casos negativos que pide la consigna: transicion
 * invalida, saldo insuficiente, cupo agotado, doble pago y calificacion no
 * autorizada. Todos los errores vuelven con el mismo formato (Resultado).
 */
class ContratacionControllerTest {

    private Escenario e;
    private PublicacionDTO mentoria;

    @BeforeEach
    void setUp() {
        e = new Escenario();
        e.perfil("ada", "Ada", "PROFESIONAL", "EMAIL", 0);
        e.perfil("beto", "Beto", "PROFESIONAL", "PUSH", 1000);
        e.perfil("clara", "Clara", "PROFESIONAL", "INTERNA", 1000);
        e.perfil("pobre", "Sin saldo", "PROFESIONAL", "INTERNA", 50);
        mentoria = e.publicar("ada", "Mentoria", "Carrera", "REMOTA", 300, List.of("Java"),
                List.of(e.franja(0, 1), e.franja(2, 1)));
    }

    private ContratacionDTO solicitar(String cliente, String franjaId) {
        return e.app.contrataciones().solicitar(cliente, new SolicitarComando(mentoria.id(), franjaId, "hola", "aprender"))
                .valorOLanzar();
    }

    private String franja(int i) {
        return mentoria.franjas().get(i).id();
    }

    @Test
    void recorridoCompletoPublicarSolicitarAceptarPagarCompletarCalificar() {
        ContratacionDTO c = solicitar("beto", franja(0));
        assertEquals("PENDIENTE", c.estado());
        assertEquals("ACEPTADA", e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar().estado());
        assertEquals(300L, e.app.contrataciones().pagar("beto", c.id()).valorOLanzar().montoPagado());
        e.app.contrataciones().iniciar("ada", c.id()).valorOLanzar();
        assertEquals("COMPLETADA", e.app.contrataciones().completar("ada", c.id()).valorOLanzar().estado());
        e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 4, "Muy buena")).valorOLanzar();

        assertEquals(700L, e.app.wallet().consultarSaldo("beto").valorOLanzar());
        assertEquals(300L, e.app.wallet().consultarSaldo("ada").valorOLanzar());
        assertEquals(4.0, e.app.perfiles().verPerfil("ada").valorOLanzar().reputacion());
        assertEquals(-300L, e.app.wallet().listarMovimientos("beto", 0, 10).valorOLanzar().contenido().get(0).importe());
    }

    @Test
    void transicionInvalidaDevuelveOperacionNoPermitida() {
        ContratacionDTO c = solicitar("beto", franja(0));
        Resultado<ContratacionDTO> r = e.app.contrataciones().completar("ada", c.id());
        assertFalse(r.exito());
        assertEquals("OPERACION_NO_PERMITIDA", r.codigoError());
        assertTrue(r.mensaje().contains("PENDIENTE"));
    }

    @Test
    void saldoInsuficienteNoModificaSaldosNiEstado() {
        ContratacionDTO c = solicitar("pobre", franja(0));
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        Resultado<ContratacionDTO> r = e.app.contrataciones().pagar("pobre", c.id());
        assertEquals("OPERACION_NO_PERMITIDA", r.codigoError());
        assertEquals(50L, e.app.wallet().consultarSaldo("pobre").valorOLanzar());
        assertEquals(0L, e.app.wallet().consultarSaldo("ada").valorOLanzar());
        assertEquals("ACEPTADA", e.app.contrataciones().verComprobante("pobre", c.id()).valorOLanzar().estado());
    }

    @Test
    void cupoAgotadoNoPermiteUnaSegundaAceptacion() {
        ContratacionDTO c1 = solicitar("beto", franja(0));
        ContratacionDTO c2 = solicitar("clara", franja(0));
        e.app.contrataciones().aceptar("ada", c1.id()).valorOLanzar();
        Resultado<ContratacionDTO> r = e.app.contrataciones().aceptar("ada", c2.id());
        assertEquals("OPERACION_NO_PERMITIDA", r.codigoError());

        Resultado<ContratacionDTO> nueva = e.app.contrataciones().solicitar("pobre",
                new SolicitarComando(mentoria.id(), franja(0), "", ""));
        assertEquals("OPERACION_NO_PERMITIDA", nueva.codigoError(), "una franja sin cupo esta ocupada: no admite solicitudes");
    }

    @Test
    void doblePagoNoCobraDosVeces() {
        ContratacionDTO c = solicitar("beto", franja(0));
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().pagar("beto", c.id()).valorOLanzar();
        Resultado<ContratacionDTO> segundo = e.app.contrataciones().pagar("beto", c.id());
        assertEquals("OPERACION_NO_PERMITIDA", segundo.codigoError());
        assertEquals(700L, e.app.wallet().consultarSaldo("beto").valorOLanzar());
        assertEquals(300L, e.app.wallet().consultarSaldo("ada").valorOLanzar());
    }

    @Test
    void calificacionNoAutorizada() {
        ContratacionDTO c = solicitar("beto", franja(0));
        assertEquals("OPERACION_NO_PERMITIDA",
                e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 5, "")).codigoError(),
                "no se califica antes de completar");

        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().pagar("beto", c.id()).valorOLanzar();
        e.app.contrataciones().iniciar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().completar("ada", c.id()).valorOLanzar();

        assertEquals("ACCESO_DENEGADO",
                e.app.calificaciones().calificar("clara", new CalificarComando(c.id(), 1, "")).codigoError(),
                "un tercero no puede calificar");
        assertEquals("DATOS_INVALIDOS",
                e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 9, "")).codigoError(),
                "el puntaje va de 1 a 5");
        e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 5, "")).valorOLanzar();
        assertEquals("OPERACION_NO_PERMITIDA",
                e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 5, "")).codigoError(),
                "una sola vez por parte");
        assertEquals(1, e.app.calificaciones().listarRecibidas("ada", 0, 10).valorOLanzar().totalElementos());
    }

    @Test
    void permisosSeRespetanEnCadaPaso() {
        ContratacionDTO c = solicitar("beto", franja(0));
        assertEquals("ACCESO_DENEGADO", e.app.contrataciones().aceptar("beto", c.id()).codigoError());
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        assertEquals("ACCESO_DENEGADO", e.app.contrataciones().pagar("ada", c.id()).codigoError());
        assertEquals("ACCESO_DENEGADO", e.app.contrataciones().cancelar("clara", c.id(), "x").codigoError());
        assertEquals("ACCESO_DENEGADO", e.app.contrataciones().verComprobante("clara", c.id()).codigoError());
        assertEquals("ACCESO_DENEGADO", e.app.publicaciones().cambiarEstado("beto", mentoria.id(), "PAUSADA", null).codigoError());
        assertEquals("ACCESO_DENEGADO", e.app.contrataciones().aceptar(" ", c.id()).codigoError(),
                "un userId que el servicio de identidad no reconoce");
        assertEquals("NO_ENCONTRADO", e.app.contrataciones().aceptar("ada", "no-existe").codigoError());
    }

    @Test
    void elAutorNoPuedeSolicitarSuPropiaPublicacion() {
        Resultado<ContratacionDTO> r = e.app.contrataciones().solicitar("ada",
                new SolicitarComando(mentoria.id(), franja(0), "", ""));
        assertEquals("DATOS_INVALIDOS", r.codigoError());
    }

    @Test
    void elClienteNoPuedeTenerDosSesionesQueSePisan() {
        PublicacionDTO otra = e.publicar("ada", "Otra mentoria", "Carrera", "REMOTA", 100, List.of(),
                List.of(e.franja(0, 3)));
        solicitar("beto", franja(0));
        Resultado<ContratacionDTO> pisada = e.app.contrataciones().solicitar("beto",
                new SolicitarComando(otra.id(), otra.franjas().get(0).id(), "", ""));
        assertEquals("OPERACION_NO_PERMITIDA", pisada.codigoError());
        assertTrue(pisada.mensaje().contains("ocupada"));
        Resultado<ContratacionDTO> repetida = e.app.contrataciones().solicitar("beto",
                new SolicitarComando(mentoria.id(), franja(0), "", ""));
        assertEquals("OPERACION_NO_PERMITIDA", repetida.codigoError());
    }

    @Test
    void cancelarAplicaLaPoliticaDeReintegroYDejaMovimientos() {
        ContratacionDTO c = solicitar("beto", franja(0));
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().pagar("beto", c.id()).valorOLanzar();
        // la franja empieza en mas de 48 hs: la politica Estandar devuelve el 100 %
        ContratacionDTO cancelada = e.app.contrataciones().cancelar("beto", c.id(), "me surgio algo").valorOLanzar();
        assertEquals("CANCELADA", cancelada.estado());
        assertEquals("me surgio algo", cancelada.motivoCancelacion());
        assertEquals(1000L, e.app.wallet().consultarSaldo("beto").valorOLanzar());
        assertEquals(0L, e.app.wallet().consultarSaldo("ada").valorOLanzar());
        assertEquals(1, e.app.publicaciones().verDetalle(mentoria.id()).valorOLanzar().franjas().get(0).cupoDisponible(),
                "cancelar libera el cupo");
    }

    @Test
    void reprogramacionDeComunAcuerdo() {
        ContratacionDTO c = solicitar("beto", franja(0));
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        ContratacionDTO propuesta = e.app.contrataciones().proponerReprogramacion("beto", c.id(), franja(1)).valorOLanzar();
        assertEquals(franja(1), propuesta.franjaPropuesta().id());
        assertEquals("ACCESO_DENEGADO", e.app.contrataciones().aceptarReprogramacion("beto", c.id()).codigoError());

        ContratacionDTO reprogramada = e.app.contrataciones().aceptarReprogramacion("ada", c.id()).valorOLanzar();
        assertEquals(franja(1), reprogramada.franja().id());
        assertNull(reprogramada.franjaPropuesta());
        assertTrue(notificacionesDe("ada").stream().anyMatch(n -> "REPROGRAMACION_PROPUESTA".equals(n.tipoEvento())));
        assertTrue(notificacionesDe("beto").stream().anyMatch(n -> "REPROGRAMACION_ACEPTADA".equals(n.tipoEvento())));
    }

    /** Caso de aceptacion de la seccion 15: "Enviar dos tipos de notificacion ante un mismo evento mediante adaptadores mock". */
    @Test
    void unMismoEventoSaleporDosCanalesSegunLaPreferenciaDeCadaParte() {
        ContratacionDTO c = solicitar("beto", franja(0));
        e.canales.envios.clear();
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();

        List<CanalesEspia.Envio> envios = e.canales.envios;
        assertEquals(2, envios.size(), "un evento (aceptacion), dos destinatarios");
        assertTrue(envios.contains(new CanalesEspia.Envio(TipoCanalNotificacion.EMAIL, "ada", envios.get(0).mensaje())));
        assertTrue(envios.contains(new CanalesEspia.Envio(TipoCanalNotificacion.PUSH, "beto", envios.get(0).mensaje())));
    }

    @Test
    void laSolicitudLeLlegaAlProveedorYSeRegistraEnElHistorial() {
        ContratacionDTO c = solicitar("beto", franja(0));
        assertTrue(notificacionesDe("ada").stream().anyMatch(n -> "SOLICITUD_RECIBIDA".equals(n.tipoEvento())));
        assertTrue(notificacionesDe("beto").stream().noneMatch(n -> "SOLICITUD_RECIBIDA".equals(n.tipoEvento())));
        var historial = e.repos.historial().buscarPorEntidad("CONTRATACION", c.id());
        assertEquals(1, historial.size());
        assertNull(historial.get(0).getEstadoAnterior());
        assertEquals("PENDIENTE", historial.get(0).getEstadoNuevo());
        assertEquals("beto", historial.get(0).getActorUserId());
    }

    @Test
    void lasNotificacionesSePaginanYSeMarcanLeidasSoloPorSuDuenio() {
        ContratacionDTO c = solicitar("beto", franja(0));
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        var pagina = e.app.notificaciones().listar("ada", 0, 1).valorOLanzar();
        assertEquals(1, pagina.contenido().size());
        assertTrue(pagina.totalElementos() >= 2);
        String id = pagina.contenido().get(0).id();
        assertEquals("ACCESO_DENEGADO", e.app.notificaciones().marcarLeida("beto", id).codigoError());
        e.app.notificaciones().marcarLeida("ada", id).valorOLanzar();
        e.app.notificaciones().marcarTodasLeidas("ada").valorOLanzar();
        assertTrue(notificacionesDe("ada").stream().allMatch(NotificacionDTO::leida));
    }

    private List<NotificacionDTO> notificacionesDe(String userId) {
        return e.app.notificaciones().listar(userId, 0, 100).valorOLanzar().contenido();
    }
}
