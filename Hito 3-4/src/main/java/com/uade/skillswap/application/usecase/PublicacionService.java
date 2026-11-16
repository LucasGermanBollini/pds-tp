package com.uade.skillswap.application.usecase;

import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.domain.model.CategoriaHabilidad;
import com.uade.skillswap.domain.model.ElementoCatalogo;
import com.uade.skillswap.domain.model.FranjaDisponibilidad;
import com.uade.skillswap.domain.model.Habilidad;
import com.uade.skillswap.domain.model.Modalidad;
import com.uade.skillswap.domain.model.MotorRecomendacion;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.Publicador;
import com.uade.skillswap.domain.model.RecomendacionStrategy;
import com.uade.skillswap.domain.model.RecomendacionStrategyFactory;
import com.uade.skillswap.domain.model.SkillCoins;
import com.uade.skillswap.domain.model.TipoPublicacion;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.repository.CriteriosBusqueda;
import com.uade.skillswap.domain.repository.ElementoCatalogoRepository;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.domain.repository.PerfilRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Publicacion (secciones 4.2 y 4.3): crear el borrador,
 * completarlo, publicarlo/pausarlo/cerrarlo/cancelarlo, buscar y recomendar.
 * Coordina el flujo sin conocer SQL ni UI (Clean Architecture, Clase 8); las
 * reglas (estados validos, permisos del autor, requisitos para publicar)
 * viven en Publicacion y sus estados.
 *
 * <p>Los observers (quien escucha los cambios de estado) se inyectan ya
 * armados desde afuera (AppFactory): construirlos aca obligaria a este caso
 * de uso a conocer NotificadorObserver, que vive en Infraestructura
 * (seccion 6: Aplicacion no conoce SDKs externos).
 */
public class PublicacionService {

    /** Datos de una franja nueva, sin id: lo asigna el caso de uso. */
    public record NuevaFranja(LocalDateTime inicio, LocalDateTime fin, int cupo) {
    }

    /** Command object (seccion 9: evita listas extensas de parametros). */
    public record DatosPublicacion(String titulo, String descripcion, String categoria, Modalidad modalidad,
                                   int duracionMinutos, long precio, List<String> etiquetas,
                                   List<NuevaFranja> franjas, TipoPublicacion tipo) {
    }

    private final PublicacionRepository publicacionRepository;
    private final ElementoCatalogoRepository elementoCatalogoRepository;
    private final PerfilRepository perfilRepository;
    private final UnitOfWork unitOfWork;
    private final List<ObservadorDeEstado> observadoresPublicacion;

    public PublicacionService(PublicacionRepository publicacionRepository,
                               ElementoCatalogoRepository elementoCatalogoRepository,
                               PerfilRepository perfilRepository,
                               UnitOfWork unitOfWork,
                               List<ObservadorDeEstado> observadoresPublicacion) {
        this.publicacionRepository = publicacionRepository;
        this.elementoCatalogoRepository = elementoCatalogoRepository;
        this.perfilRepository = perfilRepository;
        this.unitOfWork = unitOfWork;
        this.observadoresPublicacion = observadoresPublicacion;
    }

    /** Constructor de los Hitos 2/3 (tests de dominio): sin perfiles ni transaccion. */
    public PublicacionService(PublicacionRepository publicacionRepository,
                               ElementoCatalogoRepository elementoCatalogoRepository,
                               List<ObservadorDeEstado> observadoresPublicacion) {
        this(publicacionRepository, elementoCatalogoRepository, null, UnitOfWork.DIRECTO, observadoresPublicacion);
    }

    // ---- alta y edicion ----

    public Publicacion crearBorrador(Publicador autor, String titulo) {
        return crearBorrador(autor, titulo, TipoPublicacion.OFERTA);
    }

    public Publicacion crearBorrador(Publicador autor, String titulo, TipoPublicacion tipo) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo es obligatorio");
        }
        Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, titulo, tipo);
        wireObservadores(publicacion);
        return publicacionRepository.guardar(publicacion);
    }

    /**
     * Alta completa en un solo paso (lo que usa el PublicacionController):
     * crea el BORRADOR con todos sus datos. La categoria y las etiquetas
     * llegan por nombre y se buscan en el catalogo (Composite) o se crean.
     */
    public Publicacion crearBorrador(String autorUserId, DatosPublicacion datos) {
        return unitOfWork.ejecutar(() -> {
            if (datos.titulo() == null || datos.titulo().isBlank()) {
                throw new IllegalArgumentException("El titulo es obligatorio");
            }
            Perfil autor = obtenerPerfil(autorUserId);
            Publicacion publicacion = new Publicacion(UUID.randomUUID().toString(), autor, datos.titulo(), datos.tipo());
            wireObservadores(publicacion);
            aplicarDatos(publicacion, datos);
            return publicacionRepository.guardar(publicacion);
        });
    }

    private void aplicarDatos(Publicacion publicacion, DatosPublicacion datos) {
        if (datos.precio() < 0) {
            throw new IllegalArgumentException("El precio base no puede ser negativo");
        }
        ElementoCatalogo categoria = datos.categoria() == null || datos.categoria().isBlank()
                ? null : obtenerOCrearCategoria(datos.categoria());
        SkillCoins precio = SkillCoins.de(datos.precio());
        publicacion.completarDatos(datos.descripcion(), categoria, datos.modalidad(), datos.duracionMinutos(), precio);
        if (datos.etiquetas() != null) {
            datos.etiquetas().stream().filter(n -> n != null && !n.isBlank())
                    .map(this::obtenerOCrearHabilidad)
                    .forEach(publicacion::agregarEtiqueta);
        }
        if (datos.franjas() != null) {
            datos.franjas().forEach(f -> publicacion.agregarFranja(
                    new FranjaDisponibilidad(UUID.randomUUID().toString(), f.inicio(), f.fin(), f.cupo())));
        }
    }

    /**
     * Variante "plana" que usaba el Mock UI Kit en el Hito 4. Se mantiene por
     * compatibilidad: ahora la franja por defecto la decide quien llama.
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

    public Publicacion completarDatos(String publicacionId, String actorUserId, String descripcion,
                                       ElementoCatalogo categoria, Modalidad modalidad, int duracionEstimadaMinutos,
                                       SkillCoins precioBase, List<Habilidad> etiquetas,
                                       List<FranjaDisponibilidad> franjas) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.verificarAutor(actorUserId);
        publicacion.completarDatos(descripcion, categoria, modalidad, duracionEstimadaMinutos, precioBase);
        etiquetas.forEach(publicacion::agregarEtiqueta);
        franjas.forEach(publicacion::agregarFranja);
        return publicacionRepository.guardar(publicacion);
    }

    /** Agrega una franja de disponibilidad a una publicacion propia en BORRADOR o PAUSADA. */
    public Publicacion agregarFranja(String publicacionId, String actorUserId, NuevaFranja franja) {
        Publicacion publicacion = obtener(publicacionId);
        publicacion.verificarAutor(actorUserId);
        publicacion.agregarFranja(new FranjaDisponibilidad(UUID.randomUUID().toString(), franja.inicio(),
                franja.fin(), franja.cupo()));
        return publicacionRepository.guardar(publicacion);
    }

    private CategoriaHabilidad obtenerOCrearCategoria(String nombre) {
        return elementoCatalogoRepository.buscarCategoriaPorNombre(nombre)
                .orElseGet(() -> (CategoriaHabilidad) elementoCatalogoRepository.guardar(
                        new CategoriaHabilidad(UUID.randomUUID().toString(), nombre)));
    }

    Habilidad obtenerOCrearHabilidad(String nombre) {
        return elementoCatalogoRepository.buscarHabilidadPorNombre(nombre.trim())
                .orElseGet(() -> (Habilidad) elementoCatalogoRepository.guardar(
                        new Habilidad(UUID.randomUUID().toString(), nombre.trim())));
    }

    public List<String> listarCategorias() {
        return elementoCatalogoRepository.listarCategorias().stream().map(CategoriaHabilidad::obtenerNombre)
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    // ---- ciclo de vida (State) ----

    public Publicacion publicar(String publicacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Publicacion publicacion = obtener(publicacionId);
            publicacion.publicar(actorUserId);
            return publicacionRepository.guardar(publicacion);
        });
    }

    public Publicacion pausar(String publicacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Publicacion publicacion = obtener(publicacionId);
            publicacion.pausar(actorUserId);
            return publicacionRepository.guardar(publicacion);
        });
    }

    public Publicacion cerrar(String publicacionId, String actorUserId) {
        return unitOfWork.ejecutar(() -> {
            Publicacion publicacion = obtener(publicacionId);
            publicacion.cerrar(actorUserId);
            return publicacionRepository.guardar(publicacion);
        });
    }

    public Publicacion cancelar(String publicacionId, String actorUserId, String motivo) {
        return unitOfWork.ejecutar(() -> {
            Publicacion publicacion = obtener(publicacionId);
            publicacion.cancelar(actorUserId, motivo);
            return publicacionRepository.guardar(publicacion);
        });
    }

    // ---- consultas ----

    public List<Publicacion> buscarPublicadas() {
        return publicacionRepository.buscarPublicadas();
    }

    public Publicacion buscarPorId(String publicacionId) {
        return publicacionRepository.buscarPorId(publicacionId)
                .orElseThrow(() -> new IllegalArgumentException("Publicacion inexistente: " + publicacionId));
    }

    public List<Publicacion> listarPropias(String autorUserId) {
        return publicacionRepository.buscarPorAutor(autorUserId);
    }

    /** Seccion 4.3: buscar, filtrar, ordenar y paginar. El repositorio lo resuelve en la base. */
    public Pagina<Publicacion> buscar(CriteriosBusqueda criterios) {
        return publicacionRepository.buscar(criterios == null ? CriteriosBusqueda.todas() : criterios);
    }

    /**
     * Seccion 15: cambiar la estrategia de recomendacion no debe modificar
     * este caso de uso (Simple Factory + Strategy resuelven el if/else).
     * No se recomienda al usuario sus propias publicaciones.
     */
    public List<Publicacion> recomendar(Perfil perfil, String nombreEstrategia) {
        RecomendacionStrategy estrategia = RecomendacionStrategyFactory.crear(nombreEstrategia);
        MotorRecomendacion motor = new MotorRecomendacion(estrategia);
        List<Publicacion> candidatas = buscarPublicadas().stream()
                .filter(p -> !p.esAutor(perfil.obtenerId()))
                .toList();
        return motor.recomendar(perfil, candidatas);
    }

    public List<Publicacion> recomendar(String userId, String nombreEstrategia) {
        return recomendar(obtenerPerfil(userId), nombreEstrategia);
    }

    private Perfil obtenerPerfil(String userId) {
        if (perfilRepository == null) {
            throw new IllegalStateException("PublicacionService armado sin PerfilRepository");
        }
        return perfilRepository.buscarPorId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + userId));
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
