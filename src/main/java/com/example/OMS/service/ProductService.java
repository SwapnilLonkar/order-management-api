package com.example.OMS.service;

import com.example.OMS.dto.ProductDto;
import com.example.OMS.entity.Product;
import com.example.OMS.exception.ProductNotFoundException;
import com.example.OMS.mapper.ProductMapper;
import com.example.OMS.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductDto create(ProductDto dto) {
        Product saved = productRepository.save(productMapper.toEntity(dto));
        return productMapper.toDto(saved);
    }

    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toDto(product);
    }

    public Page<ProductDto> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toDto);
    }

    public ProductDto update(Long id, ProductDto dto) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
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
