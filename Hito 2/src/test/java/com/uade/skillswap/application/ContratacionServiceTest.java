package com.uade.skillswap.application;

import com.uade.skillswap.application.usecase.ContratacionService;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.infrastructure.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContratacionServiceTest {

    private PublicacionService publicacionService;
    private ContratacionService contratacionService;
    private InMemoryPerfilRepository perfilRepository;
    private InMemoryWalletSkillCoinsRepository walletRepository;
    private InMemoryPublicacionRepository publicacionRepository;

    @BeforeEach
    void setUp() {
        publicacionRepository = new InMemoryPublicacionRepository();
        InMemoryContratacionRepository contratacionRepository = new InMemoryContratacionRepository();
        InMemoryHistorialEstadoRepository historialRepository = new InMemoryHistorialEstadoRepository();
        InMemoryNotificacionRepository notificacionRepository = new InMemoryNotificacionRepository();
        perfilRepository = new InMemoryPerfilRepository();
        walletRepository = new InMemoryWalletSkillCoinsRepository();
        InMemoryMovimientoCreditosRepository movimientoRepository = new InMemoryMovimientoCreditosRepository();
        InMemoryCalificacionRepository calificacionRepository = new InMemoryCalificacionRepository();

        publicacionService = new PublicacionService(publicacionRepository, contratacionRepository,
                historialRepository, notificacionRepository);
        contratacionService = new ContratacionService(contratacionRepository, publicacionRepository, walletRepository,
                movimientoRepository, calificacionRepository, perfilRepository, historialRepository,
                notificacionRepository, new PoliticaCancelacionEstandar());
    }

    private Perfil crearPerfilConWallet(String id, String nombre, long saldoInicial) {
        Perfil perfil = new Perfil(id, nombre);
        perfilRepository.guardar(perfil);
        walletRepository.guardar(new WalletSkillCoins("wallet-" + id, id, SkillCoins.de(saldoInicial)));
        return perfil;
    }

    private Publicacion publicarOferta(Perfil autor, int cupo, long precioBase) {
        Publicacion publicacion = publicacionService.crearBorrador(autor, "Mentoria de entrevistas");
        FranjaDisponibilidad franja = new FranjaDisponibilidad("f-1", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1), cupo);
        publicacionService.completarDatos(publicacion.getId(), "desc", new CategoriaHabilidad("cat-1", "Carrera"),
                Modalidad.REMOTA, 60, SkillCoins.de(precioBase), List.of(), List.of(franja));
        return publicacionService.publicar(publicacion.getId(), autor.obtenerId());
    }

    @Test
    void flujoCompletoPublicarSolicitarAceptarPagarCompletarYCalificar() {
        Perfil proveedor = crearPerfilConWallet("prof-1", "Ada", 0);
        Perfil cliente = crearPerfilConWallet("cli-1", "Beto", 1000);
        Publicacion publicacion = publicarOferta(proveedor, 1, 300);
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);

        Contratacion contratacion = contratacionService.solicitar(cliente, publicacion, franja, "hola", "practicar");
        assertEquals("PENDIENTE", contratacion.getEstadoNombre());

        contratacionService.aceptar(contratacion.getId(), proveedor.obtenerId());
        assertEquals("ACEPTADA", contratacion.getEstadoNombre());

        contratacionService.pagar(contratacion.getId(), cliente.obtenerId(), new PrecioPublicacion(publicacion.getPrecioBase()));
        assertEquals("CONFIRMADA", contratacion.getEstadoNombre());
        assertEquals(SkillCoins.de(700), walletRepository.buscarPorTitular("cli-1").orElseThrow().getSaldo());
        assertEquals(SkillCoins.de(300), walletRepository.buscarPorTitular("prof-1").orElseThrow().getSaldo());

        contratacionService.iniciar(contratacion.getId(), proveedor.obtenerId());
        contratacionService.completar(contratacion.getId(), proveedor.obtenerId());
        assertEquals("COMPLETADA", contratacion.getEstadoNombre());

        contratacionService.calificar(contratacion.getId(), cliente.obtenerId(), proveedor.obtenerId(), 5, "Excelente");
        Perfil proveedorActualizado = perfilRepository.buscarPorId("prof-1").orElseThrow();
        assertEquals(5.0, proveedorActualizado.getReputacionPromedio());
        assertEquals(1, proveedorActualizado.getCantidadCalificaciones());

        assertThrows(IllegalStateException.class, () ->
                contratacionService.calificar(contratacion.getId(), cliente.obtenerId(), proveedor.obtenerId(), 4, "De nuevo"),
                "una persona no puede calificar dos veces la misma contratacion");
    }

    @Test
    void dosClientesNoPuedenConsumirElUltimoCupoAlAceptar() {
        Perfil proveedor = crearPerfilConWallet("prof-1", "Ada", 0);
        Perfil cliente1 = crearPerfilConWallet("cli-1", "Beto", 1000);
        Perfil cliente2 = crearPerfilConWallet("cli-2", "Clara", 1000);
        Publicacion publicacion = publicarOferta(proveedor, 1, 300);
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);

        Contratacion c1 = contratacionService.solicitar(cliente1, publicacion, franja, "hola", "practicar");
        Contratacion c2 = contratacionService.solicitar(cliente2, publicacion, franja, "hola", "practicar");

        contratacionService.aceptar(c1.getId(), proveedor.obtenerId());
        assertThrows(IllegalStateException.class, () -> contratacionService.aceptar(c2.getId(), proveedor.obtenerId()));
        assertEquals("PENDIENTE", c2.getEstadoNombre());
    }

    @Test
    void unPagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
        Perfil proveedor = crearPerfilConWallet("prof-1", "Ada", 0);
        Perfil cliente = crearPerfilConWallet("cli-1", "Beto", 50);
        Publicacion publicacion = publicarOferta(proveedor, 1, 300);
        FranjaDisponibilidad franja = publicacion.getFranjas().get(0);

        Contratacion contratacion = contratacionService.solicitar(cliente, publicacion, franja, "hola", "practicar");
        contratacionService.aceptar(contratacion.getId(), proveedor.obtenerId());

        assertThrows(IllegalStateException.class, () ->
                contratacionService.pagar(contratacion.getId(), cliente.obtenerId(), new PrecioPublicacion(publicacion.getPrecioBase())));

        assertEquals("ACEPTADA", contratacion.getEstadoNombre(), "el estado no debe cambiar si el pago falla");
        assertEquals(SkillCoins.de(50), walletRepository.buscarPorTitular("cli-1").orElseThrow().getSaldo());
        assertEquals(SkillCoins.de(0), walletRepository.buscarPorTitular("prof-1").orElseThrow().getSaldo());
    }

    @Test
    void cancelarConDosPoliticasDistintasDaReintegrosDistintos() {
        // Misma anticipacion (10hs) para las dos contrataciones: la Estandar exige
        // 24hs para devolver algo (da 0%), la Flexible devuelve 50% desde las 6hs.
        Perfil proveedor = crearPerfilConWallet("prof-1", "Ada", 0);
        Perfil cliente1 = crearPerfilConWallet("cli-1", "Beto", 1000);
        Perfil cliente2 = crearPerfilConWallet("cli-2", "Clara", 1000);
        Publicacion publicacion = publicacionService.crearBorrador(proveedor, "Mentoria urgente");
        FranjaDisponibilidad franja1 = new FranjaDisponibilidad("f-urgente-1", LocalDateTime.now().plusHours(10),
                LocalDateTime.now().plusHours(11), 2);
        FranjaDisponibilidad franja2 = new FranjaDisponibilidad("f-urgente-2", LocalDateTime.now().plusHours(10),
                LocalDateTime.now().plusHours(11), 2);
        publicacionService.completarDatos(publicacion.getId(), "desc", new CategoriaHabilidad("cat-1", "Carrera"),
                Modalidad.REMOTA, 60, SkillCoins.de(1000), List.of(), List.of(franja1, franja2));
        publicacion = publicacionService.publicar(publicacion.getId(), proveedor.obtenerId());

        Contratacion conEstandar = contratacionService.solicitar(cliente1, publicacion, franja1, "hola", "practicar");
        contratacionService.aceptar(conEstandar.getId(), proveedor.obtenerId());
        contratacionService.pagar(conEstandar.getId(), cliente1.obtenerId(), new PrecioPublicacion(publicacion.getPrecioBase()));
        contratacionService.cancelar(conEstandar.getId(), cliente1.obtenerId(), "no puedo asistir", new PoliticaCancelacionEstandar());

        Contratacion conFlexible = contratacionService.solicitar(cliente2, publicacion, franja2, "hola", "practicar");
        contratacionService.aceptar(conFlexible.getId(), proveedor.obtenerId());
        contratacionService.pagar(conFlexible.getId(), cliente2.obtenerId(), new PrecioPublicacion(publicacion.getPrecioBase()));
        contratacionService.cancelar(conFlexible.getId(), cliente2.obtenerId(), "no puedo asistir", new PoliticaCancelacionFlexible());

        assertEquals(SkillCoins.de(0), walletRepository.buscarPorTitular("cli-1").orElseThrow().getSaldo(),
                "con la politica Estandar y 10hs de anticipacion el reintegro es 0%");
        assertEquals(SkillCoins.de(500), walletRepository.buscarPorTitular("cli-2").orElseThrow().getSaldo(),
                "con la politica Flexible y 10hs de anticipacion el reintegro es 50%");
    }
}
