package com.aaryav.finance.service;

import com.aaryav.finance.dto.response.MonthlyReportResponse;
import com.aaryav.finance.dto.response.YearlyReportResponse;
import com.aaryav.finance.entity.*;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ReportService reportService;

    private User testUser;
    private Category salaryCategory;
    private Category foodCategory;
    private Category rentCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("test@example.com").build();
        salaryCategory = Category.builder().name("Salary").type(CategoryType.INCOME).build();
        foodCategory = Category.builder().name("Food").type(CategoryType.EXPENSE).build();
        rentCategory = Category.builder().name("Rent").type(CategoryType.EXPENSE).build();
        mockSecurityContext("test@example.com");
        lenient().when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Should generate monthly report with income and expenses")
    void getMonthlyReport_WithData() {
        Transaction income = Transaction.builder().amount(new BigDecimal("50000"))
                .date(LocalDate.of(2024, 1, 15)).category(salaryCategory).build();
        Transaction expense1 = Transaction.builder().amount(new BigDecimal("5000"))
                .date(LocalDate.of(2024, 1, 20)).category(foodCategory).build();
        Transaction expense2 = Transaction.builder().amount(new BigDecimal("15000"))
                .date(LocalDate.of(2024, 1, 5)).category(rentCategory).build();

        when(transactionRepository.findByUserIdAndYearAndMonth(1L, 2024, 1))
                .thenReturn(List.of(income, expense1, expense2));

        MonthlyReportResponse result = reportService.getMonthlyReport(2024, 1);

        assertThat(result.getMonth()).isEqualTo(1);
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getTotalIncome()).containsEntry("Salary", new BigDecimal("50000"));
        assertThat(result.getTotalExpenses()).containsEntry("Food", new BigDecimal("5000"));
        assertThat(result.getTotalExpenses()).containsEntry("Rent", new BigDecimal("15000"));
        assertThat(result.getNetSavings()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("Should return empty report when no transactions")
    void getMonthlyReport_Empty() {
        when(transactionRepository.findByUserIdAndYearAndMonth(1L, 2024, 2)).thenReturn(List.of());

        MonthlyReportResponse result = reportService.getMonthlyReport(2024, 2);

        assertThat(result.getTotalIncome()).isEmpty();
        assertThat(result.getTotalExpenses()).isEmpty();
        assertThat(result.getNetSavings()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Should aggregate multiple transactions per category")
    void getMonthlyReport_AggregatesSameCategory() {
        Transaction food1 = Transaction.builder().amount(new BigDecimal("200"))
                .date(LocalDate.of(2024, 1, 1)).category(foodCategory).build();
        Transaction food2 = Transaction.builder().amount(new BigDecimal("300"))
                .date(LocalDate.of(2024, 1, 15)).category(foodCategory).build();

        when(transactionRepository.findByUserIdAndYearAndMonth(1L, 2024, 1))
                .thenReturn(List.of(food1, food2));

        MonthlyReportResponse result = reportService.getMonthlyReport(2024, 1);

        assertThat(result.getTotalExpenses()).containsEntry("Food", new BigDecimal("500"));
    }

    @Test
    @DisplayName("Should generate yearly report")
    void getYearlyReport_WithData() {
        Transaction t1 = Transaction.builder().amount(new BigDecimal("50000"))
                .date(LocalDate.of(2024, 1, 15)).category(salaryCategory).build();
        Transaction t2 = Transaction.builder().amount(new BigDecimal("50000"))
                .date(LocalDate.of(2024, 6, 15)).category(salaryCategory).build();
        Transaction t3 = Transaction.builder().amount(new BigDecimal("10000"))
                .date(LocalDate.of(2024, 3, 1)).category(foodCategory).build();

        when(transactionRepository.findByUserIdAndYear(1L, 2024))
                .thenReturn(List.of(t1, t2, t3));

        YearlyReportResponse result = reportService.getYearlyReport(2024);

        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getTotalIncome()).containsEntry("Salary", new BigDecimal("100000"));
        assertThat(result.getTotalExpenses()).containsEntry("Food", new BigDecimal("10000"));
        assertThat(result.getNetSavings()).isEqualByComparingTo("90000");
    }

    @Test
    @DisplayName("Should return empty yearly report when no transactions")
    void getYearlyReport_Empty() {
        when(transactionRepository.findByUserIdAndYear(1L, 2025)).thenReturn(List.of());

        YearlyReportResponse result = reportService.getYearlyReport(2025);

        assertThat(result.getTotalIncome()).isEmpty();
        assertThat(result.getTotalExpenses()).isEmpty();
        assertThat(result.getNetSavings()).isEqualByComparingTo("0");
    }

    private void mockSecurityContext(String username) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}
