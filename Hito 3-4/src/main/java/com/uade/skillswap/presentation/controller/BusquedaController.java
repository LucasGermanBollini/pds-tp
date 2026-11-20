package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.DtoMapper;
import com.uade.skillswap.application.dto.PublicacionDTO;
import com.uade.skillswap.application.port.IdentidadProviderPort;
import com.uade.skillswap.application.usecase.PublicacionService;
import com.uade.skillswap.domain.model.Modalidad;
import com.uade.skillswap.domain.model.TipoPublicacion;
import com.uade.skillswap.domain.repository.CriteriosBusqueda;
import com.uade.skillswap.domain.repository.OrdenBusqueda;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.comando.BuscarPublicacionesComando;

import java.util.List;

/** Seccion 9: BusquedaController.buscar(filtros): "Buscar, filtrar, ordenar y paginar". */
public class BusquedaController extends ControllerBase {

    private final PublicacionService publicacionService;

    public BusquedaController(PublicacionService publicacionService, IdentidadProviderPort identidadProviderPort) {
        super(identidadProviderPort);
        this.publicacionService = publicacionService;
    }

    public Resultado<Pagina<PublicacionDTO>> buscar(BuscarPublicacionesComando f) {
        return consultar(() -> {
            CriteriosBusqueda criterios = new CriteriosBusqueda(f.texto(), f.categoria(),
                    Enums.valor(Modalidad.class, f.modalidad(), null), f.precioMinimo(), f.precioMaximo(),
                    f.habilidad(), f.reputacionMinima(), Enums.valor(TipoPublicacion.class, f.tipo(), null),
                    OrdenBusqueda.desdeTexto(f.orden()), f.pagina(), f.tamanio());
            return publicacionService.buscar(criterios).map(DtoMapper::publicacion);
        });
    }

    public Resultado<List<String>> categorias() {
        return consultar(publicacionService::listarCategorias);
    }
}
