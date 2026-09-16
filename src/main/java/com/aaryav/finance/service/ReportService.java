package com.aaryav.finance.service;

import com.aaryav.finance.dto.response.MonthlyReportResponse;
import com.aaryav.finance.dto.response.YearlyReportResponse;
import com.aaryav.finance.entity.CategoryType;
import com.aaryav.finance.entity.Transaction;
import com.aaryav.finance.entity.User;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service generating monthly and yearly financial reports.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Generates a monthly report with income/expense breakdown by category.
     */
    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new com.aaryav.finance.exception.BadRequestException("Month must be between 1 and 12");
        }
        User user = getCurrentUser();
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndYearAndMonth(user.getId(), year, month);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            if (t.getCategory().getType() == CategoryType.INCOME) {
                totalIncome.merge(categoryName, t.getAmount(), BigDecimal::add);
            } else {
                totalExpenses.merge(categoryName, t.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal incomeSum = totalIncome.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenseSum = totalExpenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(incomeSum.subtract(expenseSum))
                .build();
    }

    /**
     * Generates a yearly report with income/expense breakdown by category.
     */
    public YearlyReportResponse getYearlyReport(int year) {
        User user = getCurrentUser();
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndYear(user.getId(), year);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            if (t.getCategory().getType() == CategoryType.INCOME) {
                totalIncome.merge(categoryName, t.getAmount(), BigDecimal::add);
            } else {
                totalExpenses.merge(categoryName, t.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal incomeSum = totalIncome.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenseSum = totalExpenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return YearlyReportResponse.builder()
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(incomeSum.subtract(expenseSum))
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
