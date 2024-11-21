package com.carpetadigital.ecommerce.entity.dto.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLosMasDto {
    private Long id;

    private String title;
    private String format;
    private String category;
    private String description;
    private Float price;
    private int numeroDePaginas;
    private boolean borradoLogico;
    private int countLikes;
    private int countPreview;

    private String urlImagenPublic;
    private String urlImagenPrivate;
    private String FilecreateTime;
}
