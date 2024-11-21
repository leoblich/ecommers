package com.carpetadigital.ecommerce.entity.dto.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInfoPorIdDto {

    private Long id;

    private String title;
    private String format;
    private String category;
    private String description;
    private Float price;
    private int numeroDePaginas;
    private int countLikes;
    private int countPreView;

    private String urlImagenPublic;
    private String urlImagenPrivate;
}
