package com.devmark.cloneuber.auth.security;

import com.devmark.cloneuber.user.entity.Role;
import com.devmark.cloneuber.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Must be at least 256 bits for HMAC-SHA
        ReflectionTestUtils.setField(jwtService, "secret", "clave-super-secreta-para-testing-1234567890abcdef");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);

        user = User.builder()
                .id(1L)
                .name("Juan")
                .email("juan@test.com")
                .password("pass")
                .role(Role.PASSENGER)
                .build();
    }

    @Test
    void generateToken_retornaTokenNoNulo() {
        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_tokenTieneFormatoJwt() {
        String token = jwtService.generateToken(user);
        // JWT tiene 3 partes separadas por puntos
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractEmail_retornaEmailCorrecto() {
        String token = jwtService.generateToken(user);
        String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo("juan@test.com");
    }

    @Test
    void isTokenValid_tokenValido_retornaTrue() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_tokenExpirado_retornaFalse() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    void isTokenValid_usernameDiferente_retornaFalse() {
        String token = jwtService.generateToken(user);
        User otroUser = User.builder()
                .id(2L)
                .name("Otro")
                .email("otro@test.com")
                .password("pass")
                .role(Role.DRIVER)
                .build();
        assertThat(jwtService.isTokenValid(token, otroUser)).isFalse();
    }

    @Test
    void generateToken_diferentesUsuarios_generanTokensDiferentes() {
        User user2 = User.builder()
                .id(2L)
                .name("Otro")
                .email("otro@test.com")
                .password("pass")
                .role(Role.DRIVER)
                .build();

        String token1 = jwtService.generateToken(user);
        String token2 = jwtService.generateToken(user2);

        assertThat(token1).isNotEqualTo(token2);
    }
}
