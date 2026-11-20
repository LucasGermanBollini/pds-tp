package com.uade.skillswap.presentation;

import com.uade.skillswap.application.port.CanalNotificacionFactory;
import com.uade.skillswap.application.port.CanalNotificacionPort;
import com.uade.skillswap.domain.model.TipoCanalNotificacion;

import java.util.ArrayList;
import java.util.List;

/** Doble de prueba de la Simple Factory: registra por que canal salio cada mensaje (puerto simulado, seccion 12). */
public class CanalesEspia implements CanalNotificacionFactory {

    public record Envio(TipoCanalNotificacion canal, String destinatario, String mensaje) {
    }

    public final List<Envio> envios = new ArrayList<>();

    @Override
    public CanalNotificacionPort crear(TipoCanalNotificacion tipo) {
        return (destinatario, mensaje) -> envios.add(new Envio(tipo, destinatario, mensaje));
    }
}
