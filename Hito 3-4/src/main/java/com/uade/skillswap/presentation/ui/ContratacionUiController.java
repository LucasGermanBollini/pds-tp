package com.uade.skillswap.presentation.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models;
import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.domain.repository.Pagina;
import com.uade.skillswap.presentation.controller.ContratacionController;
import com.uade.skillswap.presentation.controller.comando.SolicitarComando;

import java.util.List;

/**
 * Adapter (contrato del Mock UI Kit -> ContratacionController). El kit tiene
 * un unico boton "Avanzar estado": aca se traduce a la accion que
 * corresponde segun el estado actual. Si la accion le toca a la otra parte
 * (por ejemplo, el cliente intenta aceptar), el dominio la rechaza con
 * ACCESO_DENEGADO y la interfaz lo muestra.
 */
public class ContratacionUiController implements UiControllers.ContratacionController {

    private final ContratacionController contratacionController;

    public ContratacionUiController(ContratacionController contratacionController) {
        this.contratacionController = contratacionController;
    }

    @Override
    public List<Models.Contratacion> listarContrataciones(String userId) {
        return contratacionController.listar(userId, 0, Pagina.TAMANIO_MAXIMO).valorOLanzar().contenido().stream()
                .map(c -> UiMappers.aContratacionUi(c, userId)).toList();
    }

    /** El kit no pide franja ni mensaje: se toma la proxima franja con cupo. */
    @Override
    public Models.Contratacion solicitar(String userId, String publicacionId, String objetivo) {
        ContratacionDTO c = contratacionController.solicitar(userId,
                new SolicitarComando(publicacionId, null, "Solicitud enviada desde SkillSwap", objetivo)).valorOLanzar();
        return UiMappers.aContratacionUi(c, userId);
    }

    @Override
    public Models.Contratacion avanzar(String userId, String contratacionId) {
        ContratacionDTO actual = contratacionController.verComprobante(userId, contratacionId).valorOLanzar();
        ContratacionDTO resultado = switch (actual.estado()) {
            case "PENDIENTE" -> contratacionController.aceptar(userId, contratacionId).valorOLanzar();
            case "ACEPTADA" -> contratacionController.pagar(userId, contratacionId).valorOLanzar();
            case "CONFIRMADA" -> contratacionController.iniciar(userId, contratacionId).valorOLanzar();
            case "EN_CURSO" -> contratacionController.completar(userId, contratacionId).valorOLanzar();
            default -> actual; // COMPLETADA/RECHAZADA/CANCELADA son terminales: no hay "siguiente".
        };
        return UiMappers.aContratacionUi(resultado, userId);
    }

    /** Si el proveedor "cancela" una solicitud PENDIENTE, en el dominio eso es rechazarla. */
    @Override
    public Models.Contratacion cancelar(String userId, String contratacionId) {
        ContratacionDTO actual = contratacionController.verComprobante(userId, contratacionId).valorOLanzar();
        ContratacionDTO resultado = "PENDIENTE".equals(actual.estado()) && actual.proveedorId().equals(userId)
                ? contratacionController.rechazar(userId, contratacionId).valorOLanzar()
                : contratacionController.cancelar(userId, contratacionId, "Cancelada desde la interfaz").valorOLanzar();
        return UiMappers.aContratacionUi(resultado, userId);
    }
}
