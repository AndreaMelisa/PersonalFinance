package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.LoginRequest;
import com.aaryav.finance.dto.request.RegisterRequest;
import com.aaryav.finance.entity.User;
import com.aaryav.finance.exception.DuplicateResourceException;
import com.aaryav.finance.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("test@example.com")
                .password("password123")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .username("test@example.com")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_Success() {
        when(userRepository.existsByUsername("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        Map<String, Object> result = authService.register(registerRequest);

        assertThat(result.get("message")).isEqualTo("User registered successfully");
        assertThat(result.get("userId")).isEqualTo(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException for existing username")
    void register_DuplicateUsername() {
        when(userRepository.existsByUsername("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists");
    }

    @Test
    @DisplayName("Should login successfully and create session")
    void login_Success() {
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(true)).thenReturn(session);

        Map<String, String> result = authService.login(loginRequest, httpRequest);

        assertThat(result.get("message")).isEqualTo("Login successful");
        verify(session).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException for invalid login")
    void login_InvalidCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        assertThatThrownBy(() -> authService.login(loginRequest, httpRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should logout and invalidate session")
    void logout_Success() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(httpRequest.getSession(false)).thenReturn(session);

        Map<String, String> result = authService.logout(httpRequest);

        assertThat(result.get("message")).isEqualTo("Logout successful");
        verify(session).invalidate();
    }

    @Test
    @DisplayName("Should logout gracefully when no session exists")
    void logout_NoSession() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getSession(false)).thenReturn(null);

        Map<String, String> result = authService.logout(httpRequest);

        assertThat(result.get("message")).isEqualTo("Logout successful");
    }
}
