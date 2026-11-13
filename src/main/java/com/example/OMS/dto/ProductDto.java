package com.example.OMS.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductDto {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String imageUrl;

    // The full ordered gallery, cover first. On write, the first entry
    // becomes imageUrl (the cover) and the rest are stored as gallery photos;
    // when omitted, imageUrl alone is used (single-photo products).
    private List<String> imageUrls;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    // Read-only in responses: the seller account that owns the product.
    private Long sellerId;

    private String sellerUsername;
}