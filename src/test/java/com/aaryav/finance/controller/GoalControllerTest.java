package com.aaryav.finance.controller;

import com.aaryav.finance.dto.request.GoalRequest;
import com.aaryav.finance.dto.request.GoalUpdateRequest;
import com.aaryav.finance.dto.response.GoalResponse;
import com.aaryav.finance.exception.GlobalExceptionHandler;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.service.GoalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GoalControllerTest {

    private static final String BASE_PATH = "/api";
    private static final LocalDate START_DATE = LocalDate.of(2024, 9, 1);
    private static final LocalDate TARGET_DATE = LocalDate.of(2025, 3, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalService goalService;

    @Test
    @DisplayName("Debe crear una meta válida y responder 201 Created")
    void shouldCreateGoalSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        GoalRequest request = GoalRequest.builder()
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("5000.00"))
                .targetDate(TARGET_DATE)
                .startDate(START_DATE)
                .build();
        GoalResponse response = goalResponse(1L, "Emergency Fund", new BigDecimal("5000.00"), TARGET_DATE);
        when(goalService.createGoal(any(GoalRequest.class))).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/goals")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.targetAmount").value(5000.0))
                .andExpect(jsonPath("$.targetDate").value("2025-03-01"));

        ArgumentCaptor<GoalRequest> captor = ArgumentCaptor.forClass(GoalRequest.class);
        verify(goalService).createGoal(captor.capture());
        assertThat(captor.getValue().getGoalName()).isEqualTo("Emergency Fund");
        assertThat(captor.getValue().getTargetAmount()).isEqualByComparingTo("5000.00");
        assertThat(captor.getValue().getTargetDate()).isEqualTo(TARGET_DATE);
        assertThat(captor.getValue().getStartDate()).isEqualTo(START_DATE);
    }

    @Test
    @DisplayName("Debe rechazar una cantidad negativa en la meta")
    void shouldRejectNegativeGoalTargetAmount() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        GoalRequest request = GoalRequest.builder()
                .goalName("Emergency Fund")
                .targetAmount(new BigDecimal("-100.00"))
                .targetDate(TARGET_DATE)
                .startDate(START_DATE)
                .build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/goals")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Target amount must be positive")));
        verifyNoInteractions(goalService);
    }

    @Test
    @DisplayName("Debe rechazar campos obligatorios ausentes al crear una meta")
    void shouldRejectMissingRequiredGoalFields() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        GoalRequest request = GoalRequest.builder()
                .goalName("")
                .targetAmount(null)
                .targetDate(null)
                .build();

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(post(BASE_PATH + "/goals")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Goal name is required")))
                .andExpect(jsonPath("$.message").value(containsString("Target amount is required")))
                .andExpect(jsonPath("$.message").value(containsString("Target date is required")));
        verifyNoInteractions(goalService);
    }

    @Test
    @DisplayName("Debe obtener todas las metas con 200 OK")
    void shouldGetAllGoals() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        Map<String, List<GoalResponse>> response = Map.of("goals", List.of(
                goalResponse(1L, "Emergency Fund", new BigDecimal("5000.00"), TARGET_DATE),
                goalResponse(2L, "Travel Fund", new BigDecimal("3000.00"), LocalDate.of(2025, 6, 1))));
        when(goalService.getAllGoals()).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/goals")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.goals[0].goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.goals[1].goalName").value("Travel Fund"));
        verify(goalService).getAllGoals();
    }

    @Test
    @DisplayName("Debe obtener una meta por su ID")
    void shouldGetGoalById() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        when(goalService.getGoalById(1L))
                .thenReturn(goalResponse(1L, "Emergency Fund", new BigDecimal("5000.00"), TARGET_DATE));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/goals/1")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.targetAmount").value(5000.0));
        verify(goalService).getGoalById(1L);
    }

    @Test
    @DisplayName("Debe actualizar una meta existente con 200 OK")
    void shouldUpdateGoalSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        LocalDate updatedTargetDate = LocalDate.of(2025, 5, 1);
        GoalUpdateRequest request = GoalUpdateRequest.builder()
                .targetAmount(new BigDecimal("6500.00"))
                .targetDate(updatedTargetDate)
                .build();
        when(goalService.updateGoal(eq(1L), any(GoalUpdateRequest.class)))
                .thenReturn(goalResponse(1L, "Emergency Fund", new BigDecimal("6500.00"), updatedTargetDate));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(put(BASE_PATH + "/goals/1")
                .contextPath(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(6500.0))
                .andExpect(jsonPath("$.targetDate").value("2025-05-01"))
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"));

        ArgumentCaptor<GoalUpdateRequest> captor = ArgumentCaptor.forClass(GoalUpdateRequest.class);
        verify(goalService).updateGoal(eq(1L), captor.capture());
        assertThat(captor.getValue().getTargetAmount()).isEqualByComparingTo("6500.00");
        assertThat(captor.getValue().getTargetDate()).isEqualTo(updatedTargetDate);
    }

    @Test
    @DisplayName("Debe eliminar una meta existente con 200 OK")
    void shouldDeleteGoalSuccessfully() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        when(goalService.deleteGoal(1L)).thenReturn(Map.of("message", "Goal deleted successfully"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(delete(BASE_PATH + "/goals/1")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));
        verify(goalService).deleteGoal(1L);
    }

    @Test
    @DisplayName("Debe manejar un ID inexistente en la meta con 404 Not Found")
    void shouldHandleMissingGoalId() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        doThrow(new ResourceNotFoundException("Goal not found")).when(goalService).getGoalById(99L);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/goals/99")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal not found"));
        verify(goalService).getGoalById(99L);
    }

    private GoalResponse goalResponse(Long id, String name, BigDecimal amount, LocalDate targetDate) {
        return GoalResponse.builder()
                .id(id)
                .goalName(name)
                .targetAmount(amount)
                .targetDate(targetDate)
                .startDate(START_DATE)
                .currentProgress(BigDecimal.ZERO)
                .progressPercentage(0.0)
                .remainingAmount(amount)
                .build();
    }
}
