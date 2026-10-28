package com.unev.sige.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.unev.sige.data.Autenticacion2FADAO;
import com.unev.sige.data.ConexionBD;
import com.unev.sige.model.Usuario;
import com.unev.sige.security.Configuracion2FAException;
import com.unev.sige.security.TOTP;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.sql.SQLException;
import java.util.Map;

/** Inscripción y verificación de TOTP para la autenticación. */
final class Autenticacion2FA {
    private static final Autenticacion2FADAO DAO = new Autenticacion2FADAO();
    private Autenticacion2FA() { }

    static boolean exigir(Window owner, Usuario usuario) throws SQLException, Configuracion2FAException {
        String correo=usuario.getCorreo(); int id=usuario.getId();
        Autenticacion2FADAO.Estado estado=DAO.obtenerEstado(id);
        if(estado.bloqueado()) { auditar(id,correo,"FALLIDO","Cuenta bloqueada temporalmente por 2FA");
            Dialogos.error("La cuenta está bloqueada por 15 minutos debido a intentos 2FA fallidos."); return false; }

        String secretoCifrado=estado.secretoCifrado();
        boolean inscripcion=!estado.activo();
        if(inscripcion) {
            if(secretoCifrado==null || secretoCifrado.isBlank()) {
                String clave=ConexionBD.claveCifrado2FA();
                if(clave==null || clave.isBlank()) throw new Configuracion2FAException(
                        "Falta configurar 2fa.encryption-key en config.properties (o SIGE_2FA_ENCRYPTION_KEY). Genera una clave AES-256 y vuelve a abrir SIGE.");
                secretoCifrado=TOTP.cifrar(TOTP.nuevoSecreto(),clave);
            }
            secretoCifrado=DAO.guardarSecretoPendienteSiFalta(id,secretoCifrado);
        }
        String clave=ConexionBD.claveCifrado2FA();
        if(clave==null || clave.isBlank()) throw new Configuracion2FAException(
                "Falta configurar 2fa.encryption-key en config.properties (o SIGE_2FA_ENCRYPTION_KEY).");
        String secreto=TOTP.descifrar(secretoCifrado,clave);
        if(inscripcion && !mostrarInscripcion(owner,correo,secreto)) {
            auditar(id,correo,"FALLIDO","Inscripción 2FA cancelada"); return false;
        }

        for(int n=0;n<3;n++) {
            TextInputDialog d=new TextInputDialog(); d.setTitle("Verificación de dos factores");
            d.setHeaderText(inscripcion?"Confirma la configuración":"Código de autenticación");
            d.setContentText("Escribe el código de 6 dígitos de tu aplicación:");
            Dialogos.prepararModal(d,owner);
            var respuesta=d.showAndWait();
            if(respuesta.isEmpty()) { auditar(id,correo,"FALLIDO","Verificación 2FA cancelada"); return false; }
            try {
                if(TOTP.verificar(secreto,respuesta.get().trim())) {
                    DAO.registrarExito(id,inscripcion); return true;
                }
            } catch(Exception e) { throw new Configuracion2FAException("No se pudo verificar el código 2FA.",e); }
            Autenticacion2FADAO.Intento intento=DAO.registrarIntentoFallido(id);
            auditar(id,correo,"FALLIDO","Código 2FA incorrecto");
            if(intento.bloqueado()) { Dialogos.error("Código incorrecto. La cuenta queda bloqueada durante 15 minutos."); return false; }
            Dialogos.error("Código incorrecto. Intentos restantes: "+intento.restantes()+".");
        }
        return false;
    }

    private static boolean mostrarInscripcion(Window owner,String correo,String secreto) throws Configuracion2FAException {
        try {
            BitMatrix matrix=new QRCodeWriter().encode(TOTP.uri(correo,secreto), BarcodeFormat.QR_CODE,220,220,
                    Map.of(EncodeHintType.MARGIN,1));
            WritableImage image=new WritableImage(matrix.getWidth(),matrix.getHeight()); PixelWriter writer=image.getPixelWriter();
            for(int y=0;y<matrix.getHeight();y++) for(int x=0;x<matrix.getWidth();x++)
                writer.setArgb(x,y,matrix.get(x,y)?0xFF000000:0xFFFFFFFF);
            Label texto=new Label("Escanea el QR con Google Authenticator, Microsoft Authenticator o una app compatible.");
            texto.setWrapText(true); texto.setMaxWidth(330);
            Label manual=new Label("Si no puedes escanearlo, introduce esta clave: "+secreto);
            manual.setWrapText(true); manual.setMaxWidth(330);
            VBox contenido=new VBox(10,texto,new ImageView(image),manual); contenido.setAlignment(Pos.CENTER);contenido.setPadding(new Insets(12));
            Dialog<ButtonType> d=new Dialog<>();d.setTitle("Configurar autenticación de dos factores");
            d.setHeaderText("Vincula tu cuenta antes de continuar");d.getDialogPane().setContent(contenido);
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
            d.getDialogPane().setPrefWidth(380);Dialogos.prepararModal(d,owner);
            return d.showAndWait().filter(b->b==ButtonType.OK).isPresent();
        } catch(WriterException e) { throw new Configuracion2FAException("No se pudo generar el QR de autenticación.",e); }
    }

    private static void auditar(Integer id,String correo,String resultado,String detalle) {
        try { DAO.registrarAcceso(id,correo,resultado,detalle); }
        catch(SQLException e) { e.printStackTrace(); }
    }
}
