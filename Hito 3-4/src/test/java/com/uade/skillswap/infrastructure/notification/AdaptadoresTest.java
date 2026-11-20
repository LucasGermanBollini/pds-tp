package com.uade.skillswap.infrastructure.notification;

import com.uade.skillswap.application.port.CanalNotificacionPort;
import com.uade.skillswap.domain.model.AccesoDenegadoException;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.infrastructure.identidad.IdentidadExternaSdk;
import com.uade.skillswap.infrastructure.identidad.IdentidadServiceAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seccion 7 (Adapter, Simple Factory): cada adaptador traduce el contrato de
 * la aplicacion (enviar(userId, mensaje)) a la API incompatible de su "SDK".
 * Los SDK se reemplazan por espias para ver exactamente que reciben.
 */
class AdaptadoresTest {

    static class EmailSdkEspia extends EmailExternoSdk {
        final List<String> llamadas = new ArrayList<>();

        @Override
        public String despacharCorreo(String direccionDestino, String asunto, String cuerpo) {
            llamadas.add(direccionDestino + "|" + asunto + "|" + cuerpo);
            return "EMAIL-TEST";
        }
    }

    static class PushSdkEspia extends PushExternoSdk {
        final List<String> tokens = new ArrayList<>();
        final List<Map<String, String>> payloads = new ArrayList<>();

        @Override
        public String enviarPush(String tokenDispositivo, Map<String, String> payload) {
            tokens.add(tokenDispositivo);
            payloads.add(payload);
            return "PUSH-TEST";
        }
    }

    @Test
    void elAdapterDeEmailConvierteElUserIdEnDireccionYAgregaAsunto() {
        EmailSdkEspia sdk = new EmailSdkEspia();
        new EmailNotificacionAdapter(sdk).enviar("cli-1", "Tu pago fue confirmado");
        assertEquals(List.of("cli-1@skillswap.test|Novedades en SkillSwap|Tu pago fue confirmado"), sdk.llamadas);
    }

    @Test
    void elAdapterDePushArmaElTokenYElPayload() {
        PushSdkEspia sdk = new PushSdkEspia();
        new PushNotificacionAdapter(sdk).enviar("cli-1", "Nueva solicitud");
        assertEquals(List.of("device-token-cli-1"), sdk.tokens);
        assertEquals("Nueva solicitud", sdk.payloads.get(0).get("cuerpo"));
    }

    @Test
    void losSdkSimuladosRechazanDestinosVacios() {
        assertThrows(IllegalArgumentException.class, () -> new EmailExternoSdk().despacharCorreo("", "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new PushExternoSdk().enviarPush(" ", Map.of()));
        assertTrue(new EmailExternoSdk().despacharCorreo("x@y", "a", "b").startsWith("EMAIL-"));
        assertTrue(new PushExternoSdk().enviarPush("token", Map.of()).startsWith("PUSH-"));
    }

    @Test
    void laFactoryDevuelveElAdapterDeCadaCanalYPorDefectoElInterno() {
        CanalNotificacionFactoryImpl factory = new CanalNotificacionFactoryImpl();
        assertInstanceOf(EmailNotificacionAdapter.class, factory.crear(TipoCanalNotificacion.EMAIL));
        assertInstanceOf(PushNotificacionAdapter.class, factory.crear(TipoCanalNotificacion.PUSH));
        CanalNotificacionPort interna = factory.crear(TipoCanalNotificacion.INTERNA);
        assertInstanceOf(NotificacionInternaAdapter.class, interna);
        assertSame(interna, factory.crear(null));
        assertDoesNotThrow(() -> interna.enviar("cli-1", "hola"));
    }

    @Test
    void elAdapterDeIdentidadTraduceElCodigoDeEstadoAUnaExcepcion() {
        IdentidadServiceAdapter identidad = new IdentidadServiceAdapter(new IdentidadExternaSdk());
        assertDoesNotThrow(() -> identidad.validarUsuarioActivo("user-mateo"));
        assertThrows(AccesoDenegadoException.class, () -> identidad.validarUsuarioActivo(" "));
        assertThrows(AccesoDenegadoException.class, () -> identidad.validarUsuarioActivo(null));

        IdentidadServiceAdapter conUsuarioSuspendido = new IdentidadServiceAdapter(new IdentidadExternaSdk() {
            @Override
            public String consultarEstado(String userId) {
                return "SUSPENDIDO";
            }
        });
        assertThrows(AccesoDenegadoException.class, () -> conUsuarioSuspendido.validarUsuarioActivo("user-mateo"));
    }
}
