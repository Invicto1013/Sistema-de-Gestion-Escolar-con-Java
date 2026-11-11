package com.unev.sige.data;

import com.unev.sige.model.Profesor;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.Padre;
import com.unev.sige.model.Rol;
import com.unev.sige.model.Usuario;
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
        Mensaje m = mensajePerfilProfesor(p);
        enviar(p.getCorreo(), p.getNombreCompleto(), m.asunto(), m.cuerpo());
    }
    public static void enviarPerfilEstudiante(Estudiante e) throws MessagingException {
        Mensaje m = mensajePerfilEstudiante(e);
        enviar(e.getCorreo(), e.getNombreCompleto(), m.asunto(), m.cuerpo());
    }
    public static void enviarPerfilPadre(Padre p) throws MessagingException {
        Mensaje m = mensajePerfilPadre(p);
        enviar(p.getCorreo(), p.getNombreCompleto(), m.asunto(), m.cuerpo());
    }
    public static Mensaje mensajePerfilEstudiante(Estudiante e) {
        return new Mensaje("Tu perfil de estudiante SIGE fue creado", "La institución creó tu perfil de estudiante en SIGE.\n\nCorreo de acceso: "+e.getCorreo()+"\nMatrícula: "+e.getMatricula()+"\nGrado: "+e.getGrado()+"\n\nEste mensaje no incluye tu contraseña. Si no esperabas este mensaje, contacta a la administración.");
    }
    public static Mensaje mensajePerfilPadre(Padre p) {
        return new Mensaje("Tu perfil de padre o tutor SIGE fue creado", "La institución creó tu perfil de padre o tutor en SIGE.\n\nCorreo de acceso: "+p.getCorreo()+"\n\nEste mensaje no incluye tu contraseña. La administración debe vincular esta cuenta con el estudiante para consultar su información.");
    }
    /** Aviso de creación de una cuenta de director/a o secretario/a. */
    public static void enviarPerfilPersonal(Usuario u) throws MessagingException {
        Mensaje m = mensajePerfilPersonal(u);
        enviar(u.getCorreo(), u.getNombreCompleto(), m.asunto(), m.cuerpo());
    }

    /** Asunto y cuerpo de un aviso, para enviarlo por la API, por SMTP o a mano con un enlace mailto. */
    public record Mensaje(String asunto, String cuerpo) { }

    public static Mensaje mensajePerfilPersonal(Usuario u) {
        String cargo = u.getRol() == Rol.DIRECTOR ? "director/a" : "secretario/a";
        return new Mensaje("Tu perfil de " + cargo + " SIGE fue creado",
                "La institución creó tu perfil de " + cargo + " en SIGE.\n\nCorreo de acceso: " + u.getCorreo()
                + "\n\nEste mensaje no incluye tu contraseña. La administración te la entregará; deberás cambiarla al iniciar sesión por primera vez.");
    }
    public static Mensaje mensajePerfilProfesor(Profesor p) {
        return new Mensaje("Tu perfil docente SIGE fue creado", "La institución creó tu perfil de profesor en SIGE.\n\nCorreo de acceso: "+p.getCorreo()+"\nCódigo de profesor: "+p.getCodigoProfesor()+"\nEspecialidad: "+p.getEspecialidad()+"\n\nEste mensaje no incluye tu contraseña. Solicita a la administración las instrucciones de acceso.");
    }

    /** Enlace mailto para que la persona que administra envíe el aviso desde su propio correo, sin configurar nada. */
    public static String enlaceMailto(String para, Mensaje m) {
        return "mailto:" + codificar(para) + "?subject=" + codificar(m.asunto()) + "&body=" + codificar(m.cuerpo());
    }
    private static String codificar(String t) {
        return java.net.URLEncoder.encode(t == null ? "" : t, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Hay un método automático de envío configurado (API HTTPS o SMTP). */
    public static boolean envioAutomaticoConfigurado() {
        return apiConfigurada() || !vacio(configuracion("SIGE_SMTP_HOST", "mail.smtp.host"));
    }

    // ---- Envío por API HTTPS (Brevo). Funciona en hosting gratuito que bloquea los puertos SMTP. ----
    private static boolean apiConfigurada() {
        return !vacio(configuracion("SIGE_BREVO_API_KEY", "mail.api.key")) && !vacio(configuracion("SIGE_MAIL_FROM", "mail.from"));
    }
    private static String jsonTexto(String t) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : (t == null ? "" : t).toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }
    static String cuerpoJsonApi(String remitente, String nombreRemitente, String correo, String nombre, String asunto, String texto) {
        return "{\"sender\":{\"name\":" + jsonTexto(nombreRemitente) + ",\"email\":" + jsonTexto(remitente) + "},"
                + "\"to\":[{\"email\":" + jsonTexto(correo) + ",\"name\":" + jsonTexto(nombre) + "}],"
                + "\"subject\":" + jsonTexto(asunto) + ",\"textContent\":" + jsonTexto(texto) + "}";
    }
    private static void enviarPorApi(String correo, String nombre, String asunto, String texto) throws MessagingException {
        String clave = configuracion("SIGE_BREVO_API_KEY", "mail.api.key").trim();
        String remitente = configuracion("SIGE_MAIL_FROM", "mail.from").trim();
        String nombreRem = valor("SIGE_MAIL_FROM_NAME", "mail.from.name", "SIGE");
        try {
            java.net.http.HttpClient cliente = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("api-key", clave).header("accept", "application/json").header("content-type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(cuerpoJsonApi(remitente, nombreRem, correo, nombre, asunto, texto), StandardCharsets.UTF_8))
                    .build();
            java.net.http.HttpResponse<String> resp = cliente.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) throw new MessagingException("El servicio de correo respondió con el código " + resp.statusCode() + ".");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Envío interrumpido.");
        } catch (java.io.IOException io) {
            throw new MessagingException("No se pudo contactar con el servicio de correo.");
        }
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
        if (apiConfigurada()) { enviarPorApi(correo, nombre, asunto, detalles); return; }
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