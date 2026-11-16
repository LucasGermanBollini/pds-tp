package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.NotificadorObserver;
import com.uade.skillswap.domain.model.observer.RegistradorHistorialObserver;
import com.uade.skillswap.domain.repository.*;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Publicacion. Coordina el flujo (crear, publicar, pausar,
 * cerrar, cancelar, buscar y recomendar) sin conocer SQL ni UI (Clean
 * Architecture, Clase 8).
 */
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final NotificacionRepository notificacionRepository;

    public PublicacionService(PublicacionRepository publicacionRepository,
                               ContratacionRepository contratacionRepository,
                               HistorialEstadoRepository historialEstadoRepository,
                               NotificacionRepository notificacionRepository) {
        this.publicacionRepository = publicacionRepository;
        this.contratacionRepository = contratacionRepository;
        this.historialEstadoRepository = historialEstadoRepository;
        this.notificacionRepository = notificacionRepository;
    }

    public Publicacion crearBorrador(Publicador autor, String titulo) {
        Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, titulo);
        wireObservadores(publicacion);
        return publicacionRepository.guardar(publicacion);
    }

    public Publicacion completarDatos(String publicacionId, String descripcion, ElementoCatalogo categoria,
                                       Modalidad modalidad, int duracionEstimadaMinutos, SkillCoins precioBase,
                                       List<Habilidad> etiquetas, List<FranjaDisponibilidad> franjas) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.completarDatos(descripcion, categoria, modalidad, duracionEstimadaMinutos, precioBase);
        etiquetas.forEach(publicacion::agregarEtiqueta);
        franjas.forEach(publicacion::agregarFranja);
        return publicacionRepository.guardar(publicacion);
    }

    public Publicacion publicar(String publicacionId, String actorUserId) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.publicar(actorUserId);
        return publicacionRepository.guardar(publicacion);
    }

    public Publicacion pausar(String publicacionId, String actorUserId) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.pausar(actorUserId);
        return publicacionRepository.guardar(publicacion);
    }

    public Publicacion cerrar(String publicacionId, String actorUserId) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.cerrar(actorUserId);
        return publicacionRepository.guardar(publicacion);
    }

    public Publicacion cancelar(String publicacionId, String actorUserId, String motivo) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.cancelar(actorUserId, motivo);
        return publicacionRepository.guardar(publicacion);
    }

    public List<Publicacion> buscarPublicadas() {
        return publicacionRepository.listarTodos().stream()
                .filter(p -> "PUBLICADA".equals(p.getEstadoNombre()))
                .toList();
    }

    /**
     * Seccion 15: cambiar la estrategia de recomendacion no debe modificar
     * este caso de uso (Simple Factory + Strategy resuelven el if/else).
     */
    public List<Publicacion> recomendar(Perfil perfil, String nombreEstrategia) {
        RecomendacionStrategy estrategia = RecomendacionStrategyFactory.crear(nombreEstrategia);
        MotorRecomendacion motor = new MotorRecomendacion(estrategia);
        return motor.recomendar(perfil, buscarPublicadas());
    }

    private Publicacion obtener(String publicacionId) {
        return publicacionRepository.buscarPorId(publicacionId)
                .orElseThrow(() -> new IllegalArgumentException("Publicacion inexistente: " + publicacionId));
    }

    private void wireObservadores(Publicacion publicacion) {
        publicacion.agregarObservador(new RegistradorHistorialObserver(historialEstadoRepository));
        publicacion.agregarObservador(new NotificadorObserver(publicacionRepository, contratacionRepository,
                notificacionRepository));
    }
}
