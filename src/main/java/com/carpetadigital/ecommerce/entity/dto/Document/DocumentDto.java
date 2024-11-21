package com.carpetadigital.ecommerce.entity.dto.Document;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    private Long id;

    private String imagenNameId;
    private String imagenUrlPublic;
    private String imagenUrlprivate;
    private Long imageCreateTime;

    private String fileNameId;
    private String fileUrlPublic;
    private String fileUrlPrivate;
    private String fileDownLoadToken;
    private Long fileCreateTime;

    private boolean borradoLogico;
    private int numeroDePaginas;
    private int countLikes;
    private int countPreView;

    @NotBlank(message = "No puede estar vacío")
    @NotNull(message = "El campo no puede estar vacío")
    @Size(min=3, max = 100, message = "El campo debe tener entre 3 y 100 caracteres")
    private String title;

    @NotBlank(message = "No puede estar vacío")
    @NotNull(message = "El campo no puede estar vacío")
    @Size(min=3, message = "El campo debe tener más de 3 caracteres")
    private String description;

    @NotBlank(message = "No puede estar vacío")
    @NotNull(message = "El campo no puede estar vacío")
    @Size(min=3, max = 10, message = "El campo debe tener entre 3 y 10 caracteres")
    private String format;

    @DecimalMin("0.0")
    @NotNull(message = "El campo no puede estar vacío")
    private Float price;

    @NotBlank(message = "No puede estar vacío")
    @NotNull(message = "El campo no puede estar vacío")
    @Size(min=3, max = 50, message = "El campo debe tener entre 3 y 50 caracteres")
    private String category;


    @NotNull(message = "Error al encontrar el archivo a guardar")
    private MultipartFile file;
}
