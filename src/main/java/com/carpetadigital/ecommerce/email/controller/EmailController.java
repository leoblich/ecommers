package com.carpetadigital.ecommerce.email.controller;

import com.carpetadigital.ecommerce.email.service.EmailService;
import com.carpetadigital.ecommerce.entity.dto.EmailDto;
import com.carpetadigital.ecommerce.utils.handler.ResponseHandler;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/email")
public class EmailController {

    private final EmailService emailService;

    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    @GetMapping
    public String getTestion() {
       return "hola";
}

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestParam String toEmail,
                                            @RequestParam String subject,
                                            @RequestParam String body) {
        try {
            emailService.sendProductEmail(toEmail, subject, body);
            return ResponseEntity.ok("Email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending email: " + e.getMessage());
        }
    }

    @PostMapping("/send-payment")
    public ResponseEntity<Object> enviarPago(@RequestBody EmailDto emailDto) throws MessagingException {

        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                emailService.sendEmailBasedOnType(emailDto),
                true
        );
    }


}
