package com.example.OMS.service;

import com.example.OMS.dto.OrderDto;
import com.example.OMS.dto.OrderItemDto;
import com.example.OMS.entity.*;
import com.example.OMS.exception.CustomerNotFoundException;
import com.example.OMS.exception.InsufficientStockException;
import com.example.OMS.exception.OrderNotFoundException;
import com.example.OMS.exception.ProductNotFoundException;
import com.example.OMS.mapper.OrderMapper;
import com.example.OMS.repository.CustomerRepository;
import com.example.OMS.repository.OrderRepository;
import com.example.OMS.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderDto create(OrderDto dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(dto.getCustomerId()));

        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDto itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException(itemDto.getProductId()));

            if (product.getStockQuantity() < itemDto.getQuantity()) {
                throw new InsufficientStockException(
                        product.getName(), product.getStockQuantity(), itemDto.getQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .items(orderItems)
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    public OrderDto getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toDto(order);
    }

    public Page<OrderDto> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toDto);
    }

    public List<OrderDto> getByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Transactional
    public OrderDto updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Cannot change status of an order that is already " + order.getStatus());
        }

        order.setStatus(newStatus);
        return orderMapper.toDto(orderRepository.save(order));
    }
}