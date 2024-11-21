package com.carpetadigital.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "documents")
public class DocumentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 10)
    private String format;

    @Column(nullable = false)
    private Float price;

    @Column(name = "numero_de_paginas")
    private int numeroDePaginas = 0;

    @Column(name = "file_name_Id", nullable = false, length = 250)
    private String fileNameId;
    @Column(name = "file_url_public", nullable = false, length = 500)
    private String fileUrlPublic;
    @Column(name = "file_url_private", nullable = false, length = 500)
    private String fileUrlPrivate;
    @Column(name = "file_down_load_token", nullable = false, length = 150)
    private String fileDownLoadToken;
    @Column(name = "file_create_time", nullable = false, length = 150)
    private Long fileCreateTime = 0L;

    @Column(length = 50)
    private String category;

    @Column(name = "borrado_logico")
    private Boolean borradoLogico = false;

    @Column(name = "created_at")
    private LocalDate createdAt = LocalDate.now();

    @Column(name = "image_name_id", nullable = false, length = 250)
    private String imagenNameId;
    @Column(name = "image_url_public", nullable = false, length = 500)
    private String imagenUrlPublic;
    @Column(name = "image_url_private", nullable = false, length = 500)
    private String imagenUrlprivate;
    @Column(name = "image_down_load_token", nullable = false, length = 150)
    private String imageDownLoadToken;
    @Column(name = "image_create_time", nullable = false, length = 150)
    private Long imageCreateTime = 0L;

    @Column(name = "count_likes")
    private int countLikes = 0;

    @Column(name = "count_pre_view")
    private Integer countPreView = 0;

    @ManyToMany(mappedBy = "documents")
    @JsonBackReference
    private List<Payment> payments;
}
