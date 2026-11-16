package com.uade.skillswap.application.port;

/**
 * Adapter Pattern - puerto. Seccion 1: "perfil profesional referenciando un
 * userId provisto por un servicio de identidad externo"; seccion 9: "los
 * controllers... deben validar permisos a partir del userId recibido del
 * servicio de identidad mock". La autenticacion real esta fuera de alcance
 * (seccion 16): esto es la version simulada, no un login real.
 */
public interface IdentidadProviderPort {

    /** Lanza IllegalArgumentException si el userId no es un usuario activo conocido por la identidad externa. */
    void validarUsuarioActivo(String userId);
}
