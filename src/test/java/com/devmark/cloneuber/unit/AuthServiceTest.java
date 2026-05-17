package com.devmark.cloneuber.unit;

import com.devmark.cloneuber.auth.dto.AuthResponse;
import com.devmark.cloneuber.auth.dto.LoginRequest;
import com.devmark.cloneuber.auth.dto.RegisterRequest;
import com.devmark.cloneuber.auth.security.JwtService;
import com.devmark.cloneuber.auth.service.AuthService;
import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import com.devmark.cloneuber.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests unitarios")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("Marcos").email("marcos@test.com")
                .password("pass123").role(Role.PASSENGER).build();

        loginRequest = LoginRequest.builder()
                .email("marcos@test.com").password("pass123").build();

        existingUser = User.builder()
                .id(1L).name("Marcos").email("marcos@test.com")
                .password("hashed_pass").role(Role.PASSENGER).build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Email nuevo → usuario guardado y token devuelto")
        void register_newEmail_savesUserAndReturnsToken() {
            when(userRepository.findByEmail("marcos@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("pass123")).thenReturn("hashed_pass");
            when(userRepository.save(any(User.class))).thenReturn(existingUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.aqui");

            AuthResponse response = authService.register(registerRequest);

            assertThat(response.getToken()).isEqualTo("jwt.token.aqui");
            assertThat(response.getRole()).isEqualTo("PASSENGER");

            verify(passwordEncoder).encode("pass123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Email ya registrado → lanza RuntimeException sin guardar")
        void register_duplicateEmail_throwsException() {
            when(userRepository.findByEmail("marcos@test.com")).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya está registrado");

            verify(userRepository, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("La contraseña se guarda hasheada, nunca en texto plano")
        void register_passwordIsHashed() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("token");

            authService.register(registerRequest);

            verify(userRepository).save(argThat(u -> u.getPassword().equals("$2a$hashed")));
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Credenciales correctas → devuelve token")
        void login_validCredentials_returnsToken() {
            when(userRepository.findByEmail("marcos@test.com")).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(existingUser)).thenReturn("jwt.token.valido");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("jwt.token.valido");
            assertThat(response.getRole()).isEqualTo("PASSENGER");
            verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Credenciales incorrectas → AuthenticationManager lanza excepción")
        void login_wrongCredentials_throwsException() {
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByEmail(any());
            verify(jwtService, never()).generateToken(any());
        }
    }
}
