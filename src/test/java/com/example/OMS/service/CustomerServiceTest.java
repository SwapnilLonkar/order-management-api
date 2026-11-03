package com.example.OMS.service;

import com.example.OMS.dto.CustomerDto;
import com.example.OMS.entity.Customer;
import com.example.OMS.exception.CustomerNotFoundException;
import com.example.OMS.mapper.CustomerMapper;
import com.example.OMS.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createReturnsSavedCustomer() {
        CustomerDto input = CustomerDto.builder().name("Aarav Shah").email("aarav@example.com").phone("555-0100").build();
        Customer entity = Customer.builder().name("Aarav Shah").email("aarav@example.com").phone("555-0100").build();
        Customer saved = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").phone("555-0100").build();
        CustomerDto expected = CustomerDto.builder().id(1L).name("Aarav Shah").email("aarav@example.com").phone("555-0100").build();

        when(customerMapper.toEntity(input)).thenReturn(entity);
        when(customerRepository.save(entity)).thenReturn(saved);
        when(customerMapper.toDto(saved)).thenReturn(expected);

        assertEquals(expected, customerService.create(input));
        verify(customerRepository).save(entity);
    }

    @Test
    void getByIdReturnsCustomerWhenFound() {
        Customer customer = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        CustomerDto expected = CustomerDto.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerMapper.toDto(customer)).thenReturn(expected);

        assertEquals(expected, customerService.getById(1L));
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.getById(99L));
        verify(customerMapper, never()).toDto(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getAllReturnsMappedPage() {
        Customer customer = Customer.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        CustomerDto dto = CustomerDto.builder().id(1L).name("Aarav Shah").email("aarav@example.com").build();
        Pageable pageable = PageRequest.of(0, 10);

        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(customer)));
        when(customerMapper.toDto(customer)).thenReturn(dto);

        Page<CustomerDto> result = customerService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(dto, result.getContent().get(0));
    }

    @Test
    void updateReturnsUpdatedCustomer() {
        Customer existing = Customer.builder().id(1L).name("Old Name").email("old@example.com").phone("555-0000").build();
        CustomerDto input = CustomerDto.builder().name("Aarav Shah").email("aarav@example.com").phone("555-0100").build();
        CustomerDto expected = CustomerDto.builder().id(1L).name("Aarav Shah").email("aarav@example.com").phone("555-0100").build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);
        when(customerMapper.toDto(existing)).thenReturn(expected);

        assertEquals(expected, customerService.update(1L, input));
        assertEquals("Aarav Shah", existing.getName());
        assertEquals("aarav@example.com", existing.getEmail());
        assertEquals("555-0100", existing.getPhone());
    }

    @Test
    void updateThrowsWhenNotFound() {
        CustomerDto input = CustomerDto.builder().name("Aarav Shah").email("aarav@example.com").build();

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.update(99L, input));
        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRemovesExistingCustomer() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.delete(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThrows(CustomerNotFoundException.class, () -> customerService.delete(99L));
        verify(customerRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }
}
