package com.aaryav.finance.service;

import com.aaryav.finance.dto.request.GoalRequest;
import com.aaryav.finance.dto.request.GoalUpdateRequest;
import com.aaryav.finance.dto.response.GoalResponse;
import com.aaryav.finance.entity.CategoryType;
import com.aaryav.finance.entity.Goal;
import com.aaryav.finance.entity.Transaction;
import com.aaryav.finance.entity.User;
import com.aaryav.finance.exception.BadRequestException;
import com.aaryav.finance.exception.ForbiddenException;
import com.aaryav.finance.exception.ResourceNotFoundException;
import com.aaryav.finance.repository.GoalRepository;
import com.aaryav.finance.repository.TransactionRepository;
import com.aaryav.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service managing savings goals with dynamic progress tracking.
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new savings goal.
     */
    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User user = getCurrentUser();

        if (!request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be in the future");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        Goal goal = Goal.builder()
                .goalName(request.getGoalName())
                .targetAmount(request.getTargetAmount())
                .targetDate(request.getTargetDate())
                .startDate(startDate)
                .user(user)
                .build();

        goal = goalRepository.save(goal);
        return buildGoalResponse(goal, user.getId());
    }

    /**
     * Returns all goals for the current user with calculated progress.
     */
    public Map<String, List<GoalResponse>> getAllGoals() {
        User user = getCurrentUser();
        List<GoalResponse> goals = goalRepository.findByUserId(user.getId())
                .stream()
                .map(goal -> buildGoalResponse(goal, user.getId()))
                .collect(Collectors.toList());
        return Map.of("goals", goals);
    }

    /**
     * Returns a specific goal by ID with progress calculation.
     */
    public GoalResponse getGoalById(Long id) {
        User user = getCurrentUser();

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this goal");
        }

        return buildGoalResponse(goal, user.getId());
    }

    /**
     * Updates target amount and/or target date of a goal.
     */
    @Transactional
    public GoalResponse updateGoal(Long id, GoalUpdateRequest request) {
        User user = getCurrentUser();

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this goal");
        }

        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be in the future");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        goal = goalRepository.save(goal);
        return buildGoalResponse(goal, user.getId());
    }

    /**
     * Deletes a goal by ID.
     */
    @Transactional
    public Map<String, String> deleteGoal(Long id) {
        User user = getCurrentUser();

        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this goal");
        }

        goalRepository.delete(goal);
        return Map.of("message", "Goal deleted successfully");
    }

    /**
     * Calculates progress from transactions since goal start date.
     * Progress = Total Income - Total Expenses since startDate.
     */
    private GoalResponse buildGoalResponse(Goal goal, Long userId) {
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndDateOnOrAfter(userId, goal.getStartDate());

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getCategory().getType() == CategoryType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getCategory().getType() == CategoryType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentProgress = totalIncome.subtract(totalExpense);

        double progressPercentage = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? currentProgress.multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue()
                : 0.0;

        BigDecimal remainingAmount = goal.getTargetAmount().subtract(currentProgress)
                .max(BigDecimal.ZERO);

        return GoalResponse.builder()
                .id(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .targetDate(goal.getTargetDate())
                .startDate(goal.getStartDate())
                .currentProgress(currentProgress)
                .progressPercentage(progressPercentage)
                .remainingAmount(remainingAmount)
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
