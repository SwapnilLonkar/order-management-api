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
import com.example.OMS.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final CurrentUserService currentUserService;

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
                .placedBy(currentUserService.requireCurrentUser())
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .items(orderItems)
                .build();

        orderItems.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        User currentUser = currentUserService.requireCurrentUser();
        if (!canView(order, currentUser)) {
            throw new OrderNotFoundException(id);
        }
        return toVisibleDto(order, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getAll(Pageable pageable) {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return orderRepository.findAll(pageable).map(orderMapper::toDto);
        }
        if (currentUser.getRole() == Role.CUSTOMER) {
            // Customers only see orders they placed.
            return orderRepository.findByPlacedBy(currentUser, pageable).map(orderMapper::toDto);
        }
        // Sellers see orders that contain their products, trimmed to their own items.
        List<OrderDto> mine = orderRepository.findByItemSeller(currentUser).stream()
                .map(order -> toVisibleDto(order, currentUser))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mine.size());
        List<OrderDto> slice = start >= mine.size() ? List.of() : mine.subList(start, end);
        return new PageImpl<>(slice, pageable, mine.size());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getByCustomerId(Long customerId) {
        User currentUser = currentUserService.requireCurrentUser();
        return orderRepository.findByCustomerId(customerId).stream()
                .filter(order -> canView(order, currentUser))
                .map(order -> toVisibleDto(order, currentUser))
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

        User currentUser = currentUserService.requireCurrentUser();
        if (!canView(order, currentUser)) {
            throw new IllegalStateException(
                    "You can only update the status of orders that involve you");
        }

        order.setStatus(newStatus);
        return toVisibleDto(orderRepository.save(order), currentUser);
    }

    /** ADMIN sees every order; a CUSTOMER sees orders they placed; a SELLER sees
     *  orders containing at least one of their products. */
    private boolean canView(Order order, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (currentUser.getRole() == Role.CUSTOMER) {
            return order.getPlacedBy() != null
                    && order.getPlacedBy().getId().equals(currentUser.getId());
        }
        return order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> isSellerItem(item, currentUser));
    }

    /** Maps an order for the current user; sellers only see their own items
     *  within a shared order. */
    private OrderDto toVisibleDto(Order order, User currentUser) {
        OrderDto dto = orderMapper.toDto(order);
        if (currentUser.getRole() == Role.SELLER) {
            dto.setItems(dto.getItems().stream()
                    .filter(item -> order.getItems().stream()
                            .anyMatch(entityItem -> entityItem.getId() != null
                                    && entityItem.getId().equals(item.getId())
                                    && isSellerItem(entityItem, currentUser)))
                    .toList());
        }
        return dto;
    }

    private boolean isSellerItem(OrderItem item, User seller) {
        return item.getProduct().getSeller() != null
                && item.getProduct().getSeller().getId().equals(seller.getId());
    }
}