package com.uade.skillswap;

import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.PerfilPublicoDTO;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.config.AppFactory;
import com.uade.skillswap.presentation.controller.Resultado;
import com.uade.skillswap.presentation.controller.comando.BuscarPublicacionesComando;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;
import com.uade.skillswap.presentation.controller.comando.CrearPerfilComando;
import com.uade.skillswap.presentation.controller.comando.CrearPublicacionComando;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * El recorrido principal que pide la seccion 13 para la demo en vivo:
 * publicar -> solicitar -> aceptar -> pagar -> completar -> calificar, mas
 * dos casos negativos (permisos y doble calificacion). Pasa SOLO por los
 * controllers de la seccion 9, igual que lo haria la interfaz.
 */
public final class RecorridoDemo {

    private RecorridoDemo() {
    }

    public static void ejecutar(AppFactory.Aplicacion app) {
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        String ada = "prof-ada-" + sufijo;
        String beto = "cli-beto-" + sufijo;

        paso("0) Perfiles (validados contra el servicio de identidad mock) y saldo inicial");
        ok(app.perfiles().crearPerfil(new CrearPerfilComando(ada, "Ada, mentora de carrera", "PROFESIONAL",
                "Ex-devops, prepara entrevistas tecnicas", "America/Argentina/Buenos_Aires", "Mentora", "EMAIL")));
        ok(app.perfiles().crearPerfil(new CrearPerfilComando(beto, "Beto", "PROFESIONAL",
                "Busca su primer trabajo", "America/Argentina/Buenos_Aires", "Dev Jr", "PUSH")));
        ok(app.wallet().acreditarDemo(beto, 1000));

        paso("1) Ada publica una mentoria (BORRADOR -> PUBLICADA)");
        LocalDateTime inicio = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);
        PublicacionDTO publicacion = ok(app.publicaciones().crearBorrador(ada, new CrearPublicacionComando(
                "Preparacion para entrevistas tecnicas " + sufijo, "Simulacro de entrevista backend + feedback",
                "Carrera", "REMOTA", 60, 300, List.of("Java", "Entrevistas"),
                List.of(new CrearPublicacionComando.Franja(inicio, inicio.plusHours(1), 1)), "OFERTA")));
        publicacion = ok(app.publicaciones().cambiarEstado(ada, publicacion.id(), "PUBLICADA", null));
        System.out.println("   estado: " + publicacion.estado());

        paso("   ... y Beto la encuentra con una busqueda filtrada");
        List<PublicacionDTO> encontradas = ok(app.busqueda().buscar(BuscarPublicacionesComando.porTexto(sufijo))).contenido();
        System.out.println("   resultados para '" + sufijo + "': " + encontradas.size());

        paso("2) Beto solicita la mentoria");
        ContratacionDTO c = ok(app.contrataciones().solicitar(beto,
                new SolicitarComando(publicacion.id(), null, "Hola Ada!", "Conseguir trabajo de dev junior")));
        System.out.println("   estado: " + c.estado());

        paso("   (negativo) Beto intenta aceptar su propia solicitud");
        Resultado<ContratacionDTO> intento = app.contrataciones().aceptar(beto, c.id());
        System.out.println("   -> " + intento.codigoError() + ": " + intento.mensaje());

        paso("3) Ada acepta (se bloquea el cupo en la misma transaccion)");
        c = ok(app.contrataciones().aceptar(ada, c.id()));
        System.out.println("   estado: " + c.estado() + " | cupo disponible de la franja: " + c.franja().cupoDisponible());

        paso("4) Beto paga con SkillCoins (debito + credito en una sola transaccion)");
        c = ok(app.contrataciones().pagar(beto, c.id()));
        System.out.println("   estado: " + c.estado() + " | saldo Beto: " + ok(app.wallet().consultarSaldo(beto))
                + " | saldo Ada: " + ok(app.wallet().consultarSaldo(ada)));

        paso("5) Ada inicia y completa la sesion");
        ok(app.contrataciones().iniciar(ada, c.id()));
        c = ok(app.contrataciones().completar(ada, c.id()));
        System.out.println("   estado: " + c.estado());

        paso("6) Beto califica a Ada (y Ada a Beto)");
        ok(app.calificaciones().calificar(beto, new CalificarComando(c.id(), 5, "Excelente mentoria")));
        ok(app.calificaciones().calificar(ada, new CalificarComando(c.id(), 4, "Muy buena predisposicion")));
        Resultado<?> segunda = app.calificaciones().calificar(beto, new CalificarComando(c.id(), 1, "otra vez"));
        System.out.println("   (negativo) segunda calificacion de Beto -> " + segunda.codigoError() + ": " + segunda.mensaje());

        PerfilPublicoDTO perfilAda = ok(app.perfiles().verPerfil(ada));
        System.out.println("   reputacion recalculada de Ada: " + perfilAda.reputacion()
                + " (" + perfilAda.cantidadCalificaciones() + " calificacion/es), sesiones dadas: "
                + perfilAda.historial().sesionesCompletadasComoProveedor());

        paso("Notificaciones de Beto (canal PUSH) y de Ada (canal EMAIL)");
        ok(app.notificaciones().listar(beto, 0, 20)).contenido()
                .forEach(n -> System.out.println("   [" + n.canal() + "] " + n.mensaje()));
    }

    private static <T> T ok(Resultado<T> resultado) {
        return resultado.valorOLanzar();
    }

    private static void paso(String texto) {
        System.out.println();
        System.out.println(texto);
    }
}
