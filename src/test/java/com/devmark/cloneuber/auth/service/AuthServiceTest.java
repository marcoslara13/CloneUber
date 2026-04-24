package com.devmark.cloneuber.auth.service;

import com.devmark.cloneuber.auth.dto.AuthResponse;
import com.devmark.cloneuber.auth.dto.LoginRequest;
import com.devmark.cloneuber.auth.dto.RegisterRequest;
import com.devmark.cloneuber.auth.security.JwtService;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import com.devmark.cloneuber.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authManager;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Juan");
        registerRequest.setEmail("juan@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.PASSENGER);

        user = User.builder()
                .id(1L)
                .name("Juan")
                .email("juan@test.com")
                .password("encodedPass")
                .role(Role.PASSENGER)
                .build();
    }

    @Test
    void register_exitoso_retornaTokenYRol() {
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("token123");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getRole()).isEqualTo("PASSENGER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailDuplicado_lanzaExcepcion() {
        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El email ya está registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_guardaPasswordEncodeada() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("tok");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void login_exitoso_retornaTokenYRol() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("juan@test.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token123");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("token123");
        assertThat(response.getRole()).isEqualTo("PASSENGER");
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_usuarioNoExiste_lanzaExcepcion() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("noexiste@test.com");
        loginRequest.setPassword("pass");

        doNothing().when(authManager).authenticate(any());
        when(userRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void login_llamaAuthManagerConCredenciales() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("juan@test.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("tok");

        authService.login(loginRequest);

        verify(authManager).authenticate(argThat(auth ->
                auth instanceof UsernamePasswordAuthenticationToken &&
                auth.getPrincipal().equals("juan@test.com")
        ));
    }
}
