package com.example.OMS.mapper;

import com.example.OMS.dto.ProductDto;
import com.example.OMS.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {

    public ProductDto toDto(Product product) {
        List<String> gallery = galleryOf(product);
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(gallery.isEmpty() ? product.getImageUrl() : gallery.get(0))
                .imageUrls(gallery)
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sellerId(product.getSeller() == null ? null : product.getSeller().getId())
                .sellerUsername(product.getSeller() == null ? null : product.getSeller().getUsername())
                .build();
    }

    public Product toEntity(ProductDto dto) {
        List<String> gallery = cleanedGallery(dto);
        return Product.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .imageUrl(gallery.isEmpty() ? dto.getImageUrl() : gallery.get(0))
                .images(gallery.size() <= 1
                        ? new ArrayList<>()
                        : new ArrayList<>(gallery.subList(1, gallery.size())))
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .build();
    }

    // The product's full gallery in display order: cover (imageUrl) first,
    // then the additional photos, with blanks and duplicates removed.
    private List<String> galleryOf(Product product) {
        List<String> gallery = new ArrayList<>();
        addIfUsable(gallery, product.getImageUrl());
        if (product.getImages() != null) {
            for (String url : product.getImages()) {
                addIfUsable(gallery, url);
            }
        }
        return gallery;
    }

    // The gallery a client asked to store: imageUrls when provided (cover
    // first), otherwise the single imageUrl. Blanks/duplicates removed.
    private List<String> cleanedGallery(ProductDto dto) {
        List<String> gallery = new ArrayList<>();
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            for (String url : dto.getImageUrls()) {
                addIfUsable(gallery, url);
            }
        } else {
            addIfUsable(gallery, dto.getImageUrl());
        }
        return gallery;
    }

    private void addIfUsable(List<String> gallery, String url) {
        if (url != null && !url.isBlank() && !gallery.contains(url)) {
            gallery.add(url);
        }
    }
}
