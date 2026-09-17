package com.aaryav.finance.controller;

import com.aaryav.finance.dto.request.LoginRequest;
import com.aaryav.finance.dto.request.RegisterRequest;
import com.aaryav.finance.exception.DuplicateResourceException;
import com.aaryav.finance.exception.GlobalExceptionHandler;
import com.aaryav.finance.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    private static final String BASE_PATH = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("Debe registrar un usuario válido y responder 201 Created")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        RegisterRequest request = RegisterRequest.builder()
                .username("test@example.com")
                .password("Password123!")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Map.of("message", "User registered successfully", "userId", 1L));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/register")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").value(1));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("test@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("Password123!");
        assertThat(captor.getValue().getFullName()).isEqualTo("Test User");
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("+1234567890");
    }

    @Test
    @DisplayName("Debe rechazar un correo inválido con 400 Bad Request")
    void shouldRejectInvalidEmailOnRegister() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        RegisterRequest request = RegisterRequest.builder()
                .username("invalid-email")
                .password("Password123!")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/register")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("Username must be a valid email address")));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Debe rechazar campos obligatorios vacíos durante el registro")
    void shouldRejectBlankFieldsOnRegister() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .password("")
                .fullName("")
                .phoneNumber("")
                .build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/register")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username is required")))
                .andExpect(jsonPath("$.message").value(containsString("Password is required")))
                .andExpect(jsonPath("$.message").value(containsString("Full name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Phone number is required")));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Debe responder 409 Conflict cuando el usuario ya existe")
    void shouldHandleDuplicateUserConflict() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        RegisterRequest request = RegisterRequest.builder()
                .username("existing@example.com")
                .password("Password123!")
                .fullName("Existing User")
                .phoneNumber("+1234567890")
                .build();
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/register")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("existing@example.com");
    }

    @Test
    @DisplayName("Debe autenticar un usuario válido y responder 200 OK")
    void shouldLoginUserSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        LoginRequest request = LoginRequest.builder()
                .username("test@example.com")
                .password("Password123!")
                .build();
        when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenReturn(Map.of("message", "Login successful"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/login")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture(), any(HttpServletRequest.class));
        assertThat(captor.getValue().getUsername()).isEqualTo("test@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("Password123!");
    }

    @Test
    @DisplayName("Debe rechazar credenciales vacías durante el login")
    void shouldRejectBlankCredentialsOnLogin() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        LoginRequest request = LoginRequest.builder().username("").password("").build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/login")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username is required")))
                .andExpect(jsonPath("$.message").value(containsString("Password is required")));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Debe responder 401 Unauthorized cuando las credenciales son incorrectas")
    void shouldRejectInvalidLoginCredentials() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        LoginRequest request = LoginRequest.builder()
                .username("unknown@example.com")
                .password("WrongPassword")
                .build();
        when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/login")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(captor.capture(), any(HttpServletRequest.class));
        assertThat(captor.getValue().getUsername()).isEqualTo("unknown@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("WrongPassword");
    }

    @Test
    @DisplayName("Debe cerrar sesión correctamente con 200 OK")
    void shouldLogoutSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        when(authService.logout(any(HttpServletRequest.class)))
                .thenReturn(Map.of("message", "Logout successful"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/auth/logout")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
        verify(authService).logout(any(HttpServletRequest.class));
    }
}
