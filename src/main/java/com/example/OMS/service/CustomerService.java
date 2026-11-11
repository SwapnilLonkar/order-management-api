package com.example.OMS.service;

import com.example.OMS.dto.CustomerDto;
import com.example.OMS.entity.Customer;
import com.example.OMS.entity.User;
import com.example.OMS.exception.CustomerNotFoundException;
import com.example.OMS.mapper.CustomerMapper;
import com.example.OMS.repository.CustomerRepository;
import com.example.OMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository;

    /**
     * The customer record a CUSTOMER account orders for — a customer can only
     * ever order for themselves. The record is auto-created (named after the
     * account, with a placeholder email) and linked the first time it is
     * needed; staff can correct the details later from the Customers view.
     */
    @Transactional
    public Customer getOrCreateSelfCustomer(User user) {
        if (user.getCustomer() != null) {
            return user.getCustomer();
        }
        Customer self = Customer.builder()
                .name(user.getUsername())
                .email(user.getUsername() + "@oms.local")
                .build();
        Customer saved = customerRepository.save(self);
        user.setCustomer(saved);
        userRepository.save(user);
        return saved;
    }

    public CustomerDto create(CustomerDto dto) {
        Customer saved = customerRepository.save(customerMapper.toEntity(dto));
        return customerMapper.toDto(saved);
    }

    public CustomerDto getById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return customerMapper.toDto(customer);
    }

    public Page<CustomerDto> getAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(customerMapper::toDto);
    }

    public CustomerDto update(Long id, CustomerDto dto) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setPhone(dto.getPhone());
        return customerMapper.toDto(customerRepository.save(existing));
    }

    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteById(id);
    }
}