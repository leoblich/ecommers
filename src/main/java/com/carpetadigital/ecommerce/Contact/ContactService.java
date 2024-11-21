package com.carpetadigital.ecommerce.Contact;




import com.carpetadigital.ecommerce.entity.dto.ContactRequestDto;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

@Service
public class ContactService {

    @Autowired
    private JavaMailSender mailSender;

    public boolean enviarConsulta(ContactRequestDto contactRequestDto) throws MessagingException, jakarta.mail.MessagingException {

        try {
            enviarCorreoCliente(contactRequestDto);
            enviarCorreoInterno(contactRequestDto);
            return true;
        } catch (Exception e) {
             return false;
        }




    }

    private void enviarCorreoCliente(ContactRequestDto contactRequestDto) throws MessagingException, jakarta.mail.MessagingException {
        // Crear el mensaje de correo
        jakarta.mail.internet.MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);  // El 'true' indica que se enviará un correo HTML con recursos (como imágenes)

        // Establecer el destinatario y el asunto
        helper.setTo(contactRequestDto.getEmail());
        helper.setSubject("Confirmación de Consulta");

        // Crear el cuerpo del mensaje en HTML
        String cuerpoHtml =
                "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<title>Voucher de Suscripción</title>" +
                        "</head>" +
                        "<body style='color: black; font-size: 40px;'>" +
                        "<div style='width: 100%; text-align: center;'>" +
                        "<img src='cid:logoImage' alt='Logo' style='width: 400px; height: auto; margin-bottom: 100px;'>" +
                        "<p>¡Gracias por ponerte en contacto con nosotros!</p>" +
                        "<p>Hemos recibido tu consulta de manera exitosa. Nuestro equipo la revisará y te responderemos a la brevedad. Apreciamos tu interés y estamos aquí para ayudarte.</p>" +
                        "<p>¡Te deseamos un excelente día!</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        // Establecer el cuerpo del mensaje
        helper.setText(cuerpoHtml, true);  // 'true' indica que el contenido es HTML

        // Adjuntar la imagen y asociarla al 'cid' que se usa en el HTML
        FileSystemResource logoImage = new FileSystemResource("src/main/resources/images/logo.png");
        helper.addInline("logoImage", logoImage);


        // Enviar el correo
        mailSender.send(mensaje);
    }


    private boolean enviarCorreoInterno(ContactRequestDto contactRequestDto) throws MessagingException, jakarta.mail.MessagingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);
        helper.setTo("tomiyrami@gmail.com");
        helper.setSubject("Nueva Consulta de Contacto");

        // Usa los métodos get para obtener los valores correctos de cada campo
        String contenidoHtml = String.format(
                "<html><body>" +
                        "<h2>Nueva consulta recibida</h2>" +
                        "<p><strong>Nombre: </strong> "+contactRequestDto.getNombre()+" </p>" +
                        "<p><strong>Apellido:</strong> "+contactRequestDto.getApellido()+" </p>" +
                        "<p><strong>Email:</strong> "+contactRequestDto.getEmail()+" </p>" +
                        "<p><strong>Teléfono:</strong> "+contactRequestDto.getTelefono()+"</p>" +
                        "<p><strong>Mensaje:</strong> "+contactRequestDto.getMensaje()+"</p>" +
                        "</body></html>"

        );
        helper.setText(contenidoHtml, true);

        mailSender.send(mensaje);
        return true;
    }

}

