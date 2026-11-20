package com.uade.skillswap.domain.model.observer;

/** Observer Pattern - Subject. Lo implementan Publicacion y Contratacion. */
public interface SujetoObservable {

    void agregarObservador(ObservadorDeEstado observador);

    void quitarObservador(ObservadorDeEstado observador);

    void notificar(CambioEstadoEvento evento);
}
