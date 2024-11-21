package com.carpetadigital.ecommerce.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsersResponse {
    private String id;
    private String name;
    private String email;
    private String totalFacturas;
    private String totalPagado;
    private String status;
    private String image;
    private String country;


    public UsersResponse(Long id, String email, String firstname, String status, String image, String country, int paymentCount, double totalAmountPaid) {
        this.id = id.toString();
        this.email = email;
        this.name = firstname;
        this.status = status;
        this.image = image;
        this.country = country;
        this.totalFacturas = String.valueOf(paymentCount);
        this.totalPagado = String.valueOf(totalAmountPaid);
    }
}