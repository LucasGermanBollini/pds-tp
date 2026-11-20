package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.NotificadorObserver;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.domain.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Contratacion: solicitar, aceptar, rechazar, pagar, cancelar,
 * reprogramar, iniciar, completar y calificar (secciones 4.4, 4.5 y 5).
 */
public class ContratacionService {

    private final ContratacionRepository contratacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final WalletSkillCoinsRepository walletSkillCoinsRepository;
    private final MovimientoCreditosRepository movimientoCreditosRepository;
    private final CalificacionRepository calificacionRepository;
    private final PerfilRepository perfilRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final NotificacionRepository notificacionRepository;
    private final PoliticaCancelacionStrategy politicaCancelacionPorDefecto;

    public ContratacionService(ContratacionRepository contratacionRepository,
                                PublicacionRepository publicacionRepository,
                                WalletSkillCoinsRepository walletSkillCoinsRepository,
                                MovimientoCreditosRepository movimientoCreditosRepository,
                                CalificacionRepository calificacionRepository,
                                PerfilRepository perfilRepository,
                                HistorialEstadoRepository historialEstadoRepository,
                                NotificacionRepository notificacionRepository,
                                PoliticaCancelacionStrategy politicaCancelacionPorDefecto) {
        this.contratacionRepository = contratacionRepository;
        this.publicacionRepository = publicacionRepository;
        this.walletSkillCoinsRepository = walletSkillCoinsRepository;
        this.movimientoCreditosRepository = movimientoCreditosRepository;
        this.calificacionRepository = calificacionRepository;
        this.perfilRepository = perfilRepository;
        this.historialEstadoRepository = historialEstadoRepository;
        this.notificacionRepository = notificacionRepository;
        this.politicaCancelacionPorDefecto = politicaCancelacionPorDefecto;
    }

    public Contratacion solicitar(Perfil cliente, Publicacion publicacion, FranjaDisponibilidad franjaElegida,
                                   String mensaje, String objetivo) {
        Contratacion contratacion = new Contratacion(UUID.randomUUID().toString(), cliente, publicacion,
                franjaElegida, mensaje, objetivo);
        wireObservadores(contratacion);
        publicacion.registrarSolicitud();
        publicacionRepository.guardar(publicacion);
        return contratacionRepository.guardar(contratacion);
    }

    public Contratacion aceptar(String contratacionId, String actorUserId) {
        Contratacion contratacion = obtener(contratacionId);
        contratacion.aceptar(actorUserId);
        return contratacionRepository.guardar(contratacion);
    }

    public Contratacion rechazar(String contratacionId, String actorUserId) {
        Contratacion contratacion = obtener(contratacionId);
        contratacion.rechazar(actorUserId);
        return contratacionRepository.guardar(contratacion);
    }

    /**
     * Seccion 5: "El debito y credito de SkillCoins se realiza en una unica
     * transaccion" y seccion 15: "Rechazar un pago con saldo insuficiente sin
     * modificar saldos ni estado". Por eso se valida ANTES de mutar nada.
     */
    public Contratacion pagar(String contratacionId, String actorUserId, Cotizable precioAplicado) {
        Contratacion contratacion = obtener(contratacionId);
        WalletSkillCoins walletCliente = obtenerWallet(contratacion.getCliente().obtenerId());
        WalletSkillCoins walletProveedor = obtenerWallet(contratacion.getPublicacion().getAutor().obtenerId());
        SkillCoins monto = precioAplicado.calcularPrecio();

        if (!walletCliente.tieneSaldoSuficiente(monto)) {
            throw new IllegalStateException("Saldo insuficiente de SkillCoins para pagar la contratacion");
        }

        walletCliente.debitar(monto);
        walletProveedor.acreditar(monto);
        walletSkillCoinsRepository.guardar(walletCliente);
        walletSkillCoinsRepository.guardar(walletProveedor);
        movimientoCreditosRepository.guardar(new MovimientoCreditos(UUID.randomUUID().toString(), walletCliente.getId(),
                TipoMovimientoCreditos.DEBITO, monto, "Pago de contratacion " + contratacionId, contratacionId, LocalDateTime.now()));
        movimientoCreditosRepository.guardar(new MovimientoCreditos(UUID.randomUUID().toString(), walletProveedor.getId(),
                TipoMovimientoCreditos.CREDITO, monto, "Cobro de contratacion " + contratacionId, contratacionId, LocalDateTime.now()));

        contratacion.registrarMontoPagado(monto);
        contratacion.pagar(actorUserId);
        return contratacionRepository.guardar(contratacion);
    }

    public Contratacion cancelar(String contratacionId, String actorUserId, String motivo) {
        return cancelar(contratacionId, actorUserId, motivo, politicaCancelacionPorDefecto);
    }

    /** Seccion 15: "Cancelar y calcular el reintegro con dos politicas diferentes". */
    public Contratacion cancelar(String contratacionId, String actorUserId, String motivo,
                                  PoliticaCancelacionStrategy politica) {
        Contratacion contratacion = obtener(contratacionId);
        SkillCoins montoPagado = contratacion.getMontoPagado();

        if (montoPagado != null) {
            SkillCoins reintegro = politica.calcularReintegro(montoPagado, contratacion, LocalDateTime.now());
            WalletSkillCoins walletCliente = obtenerWallet(contratacion.getCliente().obtenerId());
            WalletSkillCoins walletProveedor = obtenerWallet(contratacion.getPublicacion().getAutor().obtenerId());
            walletProveedor.debitar(reintegro);
            walletCliente.acreditar(reintegro);
            walletSkillCoinsRepository.guardar(walletCliente);
            walletSkillCoinsRepository.guardar(walletProveedor);
            movimientoCreditosRepository.guardar(new MovimientoCreditos(UUID.randomUUID().toString(), walletCliente.getId(),
                    TipoMovimientoCreditos.CREDITO, reintegro, "Reintegro por cancelacion " + contratacionId, contratacionId, LocalDateTime.now()));
        }

        contratacion.cancelar(actorUserId, motivo);
        return contratacionRepository.guardar(contratacion);
    }

    public Contratacion proponerReprogramacion(String contratacionId, String actorUserId, FranjaDisponibilidad nuevaFranja) {
        Contratacion contratacion = obtener(contratacionId);
        contratacion.proponerReprogramacion(actorUserId, nuevaFranja);
        return contratacionRepository.guardar(contratacion);
    }

    public Contratacion iniciar(String contratacionId, String actorUserId) {
        Contratacion contratacion = obtener(contratacionId);
        contratacion.iniciar(actorUserId);
        return contratacionRepository.guardar(contratacion);
    }

    public Contratacion completar(String contratacionId, String actorUserId) {
        Contratacion contratacion = obtener(contratacionId);
        contratacion.completar(actorUserId);
        return contratacionRepository.guardar(contratacion);
    }

    /** Seccion 4.4 y 5: solo participantes de una completada, una vez, nunca a si mismo. */
    public Calificacion calificar(String contratacionId, String autorUserId, String receptorUserId,
                                   int puntaje, String comentario) {
        Contratacion contratacion = obtener(contratacionId);
        if (!"COMPLETADA".equals(contratacion.getEstadoNombre())) {
            throw new IllegalStateException("Solo se puede calificar una contratacion COMPLETADA");
        }
        boolean esParticipante = contratacion.getCliente().obtenerId().equals(autorUserId)
                || contratacion.getPublicacion().getAutor().obtenerId().equals(autorUserId);
        if (!esParticipante) {
            throw new IllegalArgumentException("Solo los participantes de la contratacion pueden calificarla");
        }
        boolean yaCalifico = calificacionRepository.buscarPorContratacion(contratacionId).stream()
                .anyMatch(c -> c.getAutorUserId().equals(autorUserId));
        if (yaCalifico) {
            throw new IllegalStateException("Ya existe una calificacion de este autor para esta contratacion");
        }

        Calificacion calificacion = new Calificacion(UUID.randomUUID().toString(), contratacionId, autorUserId,
                receptorUserId, puntaje, comentario, LocalDateTime.now());
        calificacionRepository.guardar(calificacion);
        recalcularReputacion(receptorUserId);
        return calificacion;
    }

    private void recalcularReputacion(String receptorUserId) {
        perfilRepository.buscarPorId(receptorUserId).ifPresent(perfil -> {
            List<Calificacion> recibidas = calificacionRepository.buscarPorReceptor(receptorUserId);
            perfil.recalcularReputacion(recibidas);
            perfilRepository.guardar(perfil);
        });
    }

    private WalletSkillCoins obtenerWallet(String titularUserId) {
        return walletSkillCoinsRepository.buscarPorTitular(titularUserId)
                .orElseThrow(() -> new IllegalStateException("El usuario " + titularUserId + " no tiene wallet de SkillCoins"));
    }

    private Contratacion obtener(String contratacionId) {
        return contratacionRepository.buscarPorId(contratacionId)
                .orElseThrow(() -> new IllegalArgumentException("Contratacion inexistente: " + contratacionId));
    }

    private void wireObservadores(Contratacion contratacion) {
        contratacion.agregarObservador(new RegistradorHistorialObserver(historialEstadoRepository));
        contratacion.agregarObservador(new NotificadorObserver(publicacionRepository, contratacionRepository,
                notificacionRepository));
    }
}
