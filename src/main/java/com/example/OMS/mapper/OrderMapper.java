package com.example.OMS.mapper;

import com.example.OMS.dto.OrderDto;
import com.example.OMS.dto.OrderItemDto;
import com.example.OMS.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .build())
                .collect(Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .placedById(order.getPlacedBy() == null ? null : order.getPlacedBy().getId())
                .placedByUsername(order.getPlacedBy() == null ? null : order.getPlacedBy().getUsername())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .items(itemDtos)
                .build();
    }
}