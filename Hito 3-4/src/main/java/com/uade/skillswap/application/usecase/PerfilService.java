package com.uade.skillswap.application.usecase;

import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.domain.model.AccesoDenegadoException;
import com.uade.skillswap.domain.model.DisponibilidadSemanal;
import com.uade.skillswap.domain.model.Habilidad;
import com.uade.skillswap.domain.model.Perfil;
import com.uade.skillswap.domain.model.Publicacion;
import com.uade.skillswap.domain.model.SkillCoins;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;
import com.uade.skillswap.domain.model.TipoPerfil;
import com.uade.skillswap.domain.model.WalletSkillCoins;
import com.uade.skillswap.domain.model.observer.ObservadorDeEstado;
import com.uade.skillswap.domain.repository.ElementoCatalogoRepository;
import com.uade.skillswap.domain.repository.PerfilRepository;
import com.uade.skillswap.domain.repository.PublicacionRepository;
import com.uade.skillswap.domain.repository.WalletSkillCoinsRepository;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de Perfil (seccion 4.1): registrar el perfil profesional a
 * partir del userId del servicio de identidad externo, administrar sus datos
 * (biografia, zona horaria, habilidades, intereses, disponibilidad, canal de
 * notificacion) y darlo de baja logica (seccion 5).
 *
 * <p>Solo el propio usuario modifica su perfil: el actor se compara con el
 * userId del perfil.
 */
public class PerfilService {

    /** Command object del alta (seccion 9). */
    public record DatosPerfil(String nombreVisible, TipoPerfil tipo, String biografia, String zonaHoraria,
                              String rol, TipoCanalNotificacion canalPreferido) {
    }

    private final PerfilRepository perfilRepository;
    private final WalletSkillCoinsRepository walletSkillCoinsRepository;
    private final ElementoCatalogoRepository elementoCatalogoRepository;
    private final PublicacionRepository publicacionRepository;
    private final IdentidadProviderPort identidadProviderPort;
    private final UnitOfWork unitOfWork;
    private final List<ObservadorDeEstado> observadoresPublicacion;

    public PerfilService(PerfilRepository perfilRepository,
                          WalletSkillCoinsRepository walletSkillCoinsRepository,
                          ElementoCatalogoRepository elementoCatalogoRepository,
                          PublicacionRepository publicacionRepository,
                          IdentidadProviderPort identidadProviderPort,
                          UnitOfWork unitOfWork,
                          List<ObservadorDeEstado> observadoresPublicacion) {
        this.perfilRepository = perfilRepository;
        this.walletSkillCoinsRepository = walletSkillCoinsRepository;
        this.elementoCatalogoRepository = elementoCatalogoRepository;
        this.publicacionRepository = publicacionRepository;
        this.identidadProviderPort = identidadProviderPort;
        this.unitOfWork = unitOfWork;
        this.observadoresPublicacion = observadoresPublicacion;
    }

    /**
     * Seccion 4.1: "Registrar un perfil profesional referenciando un userId
     * provisto por un servicio de identidad externo". El userId no lo
     * inventa SkillSwap: se valida contra el puerto de identidad (Adapter).
     * Junto con el perfil se abre su wallet de SkillCoins en cero.
     */
    public Perfil crearPerfil(String userId, DatosPerfil datos) {
        return unitOfWork.ejecutar(() -> {
            identidadProviderPort.validarUsuarioActivo(userId);
            if (perfilRepository.buscarPorId(userId).isPresent()) {
                throw new IllegalStateException("Ya existe un perfil para el usuario " + userId);
            }
            Perfil perfil = new Perfil(userId, datos.nombreVisible(), datos.tipo());
            perfil.actualizarDatos(datos.nombreVisible(), datos.biografia(), datos.zonaHoraria(), datos.rol());
            perfil.elegirCanalPreferido(datos.canalPreferido());
            Perfil guardado = perfilRepository.guardar(perfil);
            if (walletSkillCoinsRepository.buscarPorTitular(userId).isEmpty()) {
                walletSkillCoinsRepository.guardar(new WalletSkillCoins(UUID.randomUUID().toString(), userId, SkillCoins.CERO));
            }
            return guardado;
        });
    }

    public Perfil actualizarDatos(String actorUserId, String nombreVisible, String biografia, String zonaHoraria,
                                   String rol) {
        return modificar(actorUserId, perfil -> perfil.actualizarDatos(nombreVisible, biografia, zonaHoraria, rol));
    }

    public Perfil agregarHabilidad(String actorUserId, String nombreHabilidad) {
        return modificar(actorUserId, perfil -> perfil.agregarHabilidad(obtenerOCrearHabilidad(nombreHabilidad)));
    }

    public Perfil agregarInteres(String actorUserId, String nombreHabilidad) {
        return modificar(actorUserId, perfil -> perfil.agregarInteres(obtenerOCrearHabilidad(nombreHabilidad)));
    }

    public Perfil definirDisponibilidad(String actorUserId, List<DisponibilidadSemanal> disponibilidad) {
        return modificar(actorUserId, perfil -> perfil.definirDisponibilidad(disponibilidad));
    }

    public Perfil elegirCanalPreferido(String actorUserId, TipoCanalNotificacion canal) {
        return modificar(actorUserId, perfil -> perfil.elegirCanalPreferido(canal));
    }

    /**
     * Seccion 5: baja logica. El perfil y todo su historial quedan en la base;
     * sus publicaciones PUBLICADAS se pausan para que no reciban solicitudes
     * nuevas. Las contrataciones ya existentes no se tocan.
     */
    public Perfil darDeBaja(String actorUserId, String userId) {
        if (actorUserId == null || !actorUserId.equals(userId)) {
            throw new AccesoDenegadoException("Solo el propio usuario puede dar de baja su perfil");
        }
        return unitOfWork.ejecutar(() -> {
            for (Publicacion publicacion : publicacionRepository.buscarPorAutor(userId)) {
                if (publicacion.estaPublicada()) {
                    observadoresPublicacion.forEach(publicacion::agregarObservador);
                    publicacion.pausar(actorUserId);
                    publicacionRepository.guardar(publicacion);
                }
            }
            Perfil perfil = obtener(userId);
            perfil.darBajaLogica();
            return perfilRepository.guardar(perfil);
        });
    }

    public Perfil buscarPorId(String userId) {
        return obtener(userId);
    }

    private Perfil modificar(String actorUserId, java.util.function.Consumer<Perfil> cambio) {
        return unitOfWork.ejecutar(() -> {
            Perfil perfil = obtener(actorUserId);
            perfil.verificarActivo();
            cambio.accept(perfil);
            return perfilRepository.guardar(perfil);
        });
    }

    private Perfil obtener(String userId) {
        return perfilRepository.buscarPorId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil inexistente: " + userId));
    }

    private Habilidad obtenerOCrearHabilidad(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la habilidad es obligatorio");
        }
        return elementoCatalogoRepository.buscarHabilidadPorNombre(nombre.trim())
                .orElseGet(() -> (Habilidad) elementoCatalogoRepository.guardar(
                        new Habilidad(UUID.randomUUID().toString(), nombre.trim())));
    }
}
