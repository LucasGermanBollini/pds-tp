package com.uade.skillswap.domain.model.state;

/** Terminal: retirada con motivo obligatorio (seccion 4.2). No sobrescribe nada de la base. */
public class EstadoPublicacionCancelada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "CANCELADA";
    }
}
