package com.carpetadigital.ecommerce.Contact;




import com.carpetadigital.ecommerce.entity.dto.ContactRequestDto;
import com.carpetadigital.ecommerce.utils.handler.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;


@RestController
@RequestMapping("/api/v1/contactanos")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<Object> recibirConsulta(@RequestBody ContactRequestDto contactRequestDto) throws MessagingException, jakarta.mail.MessagingException  {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                contactService.enviarConsulta(contactRequestDto),
                true);
    }


}
