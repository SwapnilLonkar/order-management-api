package com.example.OMS.dto;

import com.example.OMS.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;

    @NotNull(message = "Customer id is required")
    private Long customerId;

    private OrderStatus status;

    private LocalDateTime orderDate;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemDto> items;
}
