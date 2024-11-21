package com.carpetadigital.ecommerce.email.service;

import com.carpetadigital.ecommerce.Auth.User.User;
import com.carpetadigital.ecommerce.Auth.User.UserRepository;
import com.carpetadigital.ecommerce.entity.dto.EmailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    private final UserRepository repository;

    // Usar la imagen desde el directorio resources/images
    private final ClassPathResource logo = new ClassPathResource("images/logo.png");

    public EmailService(UserRepository repository) {
        this.repository = repository;
    }


    // Método para formatear la fecha
    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    // Método para enviar correo basado en el tipo
    public boolean sendEmailBasedOnType(EmailDto emailDto) throws MessagingException {
        String htmlContent;
        String subject;
        User user = null;
        LocalDateTime fechaActual = LocalDateTime.now();

        if(emailDto.getUserId() != null) {
             user = repository.findById(emailDto.getUserId()).orElse(null);
        }



        if (emailDto.getTypeTemplate() == 1) {
            subject = "SUBSCRIPCIÓN ADQUIRIDA"; // o "Mensual", según sea necesario
            emailDto.setGuestEmail(user.getEmail());
            htmlContent = generateSubscriptionTemplate(fechaActual, emailDto.getAmount(), emailDto.getVoucherNumber(), user.getFirstname(), emailDto.getSubscriptionType());
        } else if (emailDto.getTypeTemplate() == 2) {
            subject = "COMPRA EXITOSA";
            emailDto.setGuestEmail(user.getEmail());
            htmlContent = generateLoggedInUserTemplate(fechaActual, emailDto.getAmount(), emailDto.getVoucherNumber(), user.getFirstname(), emailDto.getDownloadUrl());
        } else if (emailDto.getTypeTemplate() == 3) {
            subject = "COMPRA EXITOSA";
            emailDto.setGuestEmail(emailDto.getGuestEmail());
            htmlContent = generateGuestUserTemplate(fechaActual, emailDto.getAmount(), emailDto.getVoucherNumber(), emailDto.getGuestEmail(), emailDto.getDownloadUrl());
        } else if (emailDto.getTypeTemplate() == 4) {
            emailDto.setGuestEmail(user.getEmail());
            subject = "REGISTRO EXITOSO";
            htmlContent = generateWelcomeEmailTemplate(fechaActual, user.getFirstname());
        } else {
            throw new IllegalArgumentException("tipo de plantilla no válido");
        }

        if (subject != null && htmlContent != null) {
            sendProductEmail(emailDto.getGuestEmail(), subject, htmlContent);
            return true;
        }else {
             new IllegalArgumentException("Error: subject y HTML vacíos");
             return false;
        }
    }

    // Método para enviar un correo
    public void sendProductEmail(String toEmail, String subject, String body) throws MessagingException {
        log.info("Iniciando el envío de correo a: " + toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            // Adjuntar la imagen del logo
            helper.addInline("logoImage", logo);

            log.info("Preparando para enviar el correo...");
                mailSender.send(message);
            log.info("Envío exitoso a: " + toEmail);
        } catch (MessagingException e) {
            log.error("Error al enviar el correo a: " + toEmail, e);
            throw e;  // Para propagar la excepción
        }
    }

    // Método para generar plantilla de suscripción
    private String generateSubscriptionTemplate(LocalDateTime fechaActual, Double amountPaid, Integer operationNumber, String userName, String subscriptionType) {
        return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Voucher de Suscripción</title></head>" +
                "<body  style=color: black; font-size: 40px;> "+
                "<div style='width: 100%; text-align: center;'>" +
                "<img src='cid:logoImage' alt='Logo' style='width: 400px; height: auto; margin-bottom: 100px;'>" +
                "<h1 style='margin-top: 10px; font-weight: bold; font-style: italic;'>Voucher de Suscripción</h1>" +
                "<h2>¡Gracias por tu compra, " + userName + "!</h2>" +
                "<h2>Tipo de Suscripción: " + subscriptionType + "</h2>" +
                "<h2>Fecha de Pago: " + formatDateTime(fechaActual) + "</h2>" +
                "<h2>Monto Pagado: $" + amountPaid + "</h2>" +
                "<h2>Código de transacción: " + operationNumber + "</h2>" +
                "<h2>¡Gracias por unirte a nosotros!</h2>" +
                "<p>Te saluda la familia Carpeta Digital</p>" +
                "<p>Recuerda que el pago también lo puedes realizar mediante depósito en nuestra cuenta corriente.</p>" +
                "</div></body></html>";
    }

    // Método para generar plantilla de usuario logueado
    private String generateLoggedInUserTemplate(LocalDateTime fechaActual, Double amountPaid, Integer operationNumber, String userName, String downloadUrl) {
        return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Voucher de Pago</title></head>" +
                "<body  style=color: black; font-size: 40px;> "+
                "<div style='width: 100%; text-align: center;'>" +
                "<img src='cid:logoImage' alt='Logo' style='width: 400px; height: auto; margin-bottom: 100px;'>"+
                "<h1 style='margin-top: 10px; font-weight: bold; font-style: italic;'>Voucher de pago</h1>" +
                "<h2>¡Gracias por tu compra, " + userName + "!</h2>" +
                "<h2>Fecha de Pago: " + formatDateTime(fechaActual) + "</h2>" +
                "<h2>Monto Pagado: $" + amountPaid + "</h2>" +
                "<h2>Código de transacción: " + operationNumber + "</h2>" +
                "<p><a href='" + downloadUrl + "'>Descargar producto</a></p>" +
                "<p>Te saluda la familia Carpeta Digital</p>" +
                "</div></body></html>";
    }

    // Método para generar plantilla de usuario invitado
    private String generateGuestUserTemplate(LocalDateTime fechaActual, Double amountPaid, Integer voucherNumber, String email, String downloadUrl) {
        return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Voucher de Pago</title></head>" +
                "<body  style=color: black; font-size: 40px;> "+
                "<div style='width: 100%; text-align: center;'>" +
                "<img src='cid:logoImage' alt='Logo' style='width: 400px; height: auto; margin-bottom: 100px;'>"+
                "<h1 style='margin-top: 10px; font-weight: bold; font-style: italic;'>Voucher de pago</h1>" +
                "<h2>Correo del Usuario: " + email + "</h2>" +
                "<h2>Fecha de Pago: " + formatDateTime(fechaActual) + "</h2>" +
                "<h2>Monto Pagado: $" + amountPaid + "</h2>" +
                "<h2>Código de transacción: " + voucherNumber + "</h2>" +
                "<p><a href='" + downloadUrl + "'>Descargar producto</a></p>" +
                "<p>Te saluda la familia Carpeta Digital</p>" +
                "</div></body></html>";
    }

    // Método para generar plantilla de bienvenida
    private String generateWelcomeEmailTemplate(LocalDateTime fechaActual, String userName) {
        return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>Bienvenido a Carpeta Digital</title></head>" +
                "<body  style=color: black; font-size: 40px;> "+
                "<div style='width: 100%; text-align: center;'>" +
                "<img src='cid:logoImage' alt='Logo' style='width: 400px; height: auto; margin-bottom: 100px;'>"+
                "<h1>¡Bienvenido a Carpeta Digital!</h1>" +
                "<h2>¡Gracias por unirte a nuestra familia, " + userName + "!</h2>" +
                "<h3>Fecha de registro: " + formatDateTime(fechaActual) + "</h3>" +
                "<p>Te saluda la familia Carpeta Digital</p>" +
                "</div></body></html>";
    }
}
