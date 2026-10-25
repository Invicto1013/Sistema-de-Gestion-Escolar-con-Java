package com.unev.sige.data;

import com.unev.sige.model.Profesor;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

/** Envío opcional de notificaciones mediante el servidor SMTP de la institución. */
public final class CorreoNotificaciones {
    private CorreoNotificaciones() { }

    public static void enviarPerfilProfesor(Profesor p) throws MessagingException {
        enviar(p.getCorreo(),p.getNombreCompleto(),"Tu perfil docente SIGE fue creado","La institución creó tu perfil de profesor en SIGE.\n\nCorreo de acceso: "+p.getCorreo()+"\nCódigo de profesor: "+p.getCodigoProfesor()+"\nEspecialidad: "+p.getEspecialidad()+"\n\nEste mensaje no incluye tu contraseña. Solicita a la administración las instrucciones de acceso.");
    }
    public static void enviarPerfilEstudiante(Estudiante e) throws MessagingException {
        enviar(e.getCorreo(),e.getNombreCompleto(),"Tu perfil de estudiante SIGE fue creado","La institución creó tu perfil de estudiante en SIGE.\n\nCorreo de acceso: "+e.getCorreo()+"\nMatrícula: "+e.getMatricula()+"\nGrado: "+e.getGrado()+"\n\nEste mensaje no incluye tu contraseña. Si no esperabas este mensaje, contacta a la administración.");
    }
    public static void enviarPerfilPadre(Padre p) throws MessagingException {
        enviar(p.getCorreo(),p.getNombreCompleto(),"Tu perfil de padre o tutor SIGE fue creado","La institución creó tu perfil de padre o tutor en SIGE.\n\nCorreo de acceso: "+p.getCorreo()+"\n\nEste mensaje no incluye tu contraseña. La administración debe vincular esta cuenta con el estudiante para consultar su información.");
    }
    /** Solo se notifican direcciones personales de Google y Microsoft; se omiten dominios institucionales. */
    public static boolean aceptaDestinatario(String correo) {
        if (correo == null) return false;
        int arroba = correo.lastIndexOf('@');
        if (arroba < 1 || arroba == correo.length() - 1) return false;
        String dominio = correo.substring(arroba + 1).trim().toLowerCase(java.util.Locale.ROOT);
        return dominio.equals("gmail.com") || dominio.equals("googlemail.com")
                || dominio.equals("outlook.com") || dominio.equals("outlook.es") || dominio.equals("outlook.co.uk")
                || dominio.equals("hotmail.com") || dominio.equals("hotmail.es") || dominio.equals("hotmail.co.uk")
                || dominio.equals("live.com") || dominio.equals("live.es") || dominio.equals("live.co.uk")
                || dominio.equals("msn.com");
    }
    private static void enviar(String correo,String nombre,String asunto,String detalles) throws MessagingException {
        if (!aceptaDestinatario(correo)) throw new IllegalArgumentException("Solo se envían notificaciones a Gmail, Outlook, Hotmail, Live o MSN.");
        String host=configuracion("SIGE_SMTP_HOST","mail.smtp.host"), puerto=valor("SIGE_SMTP_PORT","mail.smtp.port","587");
        String usuario=configuracion("SIGE_SMTP_USER","mail.smtp.user"), clave=configuracion("SIGE_SMTP_PASSWORD","mail.smtp.password"), remitente=configuracion("SIGE_SMTP_FROM","mail.smtp.from");
        boolean auth=Boolean.parseBoolean(valor("SIGE_SMTP_AUTH","mail.smtp.auth","true")), tls=Boolean.parseBoolean(valor("SIGE_SMTP_STARTTLS","mail.smtp.starttls.enable","true")), ssl=Boolean.parseBoolean(valor("SIGE_SMTP_SSL","mail.smtp.ssl.enable","false"));
        if(vacio(host)||vacio(puerto)||vacio(remitente)||(auth&&(vacio(usuario)||vacio(clave)))) throw new IllegalStateException("Configura SMTP antes de enviar correos.");
        if(!aceptaDestinatario(remitente)||(auth&&!aceptaDestinatario(usuario))) throw new IllegalStateException("El remitente SMTP debe ser una cuenta personal Gmail, Outlook, Hotmail, Live o MSN.");
        Properties props=new Properties(); props.put("mail.smtp.host",host); props.put("mail.smtp.port",puerto); props.put("mail.smtp.auth",Boolean.toString(auth)); props.put("mail.smtp.starttls.enable",Boolean.toString(tls)); props.put("mail.smtp.starttls.required",Boolean.toString(tls)); props.put("mail.smtp.ssl.enable",Boolean.toString(ssl)); props.put("mail.smtp.connectiontimeout","10000"); props.put("mail.smtp.timeout","10000"); props.put("mail.smtp.writetimeout","10000");
        Authenticator a=auth?new Authenticator(){@Override protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(usuario,clave);}}:null;
        MimeMessage m=new MimeMessage(Session.getInstance(props,a)); m.setFrom(new InternetAddress(remitente)); m.setRecipient(Message.RecipientType.TO,new InternetAddress(correo,true)); m.setSubject(asunto,StandardCharsets.UTF_8.name()); m.setSentDate(new Date()); m.setText("Hola "+(nombre==null?"usuario":nombre)+",\n\n"+detalles+"\n\nSIGE · Sistema de Gestión Educativa",StandardCharsets.UTF_8.name()); Transport.send(m);
    }
    private static String configuracion(String env, String key) {
        return ConexionBD.configuracion(env, key);
    }

    private static String valor(String env, String key, String predeterminado) {
        String valor = configuracion(env, key);
        return valor == null || valor.isBlank() ? predeterminado : valor.trim();
    }

    private static boolean vacio(String value) { return value == null || value.isBlank(); }
}