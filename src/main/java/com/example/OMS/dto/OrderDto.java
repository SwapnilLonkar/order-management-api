package com.example.OMS.dto;

import com.example.OMS.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;

    // Required when an ADMIN places an order on someone's behalf. Ignored for
    // CUSTOMER accounts: they always order for their own linked record.
    private Long customerId;

    // Read-only in responses: the account that placed the order.
    private Long placedById;

    private String placedByUsername;

    private OrderStatus status;

    private LocalDateTime orderDate;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemDto> items;
}
