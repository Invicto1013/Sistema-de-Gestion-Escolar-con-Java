package com.unev.sige.controller;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Optional;

/** Formulario reutilizable para cambiar la contraseña de una cuenta. */
final class CambioContrasenaDialog {
    record Resultado(String actual, String nueva) { }

    private CambioContrasenaDialog() { }

    static Optional<Resultado> mostrar(Window owner, boolean pedirActual) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambiar contraseña");
        dialogo.setHeaderText(pedirActual ? "Confirma tu contraseña actual y escribe una nueva."
                : "Debes reemplazar la contraseña inicial para continuar.");
        if (owner != null) dialogo.initOwner(owner);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.OK);
        if (pedirActual) dialogo.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        PasswordField actual = new PasswordField();
        actual.setPromptText("Contraseña actual");
        PasswordField nueva = new PasswordField();
        nueva.setPromptText("Mínimo 4 caracteres");
        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Repite la nueva contraseña");
        Label error = new Label();
        error.setTextFill(Color.RED);
        error.setWrapText(true);

        GridPane contenido = new GridPane();
        contenido.setHgap(10);
        contenido.setVgap(10);
        contenido.setPadding(new Insets(14));
        int fila = 0;
        if (pedirActual) contenido.addRow(fila++, new Label("Contraseña actual:"), actual);
        contenido.addRow(fila++, new Label("Nueva contraseña:"), nueva);
        contenido.addRow(fila++, new Label("Confirmar contraseña:"), confirmar);
        contenido.add(error, 0, fila, 2, 1);
        dialogo.getDialogPane().setContent(contenido);

        dialogo.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, evento -> {
            String mensaje = null;
            if (pedirActual && (actual.getText() == null || actual.getText().isBlank())) {
                mensaje = "Escribe tu contraseña actual.";
            } else if (nueva.getText() == null || nueva.getText().length() < 4) {
                mensaje = "La nueva contraseña debe tener al menos 4 caracteres.";
            } else if (!nueva.getText().equals(confirmar.getText())) {
                mensaje = "Las contraseñas nuevas no coinciden.";
            } else if (pedirActual && nueva.getText().equals(actual.getText())) {
                mensaje = "La nueva contraseña debe ser distinta de la actual.";
            }
            if (mensaje != null) {
                error.setText(mensaje);
                evento.consume();
            }
        });

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.filter(b -> b == ButtonType.OK).isEmpty()) return Optional.empty();
        return Optional.of(new Resultado(pedirActual ? actual.getText() : null, nueva.getText()));
    }
}
