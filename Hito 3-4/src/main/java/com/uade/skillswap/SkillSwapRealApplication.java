package com.uade.skillswap;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.ui.SkillSwapView;
import ar.edu.uade.skillswap.ui.Theme;
import com.uade.skillswap.application.dto.ContratacionDTO;
import com.uade.skillswap.application.dto.FranjaDTO;
import com.uade.skillswap.application.port.UnitOfWork;
import com.uade.skillswap.config.AppFactory;
import com.uade.skillswap.config.BaseDeDatos;
import com.uade.skillswap.config.DatosDemo;
import com.uade.skillswap.config.Repositorios;
import com.uade.skillswap.domain.model.PoliticaCancelacionEstandar;
import com.uade.skillswap.infrastructure.persistence.jpa.JpaUnitOfWork;
import com.uade.skillswap.presentation.controller.Resultado;
import com.uade.skillswap.presentation.controller.comando.CalificarComando;
import com.uade.skillswap.presentation.ui.ErroresEnDialogo;

import javax.swing.*;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * El SkillSwapView del Mock UI Kit (no se rediseñan pantallas, seccion 10)
 * conectado a nuestros controllers reales, con:
 * <ul>
 *   <li>login simulado: se elige el usuario activo (seccion 10: "Un login
 *       simulado entregara el userId activo a cada operacion"), y se puede
 *       cambiar desde el menu para jugar los dos roles (cliente/proveedor);</li>
 *   <li>un menu "Acciones" para lo que el kit no trae pantalla: calificar
 *       (paso final de la demo de la seccion 13) y reprogramar;</li>
 *   <li>los errores de aplicacion se muestran en un dialogo.</li>
 * </ul>
 *
 * <p>Uso: sin argumentos usa MySQL (correr antes scripts/bootstrap-mysql.sql);
 * con --h2 usa H2 en memoria (mismas migraciones Flyway, no instala nada);
 * con --memoria usa los repositorios en memoria.
 */
public final class SkillSwapRealApplication {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final AppFactory.Aplicacion app;
    private final UiControllers ui;
    private final JFrame frame = new JFrame("SkillSwap");
    private String usuarioActivo = DatosDemo.MATEO;

    private SkillSwapRealApplication(AppFactory.Aplicacion app) {
        this.app = app;
        this.ui = ErroresEnDialogo.envolver(app.ui(), () -> frame);
    }

    public static void main(String[] args) {
        String modo = args.length > 0 ? args[0] : "--mysql";
        AppFactory.Aplicacion app = switch (modo) {
            case "--memoria" -> AppFactory.crear(Repositorios.enMemoria(), new PoliticaCancelacionEstandar(),
                    UnitOfWork.DIRECTO);
            case "--h2" -> {
                BaseDeDatos.iniciarH2();
                yield AppFactory.crear(Repositorios.jpa(), new PoliticaCancelacionEstandar(), new JpaUnitOfWork());
            }
            default -> {
                BaseDeDatos.iniciarMySql();
                yield AppFactory.crear(Repositorios.jpa(), new PoliticaCancelacionEstandar(), new JpaUnitOfWork());
            }
        };
        DatosDemo.sembrarSiHaceFalta(app);

        Theme.install();
        ErroresEnDialogo.silenciarErroresYaMostrados();
        SwingUtilities.invokeLater(() -> new SkillSwapRealApplication(app).mostrar());
    }

    private void mostrar() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1120, 700));
        frame.setSize(1365, 820);
        frame.setLocationRelativeTo(null);
        frame.setJMenuBar(menu());
        if (elegirUsuario()) {
            refrescar();
        }
        frame.setVisible(true);
    }

    /** Vuelve a construir la vista del kit para el usuario activo (recarga saldo, badge, etc.). */
    private void refrescar() {
        frame.setTitle("SkillSwap — sesion de " + usuarioActivo);
        frame.setContentPane(new SkillSwapView(ui, usuarioActivo));
        frame.revalidate();
        frame.repaint();
    }

    private JMenuBar menu() {
        JMenuBar barra = new JMenuBar();
        JMenu sesion = new JMenu("Sesion");
        sesion.add(item("Cambiar de usuario (login simulado)...", () -> {
            if (elegirUsuario()) {
                refrescar();
            }
        }));
        JMenu acciones = new JMenu("Acciones");
        acciones.add(item("Calificar una contratacion completada...", this::calificar));
        acciones.add(item("Proponer reprogramacion...", this::proponerReprogramacion));
        acciones.add(item("Responder una reprogramacion propuesta...", this::responderReprogramacion));
        acciones.add(item("Elegir canal de notificaciones...", this::elegirCanal));
        barra.add(sesion);
        barra.add(acciones);
        return barra;
    }

    private JMenuItem item(String texto, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(e -> accion.run());
        return item;
    }

    private boolean elegirUsuario() {
        String[] opciones = {DatosDemo.MATEO + "  (Mateo, cliente/profesional)", DatosDemo.SOFIA + "  (Sofia, mentora)",
                DatosDemo.TECHCORP + "  (TechCorp, organizacion)"};
        Object elegido = JOptionPane.showInputDialog(frame, "Elegi con que usuario entrar (login simulado):",
                "SkillSwap - Ingresar", JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (elegido == null) {
            return false;
        }
        usuarioActivo = elegido.toString().split(" ")[0];
        return true;
    }

    private void calificar() {
        List<ContratacionDTO> completadas = contrataciones().stream()
                .filter(c -> "COMPLETADA".equals(c.estado())).toList();
        ContratacionDTO elegida = elegir("No tenes contrataciones completadas para calificar.", completadas);
        if (elegida == null) {
            return;
        }
        JComboBox<Integer> puntaje = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        JTextField comentario = new JTextField(24);
        JPanel form = new JPanel(new java.awt.GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Puntaje"));
        form.add(puntaje);
        form.add(new JLabel("Comentario"));
        form.add(comentario);
        if (JOptionPane.showConfirmDialog(frame, form, "Calificar " + elegida.titulo(), JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        informar(app.calificaciones().calificar(usuarioActivo,
                new CalificarComando(elegida.id(), (Integer) puntaje.getSelectedItem(), comentario.getText())),
                "Calificacion registrada. La reputacion de la otra parte se recalculo.");
    }

    private void proponerReprogramacion() {
        List<ContratacionDTO> reprogramables = contrataciones().stream()
                .filter(c -> List.of("ACEPTADA", "CONFIRMADA").contains(c.estado()) && c.franjaPropuesta() == null)
                .toList();
        ContratacionDTO elegida = elegir("No tenes reservas ACEPTADAS/CONFIRMADAS para reprogramar.", reprogramables);
        if (elegida == null) {
            return;
        }
        List<FranjaDTO> franjas = app.publicaciones().verDetalle(elegida.publicacionId()).valorOLanzar().franjas().stream()
                .filter(f -> !f.id().equals(elegida.franja().id()) && f.cupoDisponible() > 0).toList();
        if (franjas.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "La publicacion no tiene otras franjas con cupo.");
            return;
        }
        String[] opciones = franjas.stream().map(f -> f.inicio().format(FORMATO) + " (cupo " + f.cupoDisponible() + ")")
                .toArray(String[]::new);
        Object elegido = JOptionPane.showInputDialog(frame, "Nueva franja:", "Proponer reprogramacion",
                JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (elegido == null) {
            return;
        }
        FranjaDTO nueva = franjas.get(java.util.Arrays.asList(opciones).indexOf(elegido.toString()));
        informar(app.contrataciones().proponerReprogramacion(usuarioActivo, elegida.id(), nueva.id()),
                "Propuesta enviada. La otra parte tiene que aceptarla.");
    }

    private void responderReprogramacion() {
        List<ContratacionDTO> pendientes = contrataciones().stream()
                .filter(c -> c.franjaPropuesta() != null).toList();
        ContratacionDTO elegida = elegir("No hay reprogramaciones pendientes.", pendientes);
        if (elegida == null) {
            return;
        }
        int respuesta = JOptionPane.showOptionDialog(frame, "Pasar '" + elegida.titulo() + "' del "
                        + elegida.franja().inicio().format(FORMATO) + " al " + elegida.franjaPropuesta().inicio().format(FORMATO) + "?",
                "Reprogramacion", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"Aceptar", "Rechazar", "Cerrar"}, "Aceptar");
        if (respuesta == 0) {
            informar(app.contrataciones().aceptarReprogramacion(usuarioActivo, elegida.id()), "Reserva reprogramada.");
        } else if (respuesta == 1) {
            informar(app.contrataciones().rechazarReprogramacion(usuarioActivo, elegida.id()), "Propuesta rechazada.");
        }
    }

    private void elegirCanal() {
        Object canal = JOptionPane.showInputDialog(frame, "Por donde queres recibir las notificaciones?", "Canal",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"INTERNA", "EMAIL", "PUSH"}, "INTERNA");
        if (canal != null) {
            informar(app.perfiles().elegirCanalPreferido(usuarioActivo, canal.toString()), "Canal actualizado.");
        }
    }

    private List<ContratacionDTO> contrataciones() {
        return app.contrataciones().listar(usuarioActivo, 0, 100).valorOLanzar().contenido();
    }

    private ContratacionDTO elegir(String siNoHay, List<ContratacionDTO> candidatas) {
        if (candidatas.isEmpty()) {
            JOptionPane.showMessageDialog(frame, siNoHay);
            return null;
        }
        String[] opciones = candidatas.stream()
                .map(c -> c.titulo() + " — " + (c.clienteId().equals(usuarioActivo) ? c.proveedorNombre() : c.clienteNombre())
                        + " — " + c.estado() + " [" + c.id().substring(0, 6) + "]")
                .toArray(String[]::new);
        Object elegido = JOptionPane.showInputDialog(frame, "Elegi la contratacion:", "Contrataciones",
                JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        return elegido == null ? null : candidatas.get(java.util.Arrays.asList(opciones).indexOf(elegido.toString()));
    }

    private void informar(Resultado<?> resultado, String exito) {
        if (resultado.exito()) {
            JOptionPane.showMessageDialog(frame, exito);
            refrescar();
        } else {
            ErroresEnDialogo.mostrar(frame, new com.uade.skillswap.presentation.controller.ErrorDeAplicacionException(
                    resultado.codigoError(), resultado.mensaje()));
        }
    }
}
