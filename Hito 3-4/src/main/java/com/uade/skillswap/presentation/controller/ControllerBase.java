package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.excepcion.ConflictoConcurrenteException;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.domain.model.AccesoDenegadoException;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comportamiento comun de los controllers de la seccion 9:
 * <ul>
 *   <li>{@link #consultar}: ejecuta una consulta y traduce errores.</li>
 *   <li>{@link #modificar}: ademas valida, antes de delegar, que el userId
 *       recibido sea un usuario activo para el servicio de identidad
 *       (seccion 5: "Los controllers que modifican informacion deben validar
 *       permisos a partir del userId recibido del servicio de identidad mock").
 *       Lo que cada usuario puede hacer lo decide despues el dominio.</li>
 * </ul>
 * La traduccion de excepciones a codigos es la misma para todos (manejo
 * uniforme de errores). Los controllers quedan delgados: reciben, delegan en
 * el caso de uso y convierten a DTO; sin SQL ni reglas de negocio.
 */
public abstract class ControllerBase {

    private static final Logger LOG = Logger.getLogger(ControllerBase.class.getName());

    private final IdentidadProviderPort identidadProviderPort;

    protected ControllerBase(IdentidadProviderPort identidadProviderPort) {
        this.identidadProviderPort = identidadProviderPort;
    }

    protected <T> Resultado<T> consultar(Supplier<T> consulta) {
        try {
            return Resultado.ok(consulta.get());
        } catch (RuntimeException ex) {
            return traducir(ex);
        }
    }

    protected <T> Resultado<T> modificar(String actorUserId, Supplier<T> accion) {
        try {
            identidadProviderPort.validarUsuarioActivo(actorUserId);
            return Resultado.ok(accion.get());
        } catch (RuntimeException ex) {
            return traducir(ex);
        }
    }

    protected Resultado<Void> modificarSinValor(String actorUserId, Runnable accion) {
        return modificar(actorUserId, () -> {
            accion.run();
            return null;
        });
    }

    static <T> Resultado<T> traducir(RuntimeException ex) {
        if (ex instanceof AccesoDenegadoException) {
            return Resultado.error("ACCESO_DENEGADO", ex.getMessage());
        }
        if (ex instanceof ConflictoConcurrenteException) {
            return Resultado.error("CONFLICTO_CONCURRENTE", ex.getMessage());
        }
        if (ex instanceof IllegalArgumentException) {
            String mensaje = ex.getMessage() == null ? "Datos invalidos" : ex.getMessage();
            String codigo = mensaje.contains("inexistente") ? "NO_ENCONTRADO" : "DATOS_INVALIDOS";
            return Resultado.error(codigo, mensaje);
        }
        if (ex instanceof IllegalStateException) {
            return Resultado.error("OPERACION_NO_PERMITIDA", ex.getMessage());
        }
        LOG.log(Level.SEVERE, "Error inesperado en un controller", ex);
        return Resultado.error("ERROR_INTERNO", "Ocurrio un error inesperado. Intentalo de nuevo.");
    }
}
