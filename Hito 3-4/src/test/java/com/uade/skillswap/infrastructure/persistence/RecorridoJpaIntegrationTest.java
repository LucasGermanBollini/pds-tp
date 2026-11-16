package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.application.excepcion.ConflictoConcurrenteException;
import com.uade.skillswap.config.Repositorios;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.infrastructure.persistence.jpa.JPAUtil;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaContext;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaUnitOfWork;
import com.uade.skillswap.presentation.Escenario;
import com.uade.skillswap.presentation.controller.Resultado;
import com.uade.skillswap.presentation.controller.comando.BuscarPublicacionesComando;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seccion 12: "Pruebas de integracion de controllers, casos de uso y
 * persistencia". La aplicacion completa (AppFactory) sobre JPA/Hibernate y
 * H2 en modo MySQL, con el esquema creado por las migraciones Flyway.
 */
class RecorridoJpaIntegrationTest extends JpaTestSupport {

    private Escenario escenario() {
        Escenario e = new Escenario(Repositorios.jpa(), new JpaUnitOfWork());
        e.perfil("ada", "Ada", "PROFESIONAL", "EMAIL", 0);
        e.perfil("beto", "Beto", "PROFESIONAL", "PUSH", 1000);
        return e;
    }

    @Test
    void recorridoDeLaDemoContraLaBase() {
        Escenario e = escenario();
        PublicacionDTO pub = e.publicar("ada", "Mentoria Java", "Carrera", "REMOTA", 300, List.of("Java"),
                List.of(e.franja(0, 1), e.franja(2, 1)));

        ContratacionDTO c = e.app.contrataciones().solicitar("beto", new SolicitarComando(pub.id(), null, "", ""))
                .valorOLanzar();
        e.app.contrataciones().aceptar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().proponerReprogramacion("ada", c.id(), pub.franjas().get(1).id()).valorOLanzar();
        e.app.contrataciones().aceptarReprogramacion("beto", c.id()).valorOLanzar();
        e.app.contrataciones().pagar("beto", c.id()).valorOLanzar();
        e.app.contrataciones().iniciar("ada", c.id()).valorOLanzar();
        e.app.contrataciones().completar("ada", c.id()).valorOLanzar();
        e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 5, "Excelente")).valorOLanzar();

        ContratacionDTO final_ = e.app.contrataciones().verComprobante("beto", c.id()).valorOLanzar();
        assertEquals("COMPLETADA", final_.estado());
        assertEquals(pub.franjas().get(1).id(), final_.franja().id(), "la reprogramacion quedo persistida");
        assertEquals(700L, e.app.wallet().consultarSaldo("beto").valorOLanzar());
        assertEquals(5.0, e.app.perfiles().verPerfil("ada").valorOLanzar().reputacion());
        assertEquals(2, e.app.wallet().listarMovimientos("beto", 0, 10).valorOLanzar().totalElementos(),
                "acreditacion inicial + pago");
        assertEquals("OPERACION_NO_PERMITIDA",
                e.app.calificaciones().calificar("beto", new CalificarComando(c.id(), 1, "")).codigoError());
    }

    @Test
    void laBusquedaFiltraOrdenaYPaginaEnLaBaseSinNMasUno() {
        Escenario e = escenario();
        for (int i = 1; i <= 6; i++) {
            e.publicar("ada", "Mentoria " + i, i % 2 == 0 ? "Carrera" : "Diseno", "REMOTA", i * 100L,
                    List.of("Java", "Tag" + i), List.of(e.franja(i, 2)));
        }

        Pagina<PublicacionDTO> carrera = e.app.busqueda().buscar(new BuscarPublicacionesComando(null, "carrera", null,
                null, null, "java", null, null, "MENOR_PRECIO", 0, 2)).valorOLanzar();
        assertEquals(3, carrera.totalElementos());
        assertEquals(List.of(200L, 400L), carrera.contenido().stream().map(PublicacionDTO::precio).toList());

        Pagina<PublicacionDTO> segunda = e.app.busqueda().buscar(new BuscarPublicacionesComando(null, "carrera", null,
                null, null, "java", null, null, "MENOR_PRECIO", 1, 2)).valorOLanzar();
        assertEquals(List.of(600L), segunda.contenido().stream().map(PublicacionDTO::precio).toList());

        assertEquals(List.of("Mentoria 3"), e.app.busqueda().buscar(new BuscarPublicacionesComando("tag3", null, null,
                        null, null, null, null, null, "RELEVANCIA", 0, 10)).valorOLanzar().contenido().stream()
                .map(PublicacionDTO::titulo).toList(), "el texto tambien busca en las etiquetas");
        assertEquals("Mentoria 6", e.app.busqueda().buscar(new BuscarPublicacionesComando(null, null, null, 250L, null,
                null, null, null, "MAS_RECIENTE", 0, 10)).valorOLanzar().contenido().get(0).titulo());

        // N+1: la cantidad de consultas no crece con la cantidad de resultados.
        Statistics estadisticas = estadisticas();
        estadisticas.clear();
        e.app.busqueda().buscar(new BuscarPublicacionesComando(null, null, null, null, null, null, null, null, null, 0, 6))
                .valorOLanzar();
        long conSeis = estadisticas.getPrepareStatementCount();
        estadisticas.clear();
        e.app.busqueda().buscar(new BuscarPublicacionesComando(null, null, null, null, null, null, null, null, null, 0, 2))
                .valorOLanzar();
        long conDos = estadisticas.getPrepareStatementCount();
        assertEquals(conDos, conSeis, "traer 6 publicaciones cuesta las mismas consultas que traer 2");
        // 1 ids paginados + 1 count + 1 carga con JOIN FETCH (autor, categoria, franjas) + 4 lotes de colecciones
        // (etiquetas de las publicaciones; habilidades, intereses y disponibilidad de los autores).
        assertTrue(conSeis <= 7, "fueron " + conSeis + " consultas");
    }

    @Test
    void dosTransaccionesSobreElUltimoCupoTerminanEnConflictoConcurrente() {
        Escenario e = escenario();
        PublicacionDTO pub = e.publicar("ada", "Mentoria", "Carrera", "REMOTA", 100, List.of(), List.of(e.franja(0, 1)));
        String franjaId = pub.franjas().get(0).id();

        assertThrows(ConflictoConcurrenteException.class, () -> new JpaUnitOfWork().ejecutar(() -> {
            FranjaDisponibilidad miVista = JpaContext.actual().find(FranjaDisponibilidad.class, franjaId);
            assertTrue(miVista.reservarCupo());
            // mientras tanto, otra transaccion se queda con el ultimo cupo y confirma primero
            EntityManager otra = JPAUtil.getEntityManager();
            try {
                otra.getTransaction().begin();
                otra.find(FranjaDisponibilidad.class, franjaId).reservarCupo();
                otra.getTransaction().commit();
            } finally {
                otra.close();
            }
            return null;
        }));
        Resultado<PublicacionDTO> detalle = e.app.publicaciones().verDetalle(pub.id());
        assertEquals(0, detalle.valorOLanzar().franjas().get(0).cupoDisponible(), "solo se consumio un cupo");
    }

    private Statistics estadisticas() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        } finally {
            em.close();
        }
    }
}
