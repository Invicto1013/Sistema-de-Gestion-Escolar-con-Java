package com.unev.sige.controller;

import com.unev.sige.data.CorreoNotificaciones;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/** Método alterno para avisar la creación de un perfil sin servicio de correo: abre el correo del administrador con el aviso listo. */
final class AvisoCorreo {
    private AvisoCorreo() { }

    static Button botonManual(String para, CorreoNotificaciones.Mensaje mensaje, Label estado) {
        Button boton = new Button("Enviar aviso desde mi correo");
        boton.setOnAction(e -> {
            try {
                java.awt.Desktop escritorio = java.awt.Desktop.getDesktop();
                escritorio.mail(java.net.URI.create(CorreoNotificaciones.enlaceMailto(para, mensaje)));
            } catch (Throwable ex) {
                ClipboardContent contenido = new ClipboardContent();
                contenido.putString("Para: " + para + "\nAsunto: " + mensaje.asunto() + "\n\n" + mensaje.cuerpo());
                Clipboard.getSystemClipboard().setContent(contenido);
                Dialogos.estado(estado, "No se pudo abrir tu programa de correo. El aviso se copió al portapapeles: pégalo en un correo para " + para + ".", false);
            }
        });
        return boton;
    }
}
