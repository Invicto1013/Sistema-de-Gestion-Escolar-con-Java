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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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

    @FXML
    private Label lblBienvenida;

    @FXML
    private Label lblRol;

    @FXML
    private VBox menuLateral;

    @FXML
    private VBox contenido;

    @FXML
    private BorderPane layoutRoot;

    @FXML
    private HBox appHeader;

    @FXML
    private Button btnCerrarSesion;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final List<Button> opcionesActivas = new ArrayList<>();
    private final ComboBox<String> menuCompacto = new ComboBox<>();
    private final VBox encabezadoCompacto = new VBox(6);
    private boolean actualizandoMenuCompacto;
    private int ultimaBandaResponsive = -1;

    @FXML
    private void initialize() {
        Usuario usuario = SesionActual.getUsuario();

        if (usuario == null) {
            // Se abrio el panel sin haber iniciado sesion: volver al login cuando la escena este lista.
            Platform.runLater(this::volverALogin);
            return;
        }

        layoutRoot.setMinWidth(360);
        layoutRoot.setMinHeight(420);
        contenido.setMinWidth(0);
        menuLateral.setMinWidth(0);

        lblBienvenida.setText("Bienvenido, " + usuario.getNombreCompleto());
        lblRol.setText(etiquetaRol(usuario.getRol()));

        construirMenu(usuario);
        mostrarInicio(usuario);
        menuCompacto.setPromptText("Abrir menú");
        menuCompacto.setMaxWidth(Double.MAX_VALUE);
        menuCompacto.getStyleClass().add("mobile-menu");
        menuCompacto.setOnAction(e -> {
            if (actualizandoMenuCompacto) return;
            int indice=menuCompacto.getSelectionModel().getSelectedIndex();
            if(indice>=0 && indice<opcionesActivas.size()) opcionesActivas.get(indice).fire();
        });
        menuLateral.sceneProperty().addListener((obs,anterior,scene)->{
            if(scene!=null){scene.widthProperty().addListener((o,ancho,nuevo)->
                    Platform.runLater(()->adaptarLayout(scene.getWidth())));
                Platform.runLater(()->adaptarLayout(scene.getWidth()));}
        });
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
        Button inicio = agregarOpcion("Inicio", () -> mostrarInicio(usuario));
        marcarActiva(inicio);
        agregarOpcion("Mi perfil", () -> mostrarPerfil(usuario));

        switch (usuario.getRol()) {
            case ADMIN -> {
                agregarOpcion("Usuarios", this::mostrarUsuarios);
                agregarOpcion("Historial de sesiones", () -> mostrarPanelAdmin("Historial de sesiones",
                        () -> new HistorialSesionesPanel(usuario.getId())));
                agregarOpcion("Crear profesor", () -> mostrarPanelAdmin("Crear profesor",
                        () -> new CrearProfesorPanel(usuario.getId())));
                agregarOpcion("Asignaturas", () -> mostrarPanelAdmin("Asignaturas", AsignaturasPanel::new));
                agregarOpcion("Grados y currículo", () -> mostrarPanelAdmin("Grados y currículo", CurriculoPanel::new));
                agregarOpcion("Secciones", () -> mostrarPanelAdmin("Secciones", SeccionesPanel::new));
                agregarOpcion("Inscripciones", () -> mostrarPanelAdmin("Inscripciones", InscripcionesPanel::new));
                agregarOpcion("Calificaciones", () -> mostrarPanelAdmin("Administración de calificaciones",
                        () -> new CalificacionesPanel(usuario.getId(), true)));
                agregarOpcion("Vincular padres", () -> mostrarPanelAdmin("Vinculación de padres",
                        VinculacionesPanel::new));
                agregarOpcion("Boletas", () -> mostrarPanelAdmin("Boletas",
                        () -> new BoletaPanel(Rol.ADMIN, usuario.getId())));
                agregarOpcion("Reporte de inscripciones", () -> mostrarPanelAdmin("Reporte de inscripciones",
                        () -> new ReporteInscripcionesPanel()));
            }
            case PROFESOR -> {
                agregarOpcion("Mis secciones", () -> mostrarPanelPropio(new MisSeccionesPanel(usuario.getId())));
                agregarOpcion("Calificaciones", () -> mostrarPanelPropio(new CalificacionesPanel(usuario.getId())));
                agregarOpcion("Asistencia", () -> mostrarPanelPropio(new AsistenciaPanel(usuario.getId())));
                agregarOpcion("Reporte de inscripciones", () -> mostrarPanelPropio(new ReporteInscripcionesPanel()));
            }
            case ESTUDIANTE -> {
                agregarOpcion("Mis materias", () -> mostrarPanelPropio(new MisMateriasPanel(usuario.getId())));
                agregarOpcion("Mis calificaciones",
                        () -> mostrarPanelPropio(new MisCalificacionesPanel(usuario.getId())));
                agregarOpcion("Boleta", () -> mostrarPanelPropio(new BoletaPanel(Rol.ESTUDIANTE, usuario.getId())));
                agregarOpcion("Mi asistencia", () -> mostrarPanelPropio(new MiAsistenciaPanel(usuario.getId())));
                agregarOpcion("Reporte de inscripciones", () -> mostrarPanelPropio(new ReporteInscripcionesPanel()));
            }
            case PADRE -> {
                agregarOpcion("Mis hijos", () -> mostrarPanelPropio(new MisHijosPanel(usuario.getId())));
                agregarOpcion("Calificaciones", () -> mostrarPanelPropio(new CalificacionesHijosPanel(usuario.getId())));
                agregarOpcion("Asistencia", () -> mostrarPanelPropio(new AsistenciaHijosPanel(usuario.getId())));
                agregarOpcion("Boleta", () -> mostrarPanelPropio(new BoletaPanel(Rol.PADRE, usuario.getId())));
                agregarOpcion("Reporte de inscripciones", () -> mostrarPanelPropio(new ReporteInscripcionesPanel()));
            }
        }
    }

    private void adaptarLayout(double ancho) {
        int banda=ancho<460?0:ancho<900?1:ancho<1024?2:3;
        if(banda==ultimaBandaResponsive)return;
        ultimaBandaResponsive=banda;
        boolean telefono=ancho<900;
        lblRol.setVisible(!telefono);lblRol.setManaged(!telefono);
        btnCerrarSesion.setText(ancho<460?"Salir":"Cerrar sesión");
        menuLateral.setPrefWidth(ancho<1024?160:200);
        if(telefono) {
            if(layoutRoot.getTop()!=encabezadoCompacto) {
                layoutRoot.setTop(null);layoutRoot.setLeft(null);
                encabezadoCompacto.getChildren().setAll(appHeader,menuCompacto);
                layoutRoot.setTop(encabezadoCompacto);
            }
            actualizandoMenuCompacto=true;
            menuCompacto.getItems().setAll(opcionesActivas.stream().map(Button::getText).toList());
            int activa=0;for(int n=0;n<opcionesActivas.size();n++)if(opcionesActivas.get(n).getStyleClass().contains("sidebar-option-active")){activa=n;break;}
            if(!opcionesActivas.isEmpty())menuCompacto.getSelectionModel().select(activa);
            actualizandoMenuCompacto=false;
        } else {
            if(layoutRoot.getTop()==encabezadoCompacto) {
                layoutRoot.setTop(null);encabezadoCompacto.getChildren().clear();
                layoutRoot.setTop(appHeader);layoutRoot.setLeft(menuLateral);
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
        boton.getStyleClass().add("sidebar-option");
        return boton;
    }

    private void marcarActiva(Button activa) {
        for (Button boton : opcionesActivas) {
            boton.getStyleClass().remove("sidebar-option-active");
            if (!boton.getStyleClass().contains("sidebar-option")) boton.getStyleClass().add("sidebar-option");
        }
        activa.getStyleClass().remove("sidebar-option");
        activa.getStyleClass().add("sidebar-option-active");
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
            Dialogos.aplicarEstilos(alerta);
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

            TableColumn<Usuario, String> colEstado = new TableColumn<>("Estado");
            colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().isActivo() ? "Activa" : "Desactivada"));

            tabla.getColumns().addAll(colId, colNombre, colCorreo, colRol, colEstado);
            tabla.getItems().addAll(usuarios);
            VBox.setVgrow(tabla, Priority.ALWAYS);

            Button estadoCuenta = new Button("Activar / desactivar seleccionada");
            estadoCuenta.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
            estadoCuenta.setOnAction(e -> {
                Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
                if (seleccionado == null) return;
                boolean nuevoEstado = !seleccionado.isActivo();
                String accion = nuevoEstado ? "activar" : "desactivar";
                if (!Dialogos.confirmar("Cambiar estado de cuenta",
                        "¿Deseas " + accion + " la cuenta de " + seleccionado.getNombreCompleto() + "?")) return;
                try {
                    Usuario actual = SesionActual.getUsuario();
                    usuarioDAO.cambiarEstadoCuentaComoAdmin(actual.getId(), seleccionado.getId(), nuevoEstado);
                    seleccionado.setActivo(nuevoEstado);
                    tabla.refresh();
                } catch (PermisoDenegadoException ex) {
                    Dialogos.error(ex.getMessage());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Dialogos.error("No se pudo cambiar el estado de la cuenta. Revisa la conexión con MySQL.");
                }
            });

            Button resetearDosFactores = new Button("Restablecer 2FA");
            resetearDosFactores.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
            resetearDosFactores.setOnAction(e -> {
                Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
                if (seleccionado == null || !Dialogos.confirmar("Restablecer 2FA",
                        "La próxima verificación de dos factores de " + seleccionado.getNombreCompleto()
                                + " deberá configurarse de nuevo. ¿Continuar?")) return;
                try {
                    Usuario actual = SesionActual.getUsuario();
                    usuarioDAO.restablecer2FAComoAdmin(actual.getId(), seleccionado.getId());
                    Dialogos.informacion("Se restableció el segundo factor. El usuario deberá activarlo de nuevo.");
                } catch (PermisoDenegadoException ex) {
                    Dialogos.error(ex.getMessage());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Dialogos.error("No se pudo restablecer el 2FA. Revisa la conexión con MySQL.");
                }
            });

            Button cambiarRol = new Button("Cambiar rol");
            cambiarRol.disableProperty().bind(tabla.getSelectionModel().selectedItemProperty().isNull());
            cambiarRol.setOnAction(e -> {
                Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
                if (seleccionado != null && pedirCambioRol(seleccionado)) mostrarUsuarios();
            });

            Button crearProfesor = new Button("Crear usuario profesor");
            crearProfesor.setOnAction(e -> mostrarPanelAdmin("Crear profesor",
                    () -> new CrearProfesorPanel(SesionActual.getUsuario().getId())));
            HBox acciones = new HBox(8, crearProfesor, cambiarRol, estadoCuenta, resetearDosFactores);
            mostrar(titulo("Administración de usuarios"),
                    new Label("Total: " + usuarios.size() + " usuario(s)"), acciones, tabla);
        } catch (SQLException e) {
            e.printStackTrace();
            mostrar(titulo("Usuarios"),
                    mensajeError("No se pudo leer la lista de usuarios. Revisa la conexión con MySQL."));
        }
    }

    private boolean pedirCambioRol(Usuario seleccionado) {
        Usuario actual = SesionActual.getUsuario();
        ComboBox<Rol> rol = new ComboBox<>();
        rol.getItems().setAll(Rol.values());
        rol.setValue(seleccionado.getRol());
        TextField matricula = new TextField(); matricula.setPromptText("Matrícula (estudiante)");
        TextField grado = new TextField(); grado.setPromptText("Grado (estudiante)");
        TextField codigoProfesor = new TextField(); codigoProfesor.setPromptText("Código (profesor)");
        TextField especialidad = new TextField(); especialidad.setPromptText("Especialidad (profesor)");
        TextField telefono = new TextField(); telefono.setPromptText("Teléfono (padre/tutor)");
        TextField ocupacion = new TextField(); ocupacion.setPromptText("Ocupación (padre/tutor)");
        Label error = new Label(); error.getStyleClass().add("error-message"); error.setWrapText(true);

        GridPane formulario = new GridPane();
        formulario.setHgap(10); formulario.setVgap(8); formulario.setPadding(new Insets(16));
        formulario.addRow(0, new Label("Cuenta:"), new Label(seleccionado.getNombreCompleto()
                + " · " + seleccionado.getCorreo()));
        formulario.addRow(1, new Label("Rol nuevo:"), rol);
        formulario.addRow(2, new Label("Matrícula:"), matricula);
        formulario.addRow(3, new Label("Grado:"), grado);
        formulario.addRow(4, new Label("Código profesor:"), codigoProfesor);
        formulario.addRow(5, new Label("Especialidad:"), especialidad);
        formulario.addRow(6, new Label("Teléfono:"), telefono);
        formulario.addRow(7, new Label("Ocupación:"), ocupacion);
        formulario.add(error, 0, 8, 2, 1);
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambiar rol y perfil");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(formulario);
        Button aceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        aceptar.addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            Rol nuevo = rol.getValue();
            String mensaje = validarDatosRol(nuevo, matricula.getText(), grado.getText(),
                    codigoProfesor.getText(), especialidad.getText(), telefono.getText(), ocupacion.getText());
            if (mensaje != null) { error.setText(mensaje); evento.consume(); }
        });
        Dialogos.prepararModal(dialogo, menuLateral.getScene().getWindow());
        if (dialogo.showAndWait().filter(r -> r == ButtonType.OK).isEmpty()) return false;

        try {
            usuarioDAO.cambiarRolComoAdmin(actual.getId(), seleccionado.getId(), rol.getValue(),
                    matricula.getText(), grado.getText(), codigoProfesor.getText(), especialidad.getText(),
                    telefono.getText(), ocupacion.getText());
            Dialogos.informacion("Rol actualizado. La cuenta deberá cambiar su contraseña en el próximo ingreso.");
            return true;
        } catch (PermisoDenegadoException ex) {
            Dialogos.error(ex.getMessage());
        } catch (SQLException ex) {
            ex.printStackTrace();
            Dialogos.error("No se pudo cambiar el rol. Revisa los datos y la conexión con MySQL.");
        }
        return false;
    }

    private String validarDatosRol(Rol rol, String matricula, String grado, String codigo,
            String especialidad, String telefono, String ocupacion) {
        if (rol == null) return "Selecciona el rol nuevo.";
        if (rol == Rol.ESTUDIANTE) {
            if (matricula == null || matricula.isBlank() || grado == null || grado.isBlank())
                return "Para el rol estudiante, indica matrícula y grado.";
            if (matricula.trim().length() > 20 || grado.trim().length() > 30)
                return "La matrícula admite 20 caracteres y el grado 30.";
        }
        if (rol == Rol.PROFESOR) {
            if (codigo == null || codigo.isBlank() || especialidad == null || especialidad.isBlank())
                return "Para el rol profesor, indica código y especialidad.";
            if (codigo.trim().length() > 20 || especialidad.trim().length() > 100)
                return "El código admite 20 caracteres y la especialidad 100.";
        }
        if (rol == Rol.PADRE) {
            if (telefono != null && telefono.trim().length() > 30) return "El teléfono admite 30 caracteres.";
            if (ocupacion != null && ocupacion.trim().length() > 100) return "La ocupación admite 100 caracteres.";
        }
        return null;
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

    private void mostrarInicio(Usuario usuario) {
        DashboardInicioPanel panel = new DashboardInicioPanel(usuario);
        VBox.setVgrow(panel, Priority.ALWAYS);
        mostrar(panel);
    }

    private Label titulo(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("section-title");
        etiqueta.setPadding(new Insets(0, 0, 8, 0));
        return etiqueta;
    }

    private Label mensajeError(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("error-message");
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    /**
     * Agrega una fila "etiqueta: valor" al GridPane y devuelve el numero de la siguiente fila.
     */
    private int agregarDato(GridPane cuadricula, int fila, String etiqueta, String valor) {
        Label nombre = new Label(etiqueta + ":");
        nombre.getStyleClass().add("strong-text");
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
