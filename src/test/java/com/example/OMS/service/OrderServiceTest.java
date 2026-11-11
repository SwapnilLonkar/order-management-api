package com.example.OMS.service;

import com.example.OMS.dto.OrderDto;
import com.example.OMS.dto.OrderItemDto;
import com.example.OMS.entity.Customer;
import com.example.OMS.entity.Order;
import com.example.OMS.entity.OrderStatus;
import com.example.OMS.entity.Product;
import com.example.OMS.entity.Role;
import com.example.OMS.entity.User;
import com.example.OMS.exception.CustomerNotFoundException;
import com.example.OMS.exception.InsufficientStockException;
import com.example.OMS.exception.OrderNotFoundException;
import com.example.OMS.exception.ProductNotFoundException;
import com.example.OMS.mapper.OrderMapper;
import com.example.OMS.repository.CustomerRepository;
import com.example.OMS.repository.OrderRepository;
import com.example.OMS.repository.ProductRepository;
import com.example.OMS.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private OrderService orderService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        // Order visibility is role-scoped; an ADMIN current user can see and
        // change every order, which keeps these tests focused on the original
        // create/status rules. Lenient: some paths throw before the user is read.
        adminUser = User.builder().id(99L).username("admin").role(Role.ADMIN).build();
        lenient().when(currentUserService.requireCurrentUser()).thenReturn(adminUser);
    }

    @Test
    void createDeductsStockAndReturnsSavedOrder() {
        Customer customer = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        Product product = Product.builder().id(2L).name("Widget").price(19.99).stockQuantity(10).build();
        OrderDto input = OrderDto.builder()
                .customerId(1L)
                .items(List.of(OrderItemDto.builder().productId(2L).quantity(2).build()))
                .build();
        Order saved = Order.builder().id(1L).customer(customer).status(OrderStatus.PENDING).build();
        OrderDto expected = OrderDto.builder().id(1L).customerId(1L).status(OrderStatus.PENDING).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.toDto(saved)).thenReturn(expected);

        assertEquals(expected, orderService.create(input));
        assertEquals(8, product.getStockQuantity());
        verify(productRepository).save(product);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order toSave = orderCaptor.getValue();
        assertEquals(OrderStatus.PENDING, toSave.getStatus());
        assertEquals(customer, toSave.getCustomer());
        assertEquals(adminUser, toSave.getPlacedBy());
        assertEquals(1, toSave.getItems().size());
        assertEquals(2, toSave.getItems().get(0).getQuantity());
        assertEquals(19.99, toSave.getItems().get(0).getPriceAtPurchase());
        assertEquals(toSave, toSave.getItems().get(0).getOrder());
    }

    @Test
    void createThrowsWhenCustomerNotFound() {
        OrderDto input = OrderDto.builder()
                .customerId(99L)
                .items(List.of(OrderItemDto.builder().productId(2L).quantity(1).build()))
                .build();

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> orderService.create(input));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createThrowsWhenProductNotFound() {
        Customer customer = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        OrderDto input = OrderDto.builder()
                .customerId(1L)
                .items(List.of(OrderItemDto.builder().productId(99L).quantity(1).build()))
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> orderService.create(input));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createThrowsWhenStockInsufficient() {
        Customer customer = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        Product product = Product.builder().id(2L).name("Widget").price(19.99).stockQuantity(1).build();
        OrderDto input = OrderDto.builder()
                .customerId(1L)
                .items(List.of(OrderItemDto.builder().productId(2L).quantity(2).build()))
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> orderService.create(input));
        assertEquals(1, product.getStockQuantity());
        verify(productRepository, never()).save(product);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getByIdReturnsOrderWhenFound() {
        Customer customer = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        Order order = Order.builder().id(1L).customer(customer).status(OrderStatus.PENDING).build();
        OrderDto expected = OrderDto.builder().id(1L).customerId(1L).status(OrderStatus.PENDING).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(expected);

        assertEquals(expected, orderService.getById(1L));
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getById(99L));
        verify(orderMapper, never()).toDto(any(Order.class));
    }

    @Test
    void updateStatusChangesStatusWhenNotTerminal() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).build();
        OrderDto expected = OrderDto.builder().id(1L).status(OrderStatus.SHIPPED).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(expected);

        assertEquals(expected, orderService.updateStatus(1L, OrderStatus.SHIPPED));
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void updateStatusThrowsWhenCancelled() {
        Order order = Order.builder().id(1L).status(OrderStatus.CANCELLED).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.updateStatus(1L, OrderStatus.SHIPPED));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateStatusThrowsWhenDelivered() {
        Order order = Order.builder().id(1L).status(OrderStatus.DELIVERED).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.updateStatus(1L, OrderStatus.CANCELLED));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateStatusThrowsWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateStatus(99L, OrderStatus.SHIPPED));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
