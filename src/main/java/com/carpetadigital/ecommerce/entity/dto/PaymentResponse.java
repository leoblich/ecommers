package com.carpetadigital.ecommerce.entity.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private String userId;
    private String email;
    private String firstName;  // Cambié 'firstname' por 'firstName'
    private String amount;
    private LocalDateTime paymentDate;
    private String state;
}
