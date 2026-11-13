package com.example.OMS.service;

import com.example.OMS.dto.LoginRequest;
import com.example.OMS.dto.RegisterRequest;
import com.example.OMS.entity.Customer;
import com.example.OMS.entity.Role;
import com.example.OMS.entity.User;
import com.example.OMS.repository.UserRepository;
import com.example.OMS.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesPasswordDefaultsToCustomerAndReturnsToken() {
        RegisterRequest request = RegisterRequest.builder().username("alice").password("secret").build();
        Customer self = Customer.builder().id(1L).name("alice").email("alice@oms.local").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerService.getOrCreateSelfCustomer(any(User.class))).thenReturn(self);
        when(jwtService.generateToken("alice", Role.CUSTOMER)).thenReturn("jwt-token");

        assertEquals("jwt-token", authService.register(request).getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("alice", userCaptor.getValue().getUsername());
        assertEquals("hashed-password", userCaptor.getValue().getPassword());
        assertEquals(Role.CUSTOMER, userCaptor.getValue().getRole());
        verify(customerService).getOrCreateSelfCustomer(any(User.class));
    }

    @Test
    void registerWithSellerRoleKeepsRoleAndSkipsSelfCustomer() {
        RegisterRequest request = RegisterRequest.builder()
                .username("seller1").password("secret").role(Role.SELLER).build();

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("seller1", Role.SELLER)).thenReturn("jwt-token");

        assertEquals("jwt-token", authService.register(request).getToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.SELLER, userCaptor.getValue().getRole());
        verify(customerService, never()).getOrCreateSelfCustomer(any(User.class));
    }

    @Test
    void registerThrowsWhenUsernameTaken() {
        RegisterRequest request = RegisterRequest.builder().username("alice").password("secret").build();
        User existing = User.builder().id(1L).username("alice").role(Role.CUSTOMER).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerThrowsWhenAdminRequested() {
        RegisterRequest request = RegisterRequest.builder()
                .username("root").password("secret").role(Role.ADMIN).build();

        when(userRepository.findByUsername("root")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder().username("alice").password("secret").build();
        User user = User.builder().id(1L).username("alice").password("hashed-password").role(Role.CUSTOMER).build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice", Role.CUSTOMER)).thenReturn("jwt-token");

        assertEquals("jwt-token", authService.login(request).getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void loginThrowsWhenCredentialsAreBad() {
        LoginRequest request = LoginRequest.builder().username("alice").password("wrong").build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any(), any());
        verify(userRepository, never()).findByUsername(any());
    }
}
