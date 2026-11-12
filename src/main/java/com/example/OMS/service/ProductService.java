package com.example.OMS.service;

import com.example.OMS.dto.ProductDto;
import com.example.OMS.entity.Product;
import com.example.OMS.entity.Role;
import com.example.OMS.entity.User;
import com.example.OMS.exception.ProductNotFoundException;
import com.example.OMS.mapper.ProductMapper;
import com.example.OMS.repository.ProductRepository;
import com.example.OMS.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CurrentUserService currentUserService;

    public ProductDto create(ProductDto dto) {
        Product product = productMapper.toEntity(dto);
        product.setSeller(currentUserService.requireCurrentUser());
        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toDto(product);
    }

    public Page<ProductDto> getAll(Pageable pageable) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == Role.SELLER) {
            // Sellers manage their own catalog.
            return productRepository.findBySeller(currentUser, pageable).map(productMapper::toDto);
        }
        return productRepository.findAll(pageable).map(productMapper::toDto);
    }

    public ProductDto update(Long id, ProductDto dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == Role.SELLER
                && (existing.getSeller() == null
                        || !existing.getSeller().getId().equals(currentUser.getId()))) {
            throw new IllegalStateException("You can only update your own products");
        }
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setImageUrl(dto.getImageUrl());
        existing.setPrice(dto.getPrice());
        existing.setStockQuantity(dto.getStockQuantity());
        return productMapper.toDto(productRepository.save(existing));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
