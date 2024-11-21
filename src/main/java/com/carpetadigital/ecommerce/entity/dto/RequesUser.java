package com.carpetadigital.ecommerce.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequesUser {
    String username;
    String email;
    String firstname;
    String lastname;
    String country;
    String image;
    Integer rolId;
}
