package com.example.OMS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    // Optional storefront image, shown on the product card. TEXT (not the
    // default varchar) so a seller-uploaded image stored as a data URL fits.
    // This is the gallery's cover image (the first photo).
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    // Additional gallery photos, in display order, shown after the cover.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    @OrderColumn(name = "position")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stockQuantity;

    // The seller account that owns this product; null for products created before ownership existed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;
}
