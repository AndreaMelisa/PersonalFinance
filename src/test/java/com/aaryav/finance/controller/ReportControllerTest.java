package com.aaryav.finance.controller;

import com.aaryav.finance.dto.response.MonthlyReportResponse;
import com.aaryav.finance.dto.response.YearlyReportResponse;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.GlobalExceptionHandler;
import com.aaryav.finance.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReportControllerTest {

    private static final String BASE_PATH = "/api";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @DisplayName("Debe devolver el reporte mensual con 200 OK")
    void shouldGetMonthlyReport() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        MonthlyReportResponse response = MonthlyReportResponse.builder()
                .month(9)
                .year(2024)
                .totalIncome(Map.of("Salary", new BigDecimal("2500.00")))
                .totalExpenses(Map.of("Food", new BigDecimal("1250.00")))
                .netSavings(new BigDecimal("1250.00"))
                .build();
        when(reportService.getMonthlyReport(2024, 9)).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/reports/monthly/2024/9")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(2500.0))
                .andExpect(jsonPath("$.netSavings").value(1250.0));
        verify(reportService).getMonthlyReport(2024, 9);
    }

    @Test
    @DisplayName("Debe devolver el reporte anual con 200 OK")
    void shouldGetYearlyReport() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        YearlyReportResponse response = YearlyReportResponse.builder()
                .year(2024)
                .totalIncome(Map.of("Salary", new BigDecimal("30000.00")))
                .totalExpenses(Map.of("Food", new BigDecimal("16150.00")))
                .netSavings(new BigDecimal("13850.00"))
                .build();
        when(reportService.getYearlyReport(2024)).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/reports/yearly/2024")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalExpenses.Food").value(16150.0))
                .andExpect(jsonPath("$.netSavings").value(13850.0));
        verify(reportService).getYearlyReport(2024);
    }

    @Test
    @DisplayName("Debe convertir correctamente los parámetros year y month a enteros")
    void shouldConvertYearAndMonthParameters() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        MonthlyReportResponse response = MonthlyReportResponse.builder()
                .month(12)
                .year(2023)
                .totalIncome(Map.of())
                .totalExpenses(Map.of())
                .netSavings(BigDecimal.ZERO)
                .build();
        when(reportService.getMonthlyReport(2023, 12)).thenReturn(response);

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/reports/monthly/2023/12")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(12))
                .andExpect(jsonPath("$.year").value(2023));
        verify(reportService).getMonthlyReport(2023, 12);
    }

    @Test
    @DisplayName("Debe manejar un mes inválido con 400 Bad Request")
    void shouldHandleInvalidMonth() throws Exception {
        // Arrange: preparar los datos y configurar el comportamiento del servicio simulado.
        when(reportService.getMonthlyReport(2024, 13))
                .thenThrow(new BadRequestException("Month must be between 1 and 12"));

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(get(BASE_PATH + "/reports/monthly/2024/13")
                .contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Month must be between 1 and 12"));
        verify(reportService).getMonthlyReport(2024, 13);
    }

    @Test
    @DisplayName("Debe rechazar parámetros con formato incorrecto")
    void shouldRejectMalformedRequestParameters() throws Exception {
        // Arrange: preparar un valor de mes que no puede convertirse a entero.
        String invalidMonth = "not-a-month";

        // Act: ejecutar la solicitud HTTP mediante MockMvc.
        ResultActions result = mockMvc.perform(
                get(BASE_PATH + "/reports/monthly/2024/" + invalidMonth).contextPath(BASE_PATH));

        // Assert: verificar el estado HTTP, la respuesta JSON y la interacción con el servicio.
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(reportService);
    }
}
