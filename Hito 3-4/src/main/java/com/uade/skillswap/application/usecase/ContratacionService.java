package com.uade.skillswap.application.usecase;

import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.domain.model.Calificacion;
import com.uade.skillswap.domain.model.Contratacion;
import com.uade.skillswap.domain.model.Cotizable;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;
import com.uade.skillswap.domain.model.MovimientoCreditos;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.PoliticaCancelacionStrategy;
import com.uade.skillswap.domain.model.PrecioPublicacion;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.SkillCoins;
import com.uade.skillswap.domain.model.TipoMovimientoCreditos;
import com.uade.skillswap.domain.model.VerificadorDeAgenda;
import com.uade.skillswap.domain.model.WalletSkillCoins;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.repository.CalificacionRepository;
import com.uade.skillswap.domain.repository.ContratacionRepository;
import com.uade.skillswap.domain.repository.MovimientoCreditosRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;
import com.uade.skillswap.domain.repository.WalletSkillCoinsRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Contratacion: solicitar, aceptar, rechazar, pagar, iniciar,
 * completar, cancelar, reprogramar (de comun acuerdo) y calificar (secciones
 * 4.4, 4.5 y 5).
 *
 * <p>Cada caso de uso es un limite transaccional (seccion 8): corre dentro de
 * {@link UnitOfWork#ejecutar}, asi el cambio de estado, el cupo, los saldos y
 * los movimientos se guardan todos juntos o no se guarda nada.
 *
 * <p>QUIEN puede hacer cada cosa lo decide el dominio (Contratacion conoce a
 * cliente y proveedor); este servicio solo orquesta: busca, delega, guarda.
 */
public class ContratacionService {

    private final ContratacionRepository contratacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final WalletSkillCoinsRepository walletSkillCoinsRepository;
    private final MovimientoCreditosRepository movimientoCreditosRepository;
    private final CalificacionRepository calificacionRepository;
    private final PerfilRepository perfilRepository;
    private final PoliticaCancelacionStrategy politicaCancelacionPorDefecto;
    private final UnitOfWork unitOfWork;
    private final List<ObservadorDeEstado> observadoresContratacion;

    public ContratacionService(ContratacionRepository contratacionRepository,
                                PublicacionRepository publicacionRepository,
                                WalletSkillCoinsRepository walletSkillCoinsRepository,
                                MovimientoCreditosRepository movimientoCreditosRepository,
                                CalificacionRepository calificacionRepository,
                                PerfilRepository perfilRepository,
                                PoliticaCancelacionStrategy politicaCancelacionPorDefecto,
                                UnitOfWork unitOfWork,
                                List<ObservadorDeEstado> observadoresContratacion) {
        this.contratacionRepository = contratacionRepository;
        this.publicacionRepository = publicacionRepository;
        this.walletSkillCoinsRepository = walletSkillCoinsRepository;
        this.movimientoCreditosRepository = movimientoCreditosRepository;
        this.calificacionRepository = calificacionRepository;
        this.perfilRepository = perfilRepository;
        this.politicaCancelacionPorDefecto = politicaCancelacionPorDefecto;
        this.unitOfWork = unitOfWork;
        this.observadoresContratacion = observadoresContratacion;
    }

    // ---- solicitud ----

    /** Lo que usa el ContratacionController: todo por id, tal como llega desde la interfaz. */
    public Contratacion solicitar(String clienteUserId, String publicacionId, String franjaId,
                                   String mensaje, String objetivo) {
        return unitOfWork.ejecutar(() -> {
            Perfil cliente = perfilRepository.buscarPorId(clienteUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + clienteUserId));
            Publicacion publicacion = publicacionRepository.buscarPorId(publicacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Publicacion inexistente: " + publicacionId));
            FranjaDisponibilidad franja = franjaId == null
                    ? primeraFranjaConCupo(publicacion)
                    : buscarFranja(publicacion, franjaId);
            return solicitar(cliente, publicacion, franja, mensaje, objetivo);
        });
    }

    /** Seccion 8: "Definir limites transaccionales para... reserva" (solicitud + contador de la publicacion). */
    public Contratacion solicitar(Perfil cliente, Publicacion publicacion, FranjaDisponibilidad franjaElegida,
                                   String mensaje, String objetivo) {
        return unitOfWork.ejecutar(() -> {
            VerificadorDeAgenda.verificarQuePuedeReservar(cliente.obtenerId(), franjaElegida,
                    contratacionRepository.buscarPorParticipante(cliente.obtenerId()), null);
            Contratacion contratacion = new Contratacion(UUID.randomUUID().toString(), cliente, publicacion,
                    franjaElegida, mensaje, objetivo);
            wireObservadores(contratacion);
            publicacion.registrarSolicitud();
            publicacionRepository.guardar(publicacion);
            Contratacion guardada = contratacionRepository.guardar(contratacion);
            contratacion.notificarSolicitudRecibida();
            return guardada;
        });
    }

    // ---- respuesta del proveedor ----

    /** Seccion 8: "...aceptacion" (cambio de estado + bloqueo de cupo en la misma transaccion). */
    public Contratacion aceptar(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            contratacion.aceptar(actorUserId);
            return contratacionRepository.guardar(contratacion);
        });
    }

    public Contratacion rechazar(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            contratacion.rechazar(actorUserId);
            return contratacionRepository.guardar(contratacion);
        });
    }

    // ---- pago ----

    /** Paga el precio base de la publicacion (sin promociones). */
    public Contratacion pagar(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = contratacionRepository.buscarPorId(contratacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Contratacion inexistente: " + contratacionId));
            return pagar(contratacionId, actorUserId, new PrecioPublicacion(contratacion.getPublicacion().getPrecioBase()));
        });
    }

    /**
     * Seccion 5: "El debito y credito de SkillCoins se realiza en una unica
     * transaccion"; seccion 4.4: "No se permiten saldos negativos ni doble
     * cobro"; seccion 15: "Rechazar un pago con saldo insuficiente sin
     * modificar saldos ni estado".
     *
     * <p>Orden deliberado: (1) se calcula el precio con sus promociones
     * (Decorator); (2) se valida el saldo; (3) se pide al dominio la
     * transicion ACEPTADA -> CONFIRMADA, que falla si quien paga no es el
     * cliente o si ya estaba pagada; (4) recien ahi se mueve el dinero. Si
     * cualquier paso falla, no se toco ningun saldo, y ademas todo corre en
     * una sola transaccion (rollback).
     */
    public Contratacion pagar(String contratacionId, String actorUserId, Cotizable precioAplicado) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            WalletSkillCoins walletCliente = obtenerWallet(contratacion.getCliente().obtenerId());
            WalletSkillCoins walletProveedor = obtenerWallet(contratacion.getProveedorId());
            SkillCoins monto = precioAplicado.calcularPrecio();

            if (!walletCliente.tieneSaldoSuficiente(monto)) {
                throw new IllegalStateException("Saldo insuficiente de SkillCoins para pagar la contratacion");
            }
            contratacion.pagar(actorUserId);

            walletCliente.debitar(monto);
            walletProveedor.acreditar(monto);
            walletSkillCoinsRepository.guardar(walletCliente);
            walletSkillCoinsRepository.guardar(walletProveedor);
            registrarMovimiento(walletCliente, TipoMovimientoCreditos.DEBITO, monto,
                    "Pago de '" + contratacion.getPublicacion().getTitulo() + "'", contratacionId);
            registrarMovimiento(walletProveedor, TipoMovimientoCreditos.CREDITO, monto,
                    "Cobro de '" + contratacion.getPublicacion().getTitulo() + "'", contratacionId);

            contratacion.registrarMontoPagado(monto);
            return contratacionRepository.guardar(contratacion);
        });
    }

    // ---- sesion ----

    public Contratacion iniciar(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            contratacion.iniciar(actorUserId);
            return contratacionRepository.guardar(contratacion);
        });
    }

    public Contratacion completar(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            contratacion.completar(actorUserId);
            return contratacionRepository.guardar(contratacion);
        });
    }

    // ---- cancelacion ----

    public Contratacion cancelar(String contratacionId, String actorUserId, String motivo) {
        return cancelar(contratacionId, actorUserId, motivo, politicaCancelacionPorDefecto);
    }

    /**
     * Seccion 5: "La cancelacion devuelve 100 %, 50 % o 0 % segun la
     * anticipacion configurable" (Strategy: la politica se elige afuera).
     * Seccion 8: "...cancelacion" como limite transaccional.
     */
    public Contratacion cancelar(String contratacionId, String actorUserId, String motivo,
                                  PoliticaCancelacionStrategy politica) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            SkillCoins montoPagado = contratacion.getMontoPagado();
            contratacion.cancelar(actorUserId, motivo);

            if (montoPagado != null) {
                SkillCoins reintegro = politica.calcularReintegro(montoPagado, contratacion, LocalDateTime.now());
                if (reintegro.valor() > 0) {
                    WalletSkillCoins walletCliente = obtenerWallet(contratacion.getCliente().obtenerId());
                    WalletSkillCoins walletProveedor = obtenerWallet(contratacion.getProveedorId());
                    walletProveedor.debitar(reintegro);
                    walletCliente.acreditar(reintegro);
                    walletSkillCoinsRepository.guardar(walletCliente);
                    walletSkillCoinsRepository.guardar(walletProveedor);
                    String detalle = " por cancelacion de '" + contratacion.getPublicacion().getTitulo()
                            + "' (politica " + politica.getNombre() + ")";
                    registrarMovimiento(walletCliente, TipoMovimientoCreditos.CREDITO, reintegro, "Reintegro" + detalle,
                            contratacionId);
                    registrarMovimiento(walletProveedor, TipoMovimientoCreditos.DEBITO, reintegro, "Devolucion" + detalle,
                            contratacionId);
                }
            }
            return contratacionRepository.guardar(contratacion);
        });
    }

    // ---- reprogramacion consensuada (seccion 4.4) ----

    public Contratacion proponerReprogramacion(String contratacionId, String actorUserId, String nuevaFranjaId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            FranjaDisponibilidad nueva = buscarFranja(contratacion.getPublicacion(), nuevaFranjaId);
            contratacion.proponerReprogramacion(actorUserId, nueva);
            return contratacionRepository.guardar(contratacion);
        });
    }

    public Contratacion proponerReprogramacion(String contratacionId, String actorUserId, FranjaDisponibilidad nuevaFranja) {
        return proponerReprogramacion(contratacionId, actorUserId, nuevaFranja.getId());
    }

    /** La otra parte acepta: se mueve el cupo de una franja a la otra, en una sola transaccion. */
    public Contratacion aceptarReprogramacion(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            if (contratacion.getFranjaPropuesta() != null) {
                VerificadorDeAgenda.verificarQuePuedeReservar(contratacion.getCliente().obtenerId(),
                        contratacion.getFranjaPropuesta(),
                        contratacionRepository.buscarPorParticipante(contratacion.getCliente().obtenerId()),
                        contratacion.getId());
            }
            contratacion.aceptarReprogramacion(actorUserId);
            return contratacionRepository.guardar(contratacion);
        });
    }

    public Contratacion rechazarReprogramacion(String contratacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            contratacion.rechazarReprogramacion(actorUserId);
            return contratacionRepository.guardar(contratacion);
        });
    }

    // ---- calificacion ----

    /**
     * Secciones 4.4 y 5: solo participantes de una contratacion COMPLETADA,
     * una vez cada uno, nunca a si mismos. El receptor no se elige: es la
     * otra parte (lo decide Contratacion). La reputacion del receptor se
     * recalcula a partir de todas sus calificaciones (seccion 4.1).
     */
    public Calificacion calificar(String contratacionId, String autorUserId, int puntaje, String comentario) {
        return unitOfWork.ejecutar(() -> {
            Contratacion contratacion = obtener(contratacionId);
            Calificacion calificacion = contratacion.calificar(UUID.randomUUID().toString(), autorUserId, puntaje,
                    comentario);
            boolean yaCalifico = calificacionRepository.buscarPorContratacion(contratacionId).stream()
                    .anyMatch(c -> c.getAutorUserId().equals(autorUserId));
            if (yaCalifico) {
                throw new IllegalStateException("Ya calificaste esta contratacion: se califica una sola vez");
            }
            calificacionRepository.guardar(calificacion);
            recalcularReputacion(calificacion.getReceptorUserId());
            return calificacion;
        });
    }

    /** Variante del Hito 2 que recibia el receptor: ahora se valida que sea la otra parte. */
    public Calificacion calificar(String contratacionId, String autorUserId, String receptorUserId,
                                   int puntaje, String comentario) {
        Contratacion contratacion = buscarPorId(contratacionId);
        if (contratacion.esParticipante(autorUserId) && !contratacion.contraparteDe(autorUserId).equals(receptorUserId)) {
            throw new IllegalArgumentException("Solo se puede calificar a la otra parte de la contratacion");
        }
        return calificar(contratacionId, autorUserId, puntaje, comentario);
    }

    public List<Calificacion> listarCalificacionesRecibidas(String userId) {
        return calificacionRepository.buscarPorReceptor(userId).stream()
                .sorted(Comparator.comparing(Calificacion::getFecha).reversed())
                .toList();
    }

    // ---- consultas ----

    public Contratacion buscarPorId(String contratacionId) {
        return contratacionRepository.buscarPorId(contratacionId)
                .orElseThrow(() -> new IllegalArgumentException("Contratacion inexistente: " + contratacionId));
    }

    /** Seccion 4.5: "consultar comprobante/historial" solo lo pueden hacer las partes. */
    public Contratacion verComoParticipante(String contratacionId, String userId) {
        Contratacion contratacion = buscarPorId(contratacionId);
        contratacion.verificarParticipante(userId);
        return contratacion;
    }

    public List<Contratacion> listarPorParticipante(String userId) {
        return contratacionRepository.buscarPorParticipante(userId).stream()
                .sorted(Comparator.comparing(Contratacion::getFechaCreacion).reversed())
                .toList();
    }

    // ---- helpers ----

    private void registrarMovimiento(WalletSkillCoins wallet, TipoMovimientoCreditos tipo, SkillCoins monto,
                                     String motivo, String contratacionId) {
        movimientoCreditosRepository.guardar(new MovimientoCreditos(UUID.randomUUID().toString(), wallet.getId(),
                tipo, monto, motivo, contratacionId, LocalDateTime.now(), wallet.getSaldo()));
    }

    private FranjaDisponibilidad buscarFranja(Publicacion publicacion, String franjaId) {
        return publicacion.getFranjas().stream()
                .filter(f -> f.getId().equals(franjaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La franja " + franjaId + " no pertenece a la publicacion"));
    }

    private FranjaDisponibilidad primeraFranjaConCupo(Publicacion publicacion) {
        return publicacion.getFranjas().stream()
                .filter(FranjaDisponibilidad::tieneCupo)
                .min(Comparator.comparing(FranjaDisponibilidad::getFechaHoraInicio))
                .orElseThrow(() -> new IllegalStateException("La publicacion no tiene franjas con cupo disponible"));
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

    /**
     * Los observers son @Transient (no se persisten: representan integraciones
     * de runtime, como en Pedido/PedidoObserver de la catedra), asi que cada
     * Contratacion recien cargada desde JPA vuelve sin ninguno adjunto y hay
     * que re-conectarlos aca, no solo al crearla en solicitar().
     */
    private Contratacion obtener(String contratacionId) {
        Contratacion contratacion = buscarPorId(contratacionId);
        wireObservadores(contratacion);
        return contratacion;
    }

    private void wireObservadores(Contratacion contratacion) {
        observadoresContratacion.forEach(contratacion::agregarObservador);
    }
}
