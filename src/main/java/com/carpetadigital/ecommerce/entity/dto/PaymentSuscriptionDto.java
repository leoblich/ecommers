package com.carpetadigital.ecommerce.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSuscriptionDto {

    private Long userId; // ID del usuario
    private Double amount; // Monto pagado

    @JsonProperty("isSubscription")
    private boolean isSubscription; // Indica si es una suscripción

    private String status; // Estado del pago
    private String subscriptionType; // Tipo de suscripción

    private List<Long> documentIds; // IDs de los documentos comprados

    private String guestEmail;


}
