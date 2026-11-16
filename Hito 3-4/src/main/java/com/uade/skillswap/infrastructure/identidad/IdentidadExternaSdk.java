package com.uade.skillswap.infrastructure.identidad;

/**
 * Simula el servicio de identidad externo (seccion 1 y 10: "un servicio de
 * identidad externo", "login simulado"). Su contrato devuelve un codigo de
 * estado, no un boolean, como haria un proveedor real (ej. un IdP SSO).
 * No implementa autenticacion real (fuera de alcance, seccion 16): solo
 * valida que el userId tenga forma de identidad emitida por "el proveedor".
 */
public class IdentidadExternaSdk {

    public static final String ACTIVO = "ACTIVO";
    public static final String INEXISTENTE = "INEXISTENTE";

    public String consultarEstado(String userId) {
        if (userId == null || userId.isBlank()) {
            return INEXISTENTE;
        }
        return ACTIVO;
    }
}
