package com.uade.skillswap.config;

import com.uade.skillswap.presentation.controller.comando.CrearPerfilComando;
import com.uade.skillswap.presentation.controller.comando.CrearPublicacionComando;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Datos de demostracion reproducibles (seccion 8) para la interfaz: se crean
 * usando los mismos controllers que usa la UI, asi que pasan por todas las
 * reglas de negocio. Solo se siembran si la base no tiene a "user-mateo".
 *
 * <ul>
 *   <li>user-mateo: Mateo Silva (profesional; es el usuario del Mock UI Kit).</li>
 *   <li>prof-sofia: Sofia Mendez (profesional, mentora).</li>
 *   <li>org-techcorp: TechCorp (organizacion, publica un desafio).</li>
 * </ul>
 */
public final class DatosDemo {

    public static final String MATEO = "user-mateo";
    public static final String SOFIA = "prof-sofia";
    public static final String TECHCORP = "org-techcorp";

    private DatosDemo() {
    }

    public static void sembrarSiHaceFalta(AppFactory.Aplicacion app) {
        if (app.perfiles().verPerfil(MATEO).exito()) {
            return;
        }
        app.perfiles().crearPerfil(new CrearPerfilComando(MATEO, "Mateo Silva", "PROFESIONAL",
                "Construyo productos digitales y acompano a personas que quieren crecer en tecnologia.",
                "America/Argentina/Buenos_Aires", "Software Engineer & Mentor", "INTERNA")).valorOLanzar();
        app.perfiles().agregarHabilidad(MATEO, "Java").valorOLanzar();
        app.perfiles().agregarHabilidad(MATEO, "Arquitectura").valorOLanzar();
        app.perfiles().agregarInteres(MATEO, "Entrevistas").valorOLanzar();
        app.wallet().acreditarDemo(MATEO, 2480).valorOLanzar();

        app.perfiles().crearPerfil(new CrearPerfilComando(SOFIA, "Sofia Mendez", "PROFESIONAL",
                "Tech recruiter y mentora. Preparo entrevistas tecnicas hace 8 anios.",
                "America/Argentina/Buenos_Aires", "Tech Recruiter & Mentora", "EMAIL")).valorOLanzar();
        app.perfiles().agregarHabilidad(SOFIA, "Entrevistas").valorOLanzar();
        app.wallet().acreditarDemo(SOFIA, 500).valorOLanzar();

        app.perfiles().crearPerfil(new CrearPerfilComando(TECHCORP, "TechCorp", "ORGANIZACION",
                "Empresa de software que publica desafios para encontrar talento.", "America/Argentina/Buenos_Aires",
                "Organizacion", "PUSH")).valorOLanzar();

        LocalDateTime base = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS).withHour(18);
        publicar(app, SOFIA, new CrearPublicacionComando("Preparacion para entrevistas tecnicas",
                "Simulacro completo, feedback y plan personalizado para tu proxima entrevista.", "Tecnologia",
                "REMOTA", 60, 180, List.of("Entrevistas", "Java"),
                List.of(franja(base, 60, 3), franja(base.plusDays(1), 60, 3)), "OFERTA"));
        publicar(app, SOFIA, new CrearPublicacionComando("Revision de portfolio",
                "Revisamos tu portfolio y tu CV para que destaquen.", "Diseno", "REMOTA", 45, 120,
                List.of("Portfolio"), List.of(franja(base.plusDays(2), 45, 2)), "OFERTA"));
        publicar(app, MATEO, new CrearPublicacionComando("Asesoria de arquitectura de software",
                "Revisamos juntos la arquitectura de tu proyecto: capas, patrones y persistencia.", "Tecnologia",
                "HIBRIDA", 90, 250, List.of("Arquitectura", "Java"),
                List.of(franja(base.plusDays(4), 90, 2)), "OFERTA"));
        publicar(app, TECHCORP, new CrearPublicacionComando("Desafio: prototipo de app de turnos",
                "Construi un prototipo funcional de una app de turnos. El mejor entra a nuestro programa.",
                "Desarrollo", "REMOTA", 240, 400, List.of("Java", "Prototipado"),
                List.of(franja(base.plusDays(7), 240, 5)), "DESAFIO"));
    }

    private static CrearPublicacionComando.Franja franja(LocalDateTime inicio, int minutos, int cupo) {
        return new CrearPublicacionComando.Franja(inicio, inicio.plusMinutes(minutos), cupo);
    }

    private static void publicar(AppFactory.Aplicacion app, String autor, CrearPublicacionComando comando) {
        String id = app.publicaciones().crearBorrador(autor, comando).valorOLanzar().id();
        app.publicaciones().cambiarEstado(autor, id, "PUBLICADA", null).valorOLanzar();
    }
}
