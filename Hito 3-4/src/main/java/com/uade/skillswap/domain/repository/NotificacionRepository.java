package com.uade.skillswap.domain.repository;

import com.uade.skillswap.domain.model.Notificacion;

import java.util.List;

public interface NotificacionRepository extends Repositorio<Notificacion, String> {

    List<Notificacion> buscarPorDestinatario(String destinatarioUserId);
}
