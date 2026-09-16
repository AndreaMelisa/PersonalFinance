package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.TransactionRequest;
import com.aaryav.finance.dto.request.TransactionUpdateRequest;
import com.aaryav.finance.dto.response.TransactionResponse;
import com.aaryav.finance.entity.Category;
import com.aaryav.finance.entity.Transaction;
import com.aaryav.finance.entity.User;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.repository.CategoryRepository;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service handling transaction CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new transaction linked to a valid category.
     */
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = getCurrentUser();

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }

        Category category = categoryRepository
                .findByNameAccessibleByUser(request.getCategory(), user.getId())
                .orElseThrow(() -> new BadRequestException("Invalid category: " + request.getCategory()));

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .date(request.getDate())
                .category(category)
                .description(request.getDescription())
                .user(user)
                .build();

        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    /**
     * Retrieves transactions with optional filters.
     */
    public Map<String, List<TransactionResponse>> getTransactions(
            LocalDate startDate, LocalDate endDate, Long categoryId, String categoryName) {
        User user = getCurrentUser();

        // Resolve category name to ID if provided
        Long resolvedCategoryId = categoryId;
        if (categoryName != null && !categoryName.isBlank() && resolvedCategoryId == null) {
            resolvedCategoryId = categoryRepository
                    .findByNameAccessibleByUser(categoryName, user.getId())
                    .map(c -> c.getId())
                    .orElse(null);
        }

        List<TransactionResponse> transactions = transactionRepository
                .findByFilters(user.getId(), startDate, endDate, resolvedCategoryId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return Map.of("transactions", transactions);
    }

    /**
     * Updates a transaction (date is immutable).
     */
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request) {
        User user = getCurrentUser();

        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (request.getAmount() != null) {
            if (request.getAmount().signum() <= 0) {
                throw new BadRequestException("Amount must be positive");
            }
            transaction.setAmount(request.getAmount());
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository
                    .findByNameAccessibleByUser(request.getCategory(), user.getId())
                    .orElseThrow(() -> new BadRequestException("Invalid category: " + request.getCategory()));
            transaction.setCategory(category);
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    /**
     * Deletes a transaction by ID.
     */
    @Transactional
    public Map<String, String> deleteTransaction(Long id) {
        User user = getCurrentUser();

        Transaction transaction = transactionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transactionRepository.delete(transaction);
        return Map.of("message", "Transaction deleted successfully");
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .date(transaction.getDate())
                .category(transaction.getCategory().getName())
                .description(transaction.getDescription())
                .type(transaction.getCategory().getType().name())
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
