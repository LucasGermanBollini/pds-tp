package com.uade.skillswap.infrastructure.identidad;

import com.uade.skillswap.application.port.IdentidadProviderPort;

/** Adapter Pattern - adapta IdentidadExternaSdk (codigos de estado) al puerto que usan los Controllers. */
public class IdentidadServiceAdapter implements IdentidadProviderPort {

    private final IdentidadExternaSdk sdk;

    public IdentidadServiceAdapter(IdentidadExternaSdk sdk) {
        this.sdk = sdk;
    }

    @Override
    public void validarUsuarioActivo(String userId) {
        String estado = sdk.consultarEstado(userId);
        if (!IdentidadExternaSdk.ACTIVO.equals(estado)) {
            throw new IllegalArgumentException("El servicio de identidad no reconoce un usuario activo: " + userId);
        }
    }
}
