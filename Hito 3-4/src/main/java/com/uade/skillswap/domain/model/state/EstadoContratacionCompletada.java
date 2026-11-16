package com.uade.skillswap.domain.model.state;

import com.uade.skillswap.domain.model.Contratacion;

/** Terminal: habilita calificar y consultar comprobante (seccion 4.5), sin mas transiciones. */
public class EstadoContratacionCompletada extends EstadoContratacionBase {

    @Override
    public String getNombre() {
        return "COMPLETADA";
    }

    /** Habilitado: la regla "una vez por parte" la controla el caso de uso contra el repositorio. */
    @Override
    public void calificar(Contratacion ctx, String actorUserId) {
        // permitido
    }
}
