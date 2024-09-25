package com.dingdong.imageserver.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data
@Table(name = "fairytale_images")
public class FairytaleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fairytale_id", nullable = false)
    private Long fairytaleId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "image_urls", columnDefinition = "json", nullable = false)
    private String imageUrls;

    @Column(name = "prompt")
    private String prompt;

}

