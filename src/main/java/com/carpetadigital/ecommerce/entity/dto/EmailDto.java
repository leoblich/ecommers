package com.carpetadigital.ecommerce.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class EmailDto {


    private Long userId; // ID del usuario
    private Double amount; // Monto pagado
    private String subscriptionType; // Tipo de suscripción
    private List<Long> documentIds; // IDs de los documentos comprados
    private String guestEmail; // Correo del invitado
    private int typeTemplate;
    private String userName;
    private String downloadUrl;

    // Nuevos campos para manejar el envío de correos electrónicos
    private Integer voucherNumber; // Número de voucher




}