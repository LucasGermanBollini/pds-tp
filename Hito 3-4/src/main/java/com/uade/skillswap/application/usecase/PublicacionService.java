package com.uade.skillswap.application.usecase;

import com.uade.skillswap.domain.model.*;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.repository.ElementoCatalogoRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Publicacion. Coordina el flujo (crear, publicar, pausar,
 * cerrar, cancelar, buscar y recomendar) sin conocer SQL ni UI (Clean
 * Architecture, Clase 8). Los observers (quien escucha los cambios de
 * estado) se inyectan ya armados desde afuera (AppFactory): construirlos
 * aca adentro obligaria a este caso de uso a conocer NotificadorObserver,
 * que vive en Infraestructura porque adapta SDKs externos de email/push —
 * Aplicacion no puede depender de Infraestructura (seccion 6).
 */
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ElementoCatalogoRepository elementoCatalogoRepository;
    private final List<ObservadorDeEstado> observadoresPublicacion;

    public PublicacionService(PublicacionRepository publicacionRepository,
                               ElementoCatalogoRepository elementoCatalogoRepository,
                               List<ObservadorDeEstado> observadoresPublicacion) {
        this.publicacionRepository = publicacionRepository;
        this.elementoCatalogoRepository = elementoCatalogoRepository;
        this.observadoresPublicacion = observadoresPublicacion;
    }

    public Publicacion crearBorrador(Publicador autor, String titulo) {
        Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, titulo);
        wireObservadores(publicacion);
        return publicacionRepository.guardar(publicacion);
    }

    /**
     * Variante "plana" para la UI (seccion 10: la interfaz no conoce el
     * Composite de categorias ni franjas). Hace find-or-create de la
     * categoria por nombre y crea una franja por defecto, porque
     * validarRequisitosDePublicacion() exige al menos una para publicar.
     */
    public Publicacion crearBorradorSimple(Publicador autor, String titulo, String descripcion,
                                            String categoriaNombre, Modalidad modalidad,
                                            int duracionEstimadaMinutos, SkillCoins precioBase) {
        CategoriaHabilidad categoria = obtenerOCrearCategoria(categoriaNombre);
        FranjaDisponibilidad franjaPorDefecto = new FranjaDisponibilidad(UUID.randomUUID().toString(),
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusMinutes(Math.max(30, duracionEstimadaMinutos)), 5);

        Publicacion publicacion = crearBorrador(autor, titulo);
        publicacion.completarDatos(descripcion, categoria, modalidad, duracionEstimadaMinutos, precioBase);
        publicacion.agregarFranja(franjaPorDefecto);
        return publicacionRepository.guardar(publicacion);
    }

    private CategoriaHabilidad obtenerOCrearCategoria(String nombre) {
        return elementoCatalogoRepository.buscarCategoriaPorNombre(nombre)
                .orElseGet(() -> (CategoriaHabilidad) elementoCatalogoRepository.guardar(
                        new CategoriaHabilidad(UUID.randomUUID().toString(), nombre)));
    }

    public List<String> listarCategorias() {
        return elementoCatalogoRepository.listarCategorias().stream().map(CategoriaHabilidad::obtenerNombre).toList();
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

    public Publicacion buscarPorId(String publicacionId) {
        return publicacionRepository.buscarPorId(publicacionId)
                .orElseThrow(() -> new IllegalArgumentException("Publicacion inexistente: " + publicacionId));
    }

    public List<Publicacion> listarPropias(String autorUserId) {
        return publicacionRepository.buscarPorAutor(autorUserId);
    }

    /** Seccion 4.3: buscar, filtrar, ordenar (la paginacion la aplica el Controller/UI). */
    public List<Publicacion> buscar(String texto, String categoriaNombre, Modalidad modalidad,
                                     int precioMaximo, String orden) {
        String textoNormalizado = texto == null ? "" : texto.toLowerCase().trim();
        List<Publicacion> resultado = buscarPublicadas().stream()
                .filter(p -> textoNormalizado.isBlank() || contieneTexto(p, textoNormalizado))
                .filter(p -> categoriaNombre == null || categoriaNombre.isBlank() || "Todas".equalsIgnoreCase(categoriaNombre)
                        || (p.getCategoria() != null && p.getCategoria().obtenerNombre().equalsIgnoreCase(categoriaNombre)))
                .filter(p -> modalidad == null || p.getModalidad() == modalidad)
                .filter(p -> precioMaximo <= 0 || p.getPrecioBase() == null || p.getPrecioBase().valor() <= precioMaximo)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        if ("Menor precio".equalsIgnoreCase(orden)) {
            resultado.sort(Comparator.comparingLong(p -> p.getPrecioBase() == null ? 0 : p.getPrecioBase().valor()));
        } else if ("Mejor reputación".equalsIgnoreCase(orden) || "Mejor reputacion".equalsIgnoreCase(orden)) {
            resultado.sort(Comparator.comparingDouble(this::reputacionDelAutor).reversed());
        }
        return resultado;
    }

    private boolean contieneTexto(Publicacion p, String texto) {
        String etiquetas = p.getEtiquetas().stream().map(Habilidad::obtenerNombre).reduce("", (a, b) -> a + " " + b);
        String haystack = (p.getTitulo() + " " + nvl(p.getDescripcion()) + " " + etiquetas).toLowerCase();
        return haystack.contains(texto);
    }

    private double reputacionDelAutor(Publicacion p) {
        return p.getAutor() instanceof Perfil perfil ? perfil.getReputacionPromedio() : 0;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
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

    /** Los observers son @Transient; una Publicacion recien cargada desde JPA vuelve sin ninguno adjunto. */
    private Publicacion obtener(String publicacionId) {
        Publicacion publicacion = publicacionRepository.buscarPorId(publicacionId)
                .orElseThrow(() -> new IllegalArgumentException("Publicacion inexistente: " + publicacionId));
        wireObservadores(publicacion);
        return publicacion;
    }

    private void wireObservadores(Publicacion publicacion) {
        observadoresPublicacion.forEach(publicacion::agregarObservador);
    }
}
