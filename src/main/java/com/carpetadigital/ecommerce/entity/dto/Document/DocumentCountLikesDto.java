package com.carpetadigital.ecommerce.entity.dto.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCountLikesDto {

    private Long id;
    private int countLikes;
}
