package com.unev.sige.controller;

import com.unev.sige.app.Navegacion;
import com.unev.sige.app.SesionActual;
import com.unev.sige.data.SesionDAO;
import com.unev.sige.data.PermisoDenegadoException;
import com.unev.sige.data.UsuarioDAO;
import com.unev.sige.model.ControladorSesion;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.Profesor;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Controlador del panel principal (dashboard.fxml).
 * Muestra un menu distinto segun el rol del usuario que inicio sesion
 * (los modulos que aun no existen aparecen deshabilitados).
 */
@SuppressWarnings("unused")
public class DashboardController {

    private static final String ESTILO_OPCION = "-fx-alignment: CENTER_LEFT;";
    private static final String ESTILO_OPCION_ACTIVA = "-fx-alignment: CENTER_LEFT; -fx-font-weight: bold;";

    @FXML
    private Label lblBienvenida;

    @FXML
    private Label lblRol;

    @FXML
    private VBox menuLateral;

    @FXML
    private VBox contenido;

    @FXML
    private Button btnCerrarSesion;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final List<Button> opcionesActivas = new ArrayList<>();

    @FXML
    private void initialize() {
        Usuario usuario = SesionActual.getUsuario();

        if (usuario == null) {
            // Se abrio el panel sin haber iniciado sesion: volver al login cuando la escena este lista.
            Platform.runLater(this::volverALogin);
            return;
        }

        lblBienvenida.setText("Bienvenido, " + usuario.getNombreCompleto());
        lblRol.setText(etiquetaRol(usuario.getRol()));

        construirMenu(usuario);
        mostrarPerfil(usuario);
    }

    @FXML
    private void onCerrarSesion() {
        ControladorSesion sesion = SesionActual.getSesion();
        if (sesion != null) {
            try {
                new SesionDAO().cerrar(sesion.getIdSesion());
            } catch (SQLException e) {
                e.printStackTrace(); // no impide cerrar la sesion en la aplicacion
            }
        }
        SesionActual.cerrar();
        volverALogin();
    }

    // ----- Menu segun rol -----

    private void construirMenu(Usuario usuario) {
        Button perfil = agregarOpcion("Mi perfil", () -> mostrarPerfil(usuario));
        marcarActiva(perfil);

        switch (usuario.getRol()) {
            case ADMIN -> {
                agregarOpcion("Usuarios", this::mostrarUsuarios);
                agregarOpcion("Historial de sesiones", () -> mostrarPanelAdmin("Historial de sesiones",
                        () -> new HistorialSesionesPanel(usuario.getId())));
                agregarOpcion("Crear profesor", () -> mostrarPanelAdmin("Crear profesor",
                        () -> new CrearProfesorPanel(usuario.getId())));
                agregarOpcion("Asignaturas", () -> mostrarPanelAdmin("Asignaturas", AsignaturasPanel::new));
                agregarOpcion("Secciones", () -> mostrarPanelAdmin("Secciones", SeccionesPanel::new));
                agregarOpcion("Inscripciones", () -> mostrarPanelAdmin("Inscripciones", InscripcionesPanel::new));
                agregarOpcion("Calificaciones", () -> mostrarPanelAdmin("Administración de calificaciones",
                        () -> new CalificacionesPanel(usuario.getId(), true)));
                agregarOpcion("Vincular padres", () -> mostrarPanelAdmin("Vinculación de padres",
                        VinculacionesPanel::new));
                agregarOpcion("Boletas", () -> mostrarPanelAdmin("Boletas",
                        () -> new BoletaPanel(Rol.ADMIN, usuario.getId())));
            }
            case PROFESOR -> {
                agregarOpcion("Mis secciones", () -> mostrarPanelPropio(new MisSeccionesPanel(usuario.getId())));
                agregarOpcion("Calificaciones", () -> mostrarPanelPropio(new CalificacionesPanel(usuario.getId())));
                agregarOpcion("Asistencia", () -> mostrarPanelPropio(new AsistenciaPanel(usuario.getId())));
            }
            case ESTUDIANTE -> {
                agregarOpcion("Mis materias", () -> mostrarPanelPropio(new MisMateriasPanel(usuario.getId())));
                agregarOpcion("Mis calificaciones",
                        () -> mostrarPanelPropio(new MisCalificacionesPanel(usuario.getId())));
                agregarOpcion("Boleta", () -> mostrarPanelPropio(new BoletaPanel(Rol.ESTUDIANTE, usuario.getId())));
                agregarOpcion("Mi asistencia", () -> mostrarPanelPropio(new MiAsistenciaPanel(usuario.getId())));
            }
            case PADRE -> {
                agregarOpcion("Mis hijos", () -> mostrarPanelPropio(new MisHijosPanel(usuario.getId())));
                agregarOpcion("Calificaciones", () -> mostrarPanelPropio(new CalificacionesHijosPanel(usuario.getId())));
                agregarOpcion("Asistencia", () -> mostrarPanelPropio(new AsistenciaHijosPanel(usuario.getId())));
                agregarOpcion("Boleta", () -> mostrarPanelPropio(new BoletaPanel(Rol.PADRE, usuario.getId())));
            }
        }
    }

    private Button agregarOpcion(String texto, Runnable accion) {
        Button boton = crearBotonMenu(texto);
        boton.setOnAction(e -> {
            marcarActiva(boton);
            accion.run();
        });
        opcionesActivas.add(boton);
        menuLateral.getChildren().add(boton);
        return boton;
    }

    private void agregarProximamente(String texto) {
        Button boton = crearBotonMenu(texto + " (pronto)");
        boton.setDisable(true);
        menuLateral.getChildren().add(boton);
    }

    private Button crearBotonMenu(String texto) {
        Button boton = new Button(texto);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setStyle(ESTILO_OPCION);
        return boton;
    }

    private void marcarActiva(Button activa) {
        for (Button boton : opcionesActivas) {
            boton.setStyle(ESTILO_OPCION);
        }
        activa.setStyle(ESTILO_OPCION_ACTIVA);
    }

    // ----- Vistas del contenido -----

    private void mostrarPerfil(Usuario usuario) {
        GridPane datos = new GridPane();
        datos.setHgap(16);
        datos.setVgap(10);

        int fila = 0;
        fila = agregarDato(datos, fila, "Nombre", usuario.getNombreCompleto());
        fila = agregarDato(datos, fila, "Correo", usuario.getCorreo());
        fila = agregarDato(datos, fila, "Rol", etiquetaRol(usuario.getRol()));

        if (usuario instanceof Estudiante e) {
            fila = agregarDato(datos, fila, "Matrícula", e.getMatricula());
            fila = agregarDato(datos, fila, "Grado", e.getGrado());
            agregarDato(datos, fila, "Promedio académico", String.format("%.2f", e.getPromedioAcademico()));
        } else if (usuario instanceof Profesor p) {
            fila = agregarDato(datos, fila, "Código de profesor", p.getCodigoProfesor());
            agregarDato(datos, fila, "Especialidad", p.getEspecialidad());
        } else if (usuario instanceof Padre pa) {
            fila = agregarDato(datos, fila, "Teléfono", valorOVacio(pa.getTelefonoContacto()));
            agregarDato(datos, fila, "Ocupación", valorOVacio(pa.getOcupacion()));
        }

        Button cambiarContrasena = new Button("Cambiar contraseña");
        cambiarContrasena.setOnAction(e -> cambiarContrasena(usuario));
        mostrar(titulo("Mi perfil"), datos, cambiarContrasena);
    }

    private void cambiarContrasena(Usuario usuario) {
        var resultado = CambioContrasenaDialog.mostrar(lblBienvenida.getScene().getWindow(), true);
        if (resultado.isEmpty()) return;
        try {
            usuarioDAO.cambiarContrasena(usuario.getId(), resultado.get().actual(), resultado.get().nueva());
            usuario.cambiarContrasena(resultado.get().nueva());
            usuario.setDebeCambiarContrasena(false);
            Alert alerta = new Alert(Alert.AlertType.INFORMATION, "Tu contraseña se actualizó correctamente.");
            alerta.setTitle("SIGE");
            alerta.setHeaderText(null);
            alerta.showAndWait();
        } catch (PermisoDenegadoException e) {
            Dialogos.error(e.getMessage());
        } catch (SQLException e) {
            Dialogos.error("No se pudo actualizar la contraseña. Revisa la conexión con MySQL.");
        }
    }

    private void mostrarUsuarios() {
        if (!esAdminActual()) {
            mostrar(titulo("Usuarios"), mensajeError("Solo el administrador puede ver esta sección."));
            return;
        }

        try {
            List<Usuario> usuarios = usuarioDAO.listarTodos();

            TableView<Usuario> tabla = new TableView<>();
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Usuario, Number> colId = new TableColumn<>("ID");
            colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
            colId.setMaxWidth(80);

            TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre");
            colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCompleto()));

            TableColumn<Usuario, String> colCorreo = new TableColumn<>("Correo");
            colCorreo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCorreo()));

            TableColumn<Usuario, String> colRol = new TableColumn<>("Rol");
            colRol.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getRol() == null ? "?" : c.getValue().getRol().name()));

            tabla.getColumns().addAll(colId, colNombre, colCorreo, colRol);
            tabla.getItems().addAll(usuarios);
            VBox.setVgrow(tabla, Priority.ALWAYS);

            mostrar(titulo("Usuarios"), new Label("Total: " + usuarios.size() + " usuario(s)"), tabla);
        } catch (SQLException e) {
            e.printStackTrace();
            mostrar(titulo("Usuarios"),
                    mensajeError("No se pudo leer la lista de usuarios. Revisa la conexión con MySQL."));
        }
    }

    /**
     * Muestra una pantalla de administracion (asignaturas, secciones...) solo si quien tiene
     * la sesion abierta es el administrador.
     */
    private void mostrarPanelAdmin(String tituloVista, Supplier<? extends Node> crearPanel) {
        if (!esAdminActual()) {
            mostrar(titulo(tituloVista), mensajeError("Solo el administrador puede ver esta sección."));
            return;
        }
        Node panel = crearPanel.get();
        VBox.setVgrow(panel, Priority.ALWAYS);
        mostrar(panel);
    }

    /**
     * Muestra una pantalla del propio usuario (profesor o estudiante) ocupando todo el alto disponible.
     */
    private void mostrarPanelPropio(Node panel) {
        VBox.setVgrow(panel, Priority.ALWAYS);
        mostrar(panel);
    }

    private boolean esAdminActual() {
        Usuario actual = SesionActual.getUsuario();
        return actual != null && actual.getRol() == Rol.ADMIN;
    }

    // ----- Auxiliares -----

    private void mostrar(Node... nodos) {
        contenido.getChildren().setAll(nodos);
    }

    private Label titulo(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        etiqueta.setPadding(new Insets(0, 0, 8, 0));
        return etiqueta;
    }

    private Label mensajeError(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.setTextFill(Color.RED);
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    /**
     * Agrega una fila "etiqueta: valor" al GridPane y devuelve el numero de la siguiente fila.
     */
    private int agregarDato(GridPane cuadricula, int fila, String etiqueta, String valor) {
        Label nombre = new Label(etiqueta + ":");
        nombre.setStyle("-fx-font-weight: bold;");
        cuadricula.add(nombre, 0, fila);
        cuadricula.add(new Label(valorOVacio(valor)), 1, fila);
        return fila + 1;
    }

    private String valorOVacio(String valor) {
        return (valor == null || valor.isBlank()) ? "No registrado" : valor;
    }

    private String etiquetaRol(Rol rol) {
        if (rol == null) {
            return "Sin rol";
        }
        return switch (rol) {
            case ADMIN -> "Administrador";
            case PROFESOR -> "Profesor";
            case ESTUDIANTE -> "Estudiante";
            case PADRE -> "Padre / Tutor";
        };
    }

    private void volverALogin() {
        try {
            Navegacion.irA(lblBienvenida, "login.fxml", "SIGE - Inicio de sesion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
